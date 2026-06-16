import type {
  CallableRequest,
} from "firebase-functions/v2/https";
import {
  HttpsError,
} from "firebase-functions/v2/https";
import {
  requireAuthenticatedUid,
} from "../../src/shared/authorization";

function createRequest(
  uid?: string,
): CallableRequest<unknown> {
  return {
    data: {},
    auth: uid ?
      {
        uid,
        token: {},
      } :
      undefined,
  } as unknown as CallableRequest<unknown>;
}

function captureHttpsError(
  action: () => unknown,
): HttpsError {
  try {
    action();
  } catch (error) {
    if (error instanceof HttpsError) {
      return error;
    }

    throw error;
  }

  throw new Error(
    "Expected HttpsError to be thrown.",
  );
}

describe("requireAuthenticatedUid", () => {
  it("returns UID of authenticated user", () => {
    const request = createRequest("user-123");

    const result =
      requireAuthenticatedUid(request);

    expect(result).toBe("user-123");
  });

  it("throws unauthenticated when auth is missing", () => {
    const request = createRequest();

    const error = captureHttpsError(() =>
      requireAuthenticatedUid(request),
    );

    expect(error.code).toBe("unauthenticated");
    expect(error.message).toBe(
      "User must be logged in.",
    );
  });

  it("throws unauthenticated when UID is empty", () => {
    const request = createRequest("");

    const error = captureHttpsError(() =>
      requireAuthenticatedUid(request),
    );

    expect(error.code).toBe("unauthenticated");
  });
});