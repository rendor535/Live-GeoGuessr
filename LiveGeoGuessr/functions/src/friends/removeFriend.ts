import {FieldValue} from "firebase-admin/firestore";
import {
  HttpsError,
  onCall,
} from "firebase-functions/v2/https";
import {requireAuthenticatedUid} from "../shared/authorization";
import {
  db,
  getFriendRequestId,
} from "../shared/firestore";
import {requireString} from "../shared/validation";

type RemoveFriendData = {
  friendUid?: unknown;
};

export const removeFriend =
  onCall<RemoveFriendData>(async (request) => {
    const uid = requireAuthenticatedUid(request);
    const friendUid = requireString(
      request.data?.friendUid,
      "friendUid",
    );

    if (uid === friendUid) {
      throw new HttpsError(
        "invalid-argument",
        "Invalid friendUid.",
      );
    }

    const myFriendReference = db
      .collection("users")
      .doc(uid)
      .collection("friends")
      .doc(friendUid);

    const otherFriendReference = db
      .collection("users")
      .doc(friendUid)
      .collection("friends")
      .doc(uid);

    const requestId = getFriendRequestId(
      uid,
      friendUid,
    );

    const requestReference = db
      .collection("friendRequests")
      .doc(requestId);

    await db.runTransaction(async (transaction) => {
      const myFriendSnapshot =
        await transaction.get(myFriendReference);

      if (!myFriendSnapshot.exists) {
        throw new HttpsError(
          "not-found",
          "Friend relation not found.",
        );
      }

      const now = FieldValue.serverTimestamp();

      transaction.delete(myFriendReference);
      transaction.delete(otherFriendReference);

      transaction.set(
        requestReference,
        {
          status: "removed",
          updatedAt: now,
        },
        {
          merge: true,
        },
      );

      transaction.update(
        db.collection("users").doc(uid),
        {
          "stats.friendsCount":
            FieldValue.increment(-1),
          updatedAt: now,
        },
      );

      transaction.update(
        db.collection("users").doc(friendUid),
        {
          "stats.friendsCount":
            FieldValue.increment(-1),
          updatedAt: now,
        },
      );
    });

    return {
      success: true,
    };
  });
