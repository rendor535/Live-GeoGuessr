import {setGlobalOptions} from "firebase-functions";
import {onRequest} from "firebase-functions/https";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import {getAuth} from "firebase-admin/auth";
import {
  DocumentReference,
  FieldValue,
  getFirestore,
} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";

admin.initializeApp();

const db = admin.firestore();

type GameModeType = "CITY";

function getFriendRequestId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

async function getUserPublicData(uid: string) {
  const userSnap = await db.collection("users").doc(uid).get();

  if (!userSnap.exists) {
    throw new HttpsError("not-found", "User not found");
  }

  const data = userSnap.data() || {};

  return {
    uid,
    nickname: data.nickname || data.displayName || "Player",
    displayName: data.displayName || data.nickname || "Player",
    photoUrl: data.photoUrl || null,
  };
}
export const sendFriendRequest = onCall(async (request) => {
  const fromUid = request.auth?.uid;

  if (!fromUid) {
    throw new HttpsError("unauthenticated", "User must be logged in");
  }

  const toUid = String(request.data?.toUid || "");

  if (!toUid) {
    throw new HttpsError("invalid-argument", "Missing toUid");
  }

  if (fromUid === toUid) {
    throw new HttpsError("invalid-argument", "Cannot add yourself");
  }

  const requestId = getFriendRequestId(fromUid, toUid);
  const requestRef = db.collection("friendRequests").doc(requestId);

  const fromUser = await getUserPublicData(fromUid);
  const toUser = await getUserPublicData(toUid);

  await db.runTransaction(async (transaction) => {
    const requestSnap = await transaction.get(requestRef);

    if (requestSnap.exists) {
      const existing = requestSnap.data();

      if (existing?.status === "pending") {
        throw new HttpsError("already-exists", "Friend request already exists");
      }

      if (existing?.status === "accepted") {
        throw new HttpsError("already-exists", "Users are already friends");
      }
    }

    const fromFriendRef = db
      .collection("users")
      .doc(fromUid)
      .collection("friends")
      .doc(toUid);

    const fromFriendSnap = await transaction.get(fromFriendRef);

    if (fromFriendSnap.exists) {
      throw new HttpsError("already-exists", "Users are already friends");
    }

    transaction.set(requestRef, {
      fromUid,
      toUid,
      fromNickname: fromUser.nickname,
      fromDisplayName: fromUser.displayName,
      fromPhotoUrl: fromUser.photoUrl,
      toNickname: toUser.nickname,
      toDisplayName: toUser.displayName,
      toPhotoUrl: toUser.photoUrl,
      status: "pending",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return {
    success: true,
    requestId,
  };
});

export const acceptFriendRequest = onCall(async (request) => {
  const uid = request.auth?.uid;

  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be logged in");
  }

  const requestId = String(request.data?.requestId || "");

  if (!requestId) {
    throw new HttpsError("invalid-argument", "Missing requestId");
  }

  const requestRef = db.collection("friendRequests").doc(requestId);

  await db.runTransaction(async (transaction) => {
    const requestSnap = await transaction.get(requestRef);

    if (!requestSnap.exists) {
      throw new HttpsError("not-found", "Friend request not found");
    }

    const requestData = requestSnap.data();

    if (!requestData) {
      throw new HttpsError("internal", "Invalid friend request");
    }

    if (requestData.toUid !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Only receiver can accept this request"
      );
    }

    if (requestData.status !== "pending") {
      throw new HttpsError("failed-precondition", "Request is not pending");
    }

    const fromUid = requestData.fromUid;
    const toUid = requestData.toUid;

    const fromFriendRef = db
      .collection("users")
      .doc(fromUid)
      .collection("friends")
      .doc(toUid);

    const toFriendRef = db
      .collection("users")
      .doc(toUid)
      .collection("friends")
      .doc(fromUid);

    const fromFriendSnap = await transaction.get(fromFriendRef);

    const now = admin.firestore.FieldValue.serverTimestamp();

    transaction.set(fromFriendRef, {
      uid: toUid,
      nickname: requestData.toNickname,
      displayName: requestData.toDisplayName || requestData.toNickname,
      photoUrl: requestData.toPhotoUrl || null,
      addedAt: now,
    });

    transaction.set(toFriendRef, {
      uid: fromUid,
      nickname: requestData.fromNickname,
      displayName: requestData.fromDisplayName || requestData.fromNickname,
      photoUrl: requestData.fromPhotoUrl || null,
      addedAt: now,
    });

    transaction.update(requestRef, {
      status: "accepted",
      updatedAt: now,
    });

    if (!fromFriendSnap.exists) {
      transaction.update(db.collection("users").doc(fromUid), {
        "stats.friendsCount": admin.firestore.FieldValue.increment(1),
        updatedAt: now,
      });

      transaction.update(db.collection("users").doc(toUid), {
        "stats.friendsCount": admin.firestore.FieldValue.increment(1),
        updatedAt: now,
      });
    }
  });

  return {
    success: true,
  };
});

export const rejectFriendRequest = onCall(async (request) => {
  const uid = request.auth?.uid;

  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be logged in");
  }

  const requestId = String(request.data?.requestId || "");

  if (!requestId) {
    throw new HttpsError("invalid-argument", "Missing requestId");
  }

  const requestRef = db.collection("friendRequests").doc(requestId);

  await db.runTransaction(async (transaction) => {
    const requestSnap = await transaction.get(requestRef);

    if (!requestSnap.exists) {
      throw new HttpsError("not-found", "Friend request not found");
    }

    const requestData = requestSnap.data();

    if (!requestData) {
      throw new HttpsError("internal", "Invalid friend request");
    }

    if (requestData.toUid !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Only receiver can reject this request"
      );
    }

    if (requestData.status !== "pending") {
      throw new HttpsError("failed-precondition", "Request is not pending");
    }

    transaction.update(requestRef, {
      status: "rejected",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return {
    success: true,
  };
});

export const removeFriend = onCall(async (request) => {
  const uid = request.auth?.uid;

  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be logged in");
  }

  const friendUid = String(request.data?.friendUid || "");

  if (!friendUid) {
    throw new HttpsError("invalid-argument", "Missing friendUid");
  }

  if (uid === friendUid) {
    throw new HttpsError("invalid-argument", "Invalid friendUid");
  }

  const myFriendRef = db
    .collection("users")
    .doc(uid)
    .collection("friends")
    .doc(friendUid);

  const otherFriendRef = db
    .collection("users")
    .doc(friendUid)
    .collection("friends")
    .doc(uid);

  const requestId = getFriendRequestId(uid, friendUid);
  const requestRef = db.collection("friendRequests").doc(requestId);

  await db.runTransaction(async (transaction) => {
    const myFriendSnap = await transaction.get(myFriendRef);

    if (!myFriendSnap.exists) {
      throw new HttpsError("not-found", "Friend relation not found");
    }

    const now = admin.firestore.FieldValue.serverTimestamp();

    transaction.delete(myFriendRef);
    transaction.delete(otherFriendRef);

    transaction.set(
      requestRef,
      {
        status: "removed",
        updatedAt: now,
      },
      {
        merge: true,
      }
    );

    transaction.update(db.collection("users").doc(uid), {
      "stats.friendsCount": admin.firestore.FieldValue.increment(-1),
      updatedAt: now,
    });

    transaction.update(db.collection("users").doc(friendUid), {
      "stats.friendsCount": admin.firestore.FieldValue.increment(-1),
      updatedAt: now,
    });
  });

  return {
    success: true,
  };
});


type GameModeConfig = {
  type: GameModeType;
  displayName: string;
  maxScoringDistanceMeters: number;
  maxPoints: number;
  initialMapDiameterMeters: number;
  initialMapOffsetMaxMeters: number;
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
        initialMapOffsetMaxMeters: 30000,
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

function randomGaussian(): number {
  let u = 0;
  let v = 0;

  while (u === 0) {
    u = Math.random();
  }

  while (v === 0) {
    v = Math.random();
  }

  return Math.sqrt(-2.0 * Math.log(u)) *
    Math.cos(2.0 * Math.PI * v);
}

function normalizeLongitude(longitude: number): number {
  if (longitude > 180) {
    return longitude - 360;
  }

  if (longitude < -180) {
    return longitude + 360;
  }

  return longitude;
}

function createApproximateMapCenter(
  realLatitude: number,
  realLongitude: number,
  maxOffsetMeters: number
): {
  latitude: number;
  longitude: number;
} {
  const earthRadiusMeters = 6371000;

  const angle = Math.random() * 2 * Math.PI;

  const gaussianDistanceMeters = Math.abs(randomGaussian()) *
    (maxOffsetMeters / 3);

  const distanceMeters = Math.min(
    gaussianDistanceMeters,
    maxOffsetMeters
  );

  const northOffsetMeters = Math.cos(angle) * distanceMeters;
  const eastOffsetMeters = Math.sin(angle) * distanceMeters;

  const latitudeOffset =
    northOffsetMeters / earthRadiusMeters * 180 / Math.PI;

  const longitudeOffset =
    eastOffsetMeters /
    (earthRadiusMeters * Math.cos(toRadians(realLatitude))) *
    180 /
    Math.PI;

  const latitude = Math.max(
    -90,
    Math.min(90, realLatitude + latitudeOffset)
  );

  const longitude = normalizeLongitude(realLongitude + longitudeOffset);

  return {
    latitude,
    longitude,
  };
}
export const getGuessMapPreview = onCall(
  {
    region: "us-central1",
  },
  async (request) => {
    const uid = request.auth?.uid;

    if (!uid) {
      throw new HttpsError("unauthenticated", "User must be logged in.");
    }

    const postId = String(request.data.postId ?? "");
    const gameMode = String(request.data.gameMode ?? "CITY");

    if (!postId) {
      throw new HttpsError("invalid-argument", "Missing postId.");
    }

    const config = getGameModeConfig(gameMode);

    const postSnapshot = await db
      .collection("posts")
      .doc(postId)
      .get();

    if (!postSnapshot.exists) {
      throw new HttpsError("not-found", "Post not found.");
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

    const approximateCenter = createApproximateMapCenter(
      realLatitude,
      realLongitude,
      config.initialMapOffsetMaxMeters
    );

    return {
      postId,
      gameMode: config.type,
      initialMapCenterLatitude: approximateCenter.latitude,
      initialMapCenterLongitude: approximateCenter.longitude,
      initialMapDiameterMeters: config.initialMapDiameterMeters,
    };
  }
);

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

      const postOwnerUid = String(post.userId ?? "");

      if (postOwnerUid === uid) {
        throw new HttpsError(
          "permission-denied",
          "Cannot guess your own post."
        );
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
        guessedLatitude,
        guessedLongitude,
      };
    });

    return result;
  }
);

const FRIEND_REQUESTS_COLLECTION = "friendRequests";


async function deleteStoragePrefix(prefix: string): Promise<void> {
  const bucket = getStorage().bucket();
  const [files] = await bucket.getFiles({prefix});

  const batchSize = 50;

  for (let index = 0; index < files.length; index += batchSize) {
    const batch = files.slice(index, index + batchSize);

    await Promise.all(
      batch.map(async (file) => {
        await file.delete();
      }),
    );
  }
}

async function deleteDocumentReferences(
  references: DocumentReference[],
): Promise<void> {
  const db = getFirestore();

  // Usuwa duplikaty po pełnej ścieżce dokumentu.
  const uniqueReferences = new Map<string, DocumentReference>();

  for (const reference of references) {
    uniqueReferences.set(reference.path, reference);
  }

  const bulkWriter = db.bulkWriter();

  for (const reference of uniqueReferences.values()) {
    bulkWriter.delete(reference);
  }

  await bulkWriter.close();
}

async function refreshFriendStats(userUids: Set<string>): Promise<void> {
  const db = getFirestore();

  for (const uid of userUids) {
    const userReference = db.collection("users").doc(uid);
    const userSnapshot = await userReference.get();

    if (!userSnapshot.exists) {
      continue;
    }

    const friendsSnapshot = await userReference
      .collection("friends")
      .get();

    await userReference.update({
      "stats.friendsCount": friendsSnapshot.size,
      "updatedAt": FieldValue.serverTimestamp(),
    });
  }
}

async function refreshGuessStats(userUids: Set<string>): Promise<void> {
  const db = getFirestore();

  for (const uid of userUids) {
    const userReference = db.collection("users").doc(uid);
    const userSnapshot = await userReference.get();

    if (!userSnapshot.exists) {
      continue;
    }

    const guessesSnapshot = await db
      .collection("guesses")
      .where("userUid", "==", uid)
      .get();

    let pointsTotal = 0;
    let bestGuessMeters: number | null = null;

    for (const guessDocument of guessesSnapshot.docs) {
      const points = guessDocument.get("points");
      const distanceMeters = guessDocument.get("distanceMeters");

      if (typeof points === "number") {
        pointsTotal += points;
      }

      if (typeof distanceMeters === "number") {
        if (
          bestGuessMeters === null ||
          distanceMeters < bestGuessMeters
        ) {
          bestGuessMeters = distanceMeters;
        }
      }
    }

    await userReference.update({
      "stats.guessesCount": guessesSnapshot.size,
      "stats.pointsTotal": pointsTotal,
      "stats.bestGuessMeters": bestGuessMeters,
      "updatedAt": FieldValue.serverTimestamp(),
    });
  }
}

export const deleteAccount = onCall(
  {
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "512MiB",
  },
  async (request) => {
    const auth = request.auth;

    if (!auth) {
      throw new HttpsError(
        "unauthenticated",
        "User must be authenticated.",
      );
    }

    const uid = auth.uid;

    if (request.data?.confirm !== true) {
      throw new HttpsError(
        "invalid-argument",
        "Account deletion was not confirmed."
      );
    }

    const db = getFirestore();
    const userReference = db.collection("users").doc(uid);

    try {
      /*
       * Najpierw pobieramy wszystkie dane. Dzięki temu nadal znamy
       * listę znajomych i postów, zanim dokumenty zostaną usunięte.
       */
      const [
        postsSnapshot,
        ownGuessesSnapshot,
        sentRequestsSnapshot,
        receivedRequestsSnapshot,
        ownFriendsSnapshot,
      ] = await Promise.all([
        db.collection("posts")
          .where("userId", "==", uid)
          .get(),

        db.collection("guesses")
          .where("userUid", "==", uid)
          .get(),

        db.collection(FRIEND_REQUESTS_COLLECTION)
          .where("fromUid", "==", uid)
          .get(),

        db.collection(FRIEND_REQUESTS_COLLECTION)
          .where("toUid", "==", uid)
          .get(),

        userReference.collection("friends").get(),
      ]);

      const postIds = postsSnapshot.docs.map(
        (document) => document.id,
      );

      /*
       * Usuwamy również zgadnięcia innych użytkowników dotyczące
       * postów, które zaraz przestaną istnieć.
       */
      const guessesForDeletedPostsSnapshots = await Promise.all(
        postIds.map((postId) =>
          db.collection("guesses")
            .where("postId", "==", postId)
            .get(),
        ),
      );

      const friendUids = new Set<string>();

      for (const friendDocument of ownFriendsSnapshot.docs) {
        const friendUid =
          friendDocument.get("uid") ?? friendDocument.id;

        if (
          typeof friendUid === "string" &&
          friendUid.length > 0
        ) {
          friendUids.add(friendUid);
        }
      }

      const usersWithChangedGuessStats = new Set<string>();

      for (const snapshot of guessesForDeletedPostsSnapshots) {
        for (const guessDocument of snapshot.docs) {
          const guessUserUid = guessDocument.get("userUid");

          if (
            typeof guessUserUid === "string" &&
            guessUserUid !== uid
          ) {
            usersWithChangedGuessStats.add(guessUserUid);
          }
        }
      }

      /*
       * Storage usuwamy przed Firebase Authentication.
       * Gdyby wystąpił błąd, konto Auth nadal istnieje i operację
       * można powtórzyć.
       */
      await Promise.all([
        deleteStoragePrefix(`avatars/${uid}/`),
        deleteStoragePrefix(`posts/${uid}/`),
      ]);

      const referencesToDelete: DocumentReference[] = [];

      // Posty użytkownika.
      for (const document of postsSnapshot.docs) {
        referencesToDelete.push(document.ref);
      }

      // Wszystkie zgadnięcia wykonane przez użytkownika.
      for (const document of ownGuessesSnapshot.docs) {
        referencesToDelete.push(document.ref);
      }

      // Zgadnięcia odnoszące się do jego usuwanych postów.
      for (const snapshot of guessesForDeletedPostsSnapshots) {
        for (const document of snapshot.docs) {
          referencesToDelete.push(document.ref);
        }
      }

      // Wysłane requesty.
      for (const document of sentRequestsSnapshot.docs) {
        referencesToDelete.push(document.ref);
      }

      // Odebrane i zaakceptowane requesty.
      for (const document of receivedRequestsSnapshot.docs) {
        referencesToDelete.push(document.ref);
      }

      // Własna podkolekcja friends.
      for (const document of ownFriendsSnapshot.docs) {
        referencesToDelete.push(document.ref);
      }

      // Wpis użytkownika w podkolekcjach jego znajomych.
      for (const friendUid of friendUids) {
        referencesToDelete.push(
          db.collection("users")
            .doc(friendUid)
            .collection("friends")
            .doc(uid),
        );
      }

      // Dokument użytkownika usuwamy po dokumentach podkolekcji.
      referencesToDelete.push(userReference);

      await deleteDocumentReferences(referencesToDelete);

      /*
       * Przeliczamy statystyki na podstawie aktualnego stanu bazy,
       * zamiast wykonywać increment(-1). Dzięki temu ponowne
       * wywołanie funkcji nie odejmie wartości drugi raz.
       */
      await refreshFriendStats(friendUids);

      await refreshGuessStats(usersWithChangedGuessStats);

      /*
       * Firebase Authentication zawsze na końcu.
       * Po tym użytkownik nie może już ponownie wywołać funkcji.
       */
      await getAuth().deleteUser(uid);

      logger.info("Account deleted", {
        uid,
        deletedPosts: postsSnapshot.size,
        deletedOwnGuesses: ownGuessesSnapshot.size,
        deletedFriends: ownFriendsSnapshot.size,
      });

      return {
        success: true,
      };
    } catch (error) {
      logger.error("Account deletion failed", {
        uid,
        error,
      });

      if (error instanceof HttpsError) {
        throw error;
      }

      throw new HttpsError(
        "internal",
        "ACCOUNT_DELETION_FAILED",
      );
    }
  },
);

