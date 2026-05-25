import {setGlobalOptions} from "firebase-functions";
import {onRequest} from "firebase-functions/https";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

type GameModeType = "CITY";

type GameModeConfig = {
  type: GameModeType;
  displayName: string;
  maxScoringDistanceMeters: number;
  maxPoints: number;
  initialMapDiameterMeters: number;
  scoringVersion: number;
};

setGlobalOptions({maxInstances: 10});

export const health = onRequest((request, response) => {
  logger.info("Health check called");

  response.json({
    status: "ok",
    app: "Live GeoGuessr",
    backend: "Firebase Cloud Functions",
  });
});


function getGameModeConfig(type: string): GameModeConfig {
  switch (type) {
    case "CITY":
      return {
        type: "CITY",
        displayName: "Miasto",
        maxScoringDistanceMeters: 2000,
        maxPoints: 10000,
        initialMapDiameterMeters: 36000,
        scoringVersion: 1,
      };

    default:
      throw new HttpsError("invalid-argument", "Invalid game mode.");
  }
}

function toRadians(degrees: number): number {
  return degrees * Math.PI / 180;
}

function calculateDistanceMeters(
  realLat: number,
  realLng: number,
  guessLat: number,
  guessLng: number
): number {
  const earthRadiusMeters = 6371000;

  const dLat = toRadians(guessLat - realLat);
  const dLng = toRadians(guessLng - realLng);

  const lat1 = toRadians(realLat);
  const lat2 = toRadians(guessLat);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1) *
      Math.cos(lat2) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return earthRadiusMeters * c;
}

function calculatePoints(
  distanceMeters: number,
  config: GameModeConfig
): number {
  if (distanceMeters <= 0) {
    return config.maxPoints;
  }

  if (distanceMeters >= config.maxScoringDistanceMeters) {
    return 0;
  }

  const x = distanceMeters / config.maxScoringDistanceMeters;

  const scoreRatio = 2 / (10 * x - 10) + 1.21;

  const safeScoreRatio = Math.max(0, Math.min(1, scoreRatio));

  return Math.round(safeScoreRatio * config.maxPoints);
}

function assertValidCoordinates(latitude: number, longitude: number): void {
  if (Number.isNaN(latitude) || latitude < -90 || latitude > 90) {
    throw new HttpsError("invalid-argument", "Invalid latitude.");
  }

  if (Number.isNaN(longitude) || longitude < -180 || longitude > 180) {
    throw new HttpsError("invalid-argument", "Invalid longitude.");
  }
}

export const submitGuess = onCall(
  {
    region: "us-central1",
  },
  async (request) => {
    const uid = request.auth?.uid;

    if (!uid) {
      throw new HttpsError("unauthenticated", "User must be logged in.");
    }

    const postId = String(request.data.postId ?? "");
    const guessedLatitude = Number(request.data.guessedLatitude);
    const guessedLongitude = Number(request.data.guessedLongitude);
    const gameMode = String(request.data.gameMode ?? "CITY");

    if (!postId) {
      throw new HttpsError("invalid-argument", "Missing postId.");
    }

    assertValidCoordinates(guessedLatitude, guessedLongitude);

    const config = getGameModeConfig(gameMode);

    const postRef = db.collection("posts").doc(postId);
    const userRef = db.collection("users").doc(uid);
    const guessRef = db.collection("guesses").doc(`${uid}_${postId}`);

    const result = await db.runTransaction(async (transaction) => {
      const postSnapshot = await transaction.get(postRef);
      const userSnapshot = await transaction.get(userRef);
      const existingGuessSnapshot = await transaction.get(guessRef);

      if (!postSnapshot.exists) {
        throw new HttpsError("not-found", "Post not found.");
      }

      if (!userSnapshot.exists) {
        throw new HttpsError("failed-precondition", "User profile not found.");
      }

      if (existingGuessSnapshot.exists) {
        throw new HttpsError(
          "already-exists",
          "User has already guessed this post."
        );
      }

      const post = postSnapshot.data();

      if (!post) {
        throw new HttpsError("not-found", "Post not found.");
      }

      const realLatitude = Number(post.latitude);
      const realLongitude = Number(post.longitude);

      assertValidCoordinates(realLatitude, realLongitude);

      const postGameMode = String(post.gameMode ?? "CITY");

      if (postGameMode !== config.type) {
        throw new HttpsError(
          "failed-precondition",
          "Game mode does not match post game mode."
        );
      }

      const distanceMeters = calculateDistanceMeters(
        realLatitude,
        realLongitude,
        guessedLatitude,
        guessedLongitude
      );

      const points = calculatePoints(distanceMeters, config);

      const currentBestGuessMeters = userSnapshot.get("stats.bestGuessMeters");

      transaction.set(guessRef, {
        id: guessRef.id,
        userUid: uid,
        postId,
        gameMode: config.type,

        guessedLatitude,
        guessedLongitude,

        realLatitude,
        realLongitude,

        distanceMeters,
        points,

        maxPoints: config.maxPoints,
        maxScoringDistanceMeters: config.maxScoringDistanceMeters,
        scoringVersion: config.scoringVersion,

        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      const userUpdates: Record<string, unknown> = {
        "stats.pointsTotal": admin.firestore.FieldValue.increment(points),
        "stats.guessesCount": admin.firestore.FieldValue.increment(1),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      if (
        currentBestGuessMeters === null ||
        currentBestGuessMeters === undefined ||
        distanceMeters < Number(currentBestGuessMeters)
      ) {
        userUpdates["stats.bestGuessMeters"] = distanceMeters;
      }

      transaction.update(userRef, userUpdates);

      return {
        guessId: guessRef.id,
        postId,
        gameMode: config.type,
        distanceMeters,
        points,
        maxPoints: config.maxPoints,
        maxScoringDistanceMeters: config.maxScoringDistanceMeters,
        scoringVersion: config.scoringVersion,
        realLatitude,
        realLongitude,
      };
    });

    return result;
  }
);


