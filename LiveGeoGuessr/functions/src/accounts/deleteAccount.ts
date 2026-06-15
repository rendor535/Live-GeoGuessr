import {getAuth} from "firebase-admin/auth";
import type {
  DocumentReference,
} from "firebase-admin/firestore";
import {
  FieldValue,
} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import * as logger from "firebase-functions/logger";
import {
  HttpsError,
  onCall,
} from "firebase-functions/v2/https";
import {requireAuthenticatedUid} from "../shared/authorization";
import {firebaseApp} from "../shared/firebase";
import {
  db,
  deleteDocumentReferences,
} from "../shared/firestore";

const FRIEND_REQUESTS_COLLECTION =
  "friendRequests";

type DeleteAccountData = {
  confirm?: unknown;
};

async function deleteStoragePrefix(
  prefix: string,
): Promise<void> {
  const bucket =
    getStorage(firebaseApp).bucket();

  const [files] = await bucket.getFiles({
    prefix,
  });

  const batchSize = 50;

  for (
    let index = 0;
    index < files.length;
    index += batchSize
  ) {
    const batch = files.slice(
      index,
      index + batchSize,
    );

    await Promise.all(
      batch.map((file) => file.delete()),
    );
  }
}

async function refreshFriendStats(
  userUids: Set<string>,
): Promise<void> {
  for (const uid of userUids) {
    const userReference = db
      .collection("users")
      .doc(uid);

    const userSnapshot =
      await userReference.get();

    if (!userSnapshot.exists) {
      continue;
    }

    const friendsSnapshot =
      await userReference
        .collection("friends")
        .get();

    await userReference.update({
      "stats.friendsCount":
        friendsSnapshot.size,
      updatedAt:
        FieldValue.serverTimestamp(),
    });
  }
}

async function refreshGuessStats(
  userUids: Set<string>,
): Promise<void> {
  for (const uid of userUids) {
    const userReference = db
      .collection("users")
      .doc(uid);

    const userSnapshot =
      await userReference.get();

    if (!userSnapshot.exists) {
      continue;
    }

    const guessesSnapshot = await db
      .collection("guesses")
      .where("userUid", "==", uid)
      .get();

    let pointsTotal = 0;
    let bestGuessMeters: number | null =
      null;

    for (
      const guessDocument
      of guessesSnapshot.docs
    ) {
      const points =
        guessDocument.get("points");

      const distanceMeters =
        guessDocument.get(
          "distanceMeters",
        );

      if (typeof points === "number") {
        pointsTotal += points;
      }

      if (
        typeof distanceMeters ===
          "number" &&
        (
          bestGuessMeters === null ||
          distanceMeters <
            bestGuessMeters
        )
      ) {
        bestGuessMeters =
          distanceMeters;
      }
    }

    await userReference.update({
      "stats.guessesCount":
        guessesSnapshot.size,
      "stats.pointsTotal": pointsTotal,
      "stats.bestGuessMeters":
        bestGuessMeters,
      updatedAt:
        FieldValue.serverTimestamp(),
    });
  }
}

export const deleteAccount =
  onCall<DeleteAccountData>(
    {
      timeoutSeconds: 540,
      memory: "512MiB",
    },
    async (request) => {
      const uid =
        requireAuthenticatedUid(request);

      if (request.data?.confirm !== true) {
        throw new HttpsError(
          "invalid-argument",
          "Account deletion was not confirmed.",
        );
      }

      const userReference = db
        .collection("users")
        .doc(uid);

      try {
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

          db.collection(
            FRIEND_REQUESTS_COLLECTION,
          )
            .where("fromUid", "==", uid)
            .get(),

          db.collection(
            FRIEND_REQUESTS_COLLECTION,
          )
            .where("toUid", "==", uid)
            .get(),

          userReference
            .collection("friends")
            .get(),
        ]);

        const postIds =
          postsSnapshot.docs.map(
            (document) => document.id,
          );

        const guessesForDeletedPosts =
          await Promise.all(
            postIds.map((postId) =>
              db.collection("guesses")
                .where(
                  "postId",
                  "==",
                  postId,
                )
                .get(),
            ),
          );

        const friendUids =
          new Set<string>();

        for (
          const friendDocument
          of ownFriendsSnapshot.docs
        ) {
          const friendUid =
            friendDocument.get("uid") ??
            friendDocument.id;

          if (
            typeof friendUid ===
              "string" &&
            friendUid.length > 0
          ) {
            friendUids.add(friendUid);
          }
        }

        const usersWithChangedGuessStats =
          new Set<string>();

        for (
          const snapshot
          of guessesForDeletedPosts
        ) {
          for (
            const guessDocument
            of snapshot.docs
          ) {
            const guessUserUid =
              guessDocument.get(
                "userUid",
              );

            if (
              typeof guessUserUid ===
                "string" &&
              guessUserUid !== uid
            ) {
              usersWithChangedGuessStats
                .add(guessUserUid);
            }
          }
        }

        await Promise.all([
          deleteStoragePrefix(
            `avatars/${uid}/`,
          ),
          deleteStoragePrefix(
            `posts/${uid}/`,
          ),
        ]);

        const referencesToDelete:
          DocumentReference[] = [];

        for (
          const document
          of postsSnapshot.docs
        ) {
          referencesToDelete.push(
            document.ref,
          );
        }

        for (
          const document
          of ownGuessesSnapshot.docs
        ) {
          referencesToDelete.push(
            document.ref,
          );
        }

        for (
          const snapshot
          of guessesForDeletedPosts
        ) {
          for (
            const document
            of snapshot.docs
          ) {
            referencesToDelete.push(
              document.ref,
            );
          }
        }

        for (
          const document
          of sentRequestsSnapshot.docs
        ) {
          referencesToDelete.push(
            document.ref,
          );
        }

        for (
          const document
          of receivedRequestsSnapshot.docs
        ) {
          referencesToDelete.push(
            document.ref,
          );
        }

        for (
          const document
          of ownFriendsSnapshot.docs
        ) {
          referencesToDelete.push(
            document.ref,
          );
        }

        for (const friendUid of friendUids) {
          referencesToDelete.push(
            db.collection("users")
              .doc(friendUid)
              .collection("friends")
              .doc(uid),
          );
        }

        referencesToDelete.push(
          userReference,
        );

        await deleteDocumentReferences(
          referencesToDelete,
        );

        await refreshFriendStats(
          friendUids,
        );

        await refreshGuessStats(
          usersWithChangedGuessStats,
        );

        await getAuth(
          firebaseApp,
        ).deleteUser(uid);

        logger.info("Account deleted", {
          uid,
          deletedPosts:
            postsSnapshot.size,
          deletedOwnGuesses:
            ownGuessesSnapshot.size,
          deletedFriends:
            ownFriendsSnapshot.size,
        });

        return {
          success: true,
        };
      } catch (error) {
        logger.error(
          "Account deletion failed",
          {
            uid,
            error,
          },
        );

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
