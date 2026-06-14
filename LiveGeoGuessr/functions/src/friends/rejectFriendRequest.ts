import {FieldValue} from "firebase-admin/firestore";
import {
  HttpsError,
  onCall,
} from "firebase-functions/v2/https";
import {requireAuthenticatedUid} from "../shared/authorization";
import {db} from "../shared/firestore";
import {requireString} from "../shared/validation";

type RejectFriendRequestData = {
  requestId?: unknown;
};

export const rejectFriendRequest =
  onCall<RejectFriendRequestData>(
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
            "Only receiver can reject this request.",
          );
        }

        if (requestData.status !== "pending") {
          throw new HttpsError(
            "failed-precondition",
            "Request is not pending.",
          );
        }

        transaction.update(requestReference, {
          status: "rejected",
          updatedAt: FieldValue.serverTimestamp(),
        });
      });

      return {
        success: true,
      };
    },
  );
