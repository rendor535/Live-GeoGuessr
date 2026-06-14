import {FieldValue} from "firebase-admin/firestore";
import {
  HttpsError,
  onCall,
} from "firebase-functions/v2/https";
import {requireAuthenticatedUid} from "../shared/authorization";
import {db} from "../shared/firestore";
import {requireString} from "../shared/validation";

type AcceptFriendRequestData = {
  requestId?: unknown;
};

export const acceptFriendRequest =
  onCall<AcceptFriendRequestData>(
    async (request) => {
      const uid = requireAuthenticatedUid(request);
      const requestId = requireString(
        request.data?.requestId,
        "requestId",
      );

      const requestReference = db
        .collection("friendRequests")
        .doc(requestId);

      await db.runTransaction(async (transaction) => {
        const requestSnapshot =
          await transaction.get(requestReference);

        if (!requestSnapshot.exists) {
          throw new HttpsError(
            "not-found",
            "Friend request not found.",
          );
        }

        const requestData = requestSnapshot.data();

        if (!requestData) {
          throw new HttpsError(
            "internal",
            "Invalid friend request.",
          );
        }

        if (requestData.toUid !== uid) {
          throw new HttpsError(
            "permission-denied",
            "Only receiver can accept this request.",
          );
        }

        if (requestData.status !== "pending") {
          throw new HttpsError(
            "failed-precondition",
            "Request is not pending.",
          );
        }

        const fromUid = String(requestData.fromUid);
        const toUid = String(requestData.toUid);

        const fromFriendReference = db
          .collection("users")
          .doc(fromUid)
          .collection("friends")
          .doc(toUid);

        const toFriendReference = db
          .collection("users")
          .doc(toUid)
          .collection("friends")
          .doc(fromUid);

        const fromFriendSnapshot =
          await transaction.get(
            fromFriendReference,
          );

        const now = FieldValue.serverTimestamp();

        transaction.set(fromFriendReference, {
          uid: toUid,
          nickname: requestData.toNickname,
          displayName:
            requestData.toDisplayName ??
            requestData.toNickname,
          photoUrl: requestData.toPhotoUrl ?? null,
          addedAt: now,
        });

        transaction.set(toFriendReference, {
          uid: fromUid,
          nickname: requestData.fromNickname,
          displayName:
            requestData.fromDisplayName ??
            requestData.fromNickname,
          photoUrl: requestData.fromPhotoUrl ?? null,
          addedAt: now,
        });

        transaction.update(requestReference, {
          status: "accepted",
          updatedAt: now,
        });

        if (!fromFriendSnapshot.exists) {
          transaction.update(
            db.collection("users").doc(fromUid),
            {
              "stats.friendsCount":
                FieldValue.increment(1),
              updatedAt: now,
            },
          );

          transaction.update(
            db.collection("users").doc(toUid),
            {
              "stats.friendsCount":
                FieldValue.increment(1),
              updatedAt: now,
            },
          );
        }
      });

      return {
        success: true,
      };
    },
  );
