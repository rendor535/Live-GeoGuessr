import type {CallableRequest} from "firebase-functions/v2/https";
import {HttpsError} from "firebase-functions/v2/https";

export function requireAuthenticatedUid<T>(
  request: CallableRequest<T>,
): string {
  const uid = request.auth?.uid;

  if (!uid) {
    throw new HttpsError(
      "unauthenticated",
      "User must be logged in.",
    );
  }

  return uid;
}
