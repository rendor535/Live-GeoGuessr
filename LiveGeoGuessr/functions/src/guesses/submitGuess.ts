import {FieldValue} from "firebase-admin/firestore";
import {
  HttpsError,
  onCall,
} from "firebase-functions/v2/https";
import {requireAuthenticatedUid} from "../shared/authorization";
import {db} from "../shared/firestore";
import {
  assertValidCoordinates,
  requireString,
} from "../shared/validation";
import {
  calculateDistanceMeters,
  calculatePoints,
  getGameModeConfig,
} from "./scoring";

type SubmitGuessData = {
  postId?: unknown;
  guessedLatitude?: unknown;
  guessedLongitude?: unknown;
  gameMode?: unknown;
};

export const submitGuess =
  onCall<SubmitGuessData>(async (request) => {
    const uid = requireAuthenticatedUid(request);

    const postId = requireString(
      request.data?.postId,
      "postId",
    );

    const guessedLatitude = Number(
      request.data?.guessedLatitude,
    );

    const guessedLongitude = Number(
      request.data?.guessedLongitude,
    );

    const gameMode = String(
      request.data?.gameMode ?? "CITY",
    );

    assertValidCoordinates(
      guessedLatitude,
      guessedLongitude,
    );

    const config = getGameModeConfig(gameMode);

    const postReference = db
      .collection("posts")
      .doc(postId);

    const userReference = db
      .collection("users")
      .doc(uid);

    const guessReference = db
      .collection("guesses")
      .doc(`${uid}_${postId}`);

    return db.runTransaction(
      async (transaction) => {
        const [
          postSnapshot,
          userSnapshot,
          existingGuessSnapshot,
        ] = await Promise.all([
          transaction.get(postReference),
          transaction.get(userReference),
          transaction.get(guessReference),
        ]);

        if (!postSnapshot.exists) {
          throw new HttpsError(
            "not-found",
            "Post not found.",
          );
        }

        if (!userSnapshot.exists) {
          throw new HttpsError(
            "failed-precondition",
            "User profile not found.",
          );
        }

        if (existingGuessSnapshot.exists) {
          throw new HttpsError(
            "already-exists",
            "User has already guessed this post.",
          );
        }

        const post = postSnapshot.data();

        if (!post) {
          throw new HttpsError(
            "not-found",
            "Post not found.",
          );
        }

        const postOwnerUid = String(
          post.userId ?? "",
        );

        if (postOwnerUid === uid) {
          throw new HttpsError(
            "permission-denied",
            "Cannot guess your own post.",
          );
        }

        const realLatitude =
          Number(post.latitude);

        const realLongitude =
          Number(post.longitude);

        assertValidCoordinates(
          realLatitude,
          realLongitude,
        );

        const postGameMode = String(
          post.gameMode ?? "CITY",
        );

        if (postGameMode !== config.type) {
          throw new HttpsError(
            "failed-precondition",
            "Game mode does not match post game mode.",
          );
        }

        const distanceMeters =
          calculateDistanceMeters(
            realLatitude,
            realLongitude,
            guessedLatitude,
            guessedLongitude,
          );

        const points = calculatePoints(
          distanceMeters,
          config,
        );

        const currentBestGuessMeters =
          userSnapshot.get(
            "stats.bestGuessMeters",
          );

        const now =
          FieldValue.serverTimestamp();

        transaction.set(guessReference, {
          id: guessReference.id,
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
          maxScoringDistanceMeters:
            config.maxScoringDistanceMeters,
          scoringVersion:
            config.scoringVersion,
          createdAt: now,
        });

        const userUpdates:
          Record<string, unknown> = {
            "stats.pointsTotal":
              FieldValue.increment(points),
            "stats.guessesCount":
              FieldValue.increment(1),
            updatedAt: now,
          };

        if (
          currentBestGuessMeters === null ||
          currentBestGuessMeters ===
            undefined ||
          distanceMeters <
            Number(currentBestGuessMeters)
        ) {
          userUpdates[
            "stats.bestGuessMeters"
          ] = distanceMeters;
        }

        transaction.update(
          userReference,
          userUpdates,
        );

        return {
          guessId: guessReference.id,
          postId,
          gameMode: config.type,
          distanceMeters,
          points,
          maxPoints: config.maxPoints,
          maxScoringDistanceMeters:
            config.maxScoringDistanceMeters,
          scoringVersion:
            config.scoringVersion,
          realLatitude,
          realLongitude,
          guessedLatitude,
          guessedLongitude,
        };
      },
    );
  });
