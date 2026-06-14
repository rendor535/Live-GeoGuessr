import type {DocumentReference} from "firebase-admin/firestore";
import {
  getFirestore,
} from "firebase-admin/firestore";
import {HttpsError} from "firebase-functions/v2/https";
import {firebaseApp} from "./firebase";

export const db = getFirestore(firebaseApp);

export type UserPublicData = {
  uid: string;
  nickname: string;
  displayName: string;
  photoUrl: string | null;
};

export function getFriendRequestId(
  uidA: string,
  uidB: string,
): string {
  return [uidA, uidB].sort().join("_");
}

export async function getUserPublicData(
  uid: string,
): Promise<UserPublicData> {
  const userSnapshot = await db
    .collection("users")
    .doc(uid)
    .get();

  if (!userSnapshot.exists) {
    throw new HttpsError(
      "not-found",
      "User not found.",
    );
  }

  const data = userSnapshot.data() ?? {};

  return {
    uid,
    nickname:
      data.nickname ??
      data.displayName ??
      "Player",
    displayName:
      data.displayName ??
      data.nickname ??
      "Player",
    photoUrl: data.photoUrl ?? null,
  };
}

export async function deleteDocumentReferences(
  references: DocumentReference[],
): Promise<void> {
  const uniqueReferences =
    new Map<string, DocumentReference>();

  for (const reference of references) {
    uniqueReferences.set(reference.path, reference);
  }

  const bulkWriter = db.bulkWriter();

  for (const reference of uniqueReferences.values()) {
    bulkWriter.delete(reference);
  }

  await bulkWriter.close();
}
