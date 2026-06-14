import {FieldValue} from "firebase-admin/firestore";
import {
  HttpsError,
  onCall,
} from "firebase-functions/v2/https";
import {requireAuthenticatedUid} from "../shared/authorization";
import {
  db,
  getFriendRequestId,
  getUserPublicData,
} from "../shared/firestore";
import {requireString} from "../shared/validation";

type SendFriendRequestData = {
  toUid?: unknown;
};

export const sendFriendRequest =
  onCall<SendFriendRequestData>(async (request) => {
    const fromUid = requireAuthenticatedUid(request);
    const toUid = requireString(
      request.data?.toUid,
      "toUid",
    );

    if (fromUid === toUid) {
      throw new HttpsError(
        "invalid-argument",
        "Cannot add yourself.",
      );
    }

    const requestId = getFriendRequestId(
      fromUid,
      toUid,
    );

    const requestReference = db
      .collection("friendRequests")
      .doc(requestId);

    const [fromUser, toUser] = await Promise.all([
      getUserPublicData(fromUid),
      getUserPublicData(toUid),
    ]);

    await db.runTransaction(async (transaction) => {
      const requestSnapshot =
        await transaction.get(requestReference);

      if (requestSnapshot.exists) {
        const existing = requestSnapshot.data();

        if (existing?.status === "pending") {
          throw new HttpsError(
            "already-exists",
            "Friend request already exists.",
          );
        }

        if (existing?.status === "accepted") {
          throw new HttpsError(
            "already-exists",
            "Users are already friends.",
          );
        }
      }

      const friendReference = db
        .collection("users")
        .doc(fromUid)
        .collection("friends")
        .doc(toUid);

      const friendSnapshot =
        await transaction.get(friendReference);

      if (friendSnapshot.exists) {
        throw new HttpsError(
          "already-exists",
          "Users are already friends.",
        );
      }

      const now = FieldValue.serverTimestamp();

      transaction.set(requestReference, {
        fromUid,
        toUid,
        fromNickname: fromUser.nickname,
        fromDisplayName: fromUser.displayName,
        fromPhotoUrl: fromUser.photoUrl,
        toNickname: toUser.nickname,
        toDisplayName: toUser.displayName,
        toPhotoUrl: toUser.photoUrl,
        status: "pending",
        createdAt: now,
        updatedAt: now,
      });
    });

    return {
      success: true,
      requestId,
    };
  });
