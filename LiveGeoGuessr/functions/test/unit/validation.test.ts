import {
  HttpsError,
} from "firebase-functions/v2/https";
import {
  assertValidCoordinates,
  requireString,
} from "../../src/shared/validation";

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

describe("requireString", () => {
  it("returns trimmed string", () => {
    const result = requireString(
      "  post-123  ",
      "postId",
    );

    expect(result).toBe("post-123");
  });

  it.each([
    null,
    undefined,
    "",
    " ",
    "   ",
  ])(
    "throws invalid-argument for missing value: %p",
    (value) => {
      const error = captureHttpsError(() =>
        requireString(value, "postId"),
      );

      expect(error.code).toBe(
        "invalid-argument",
      );

      expect(error.message).toBe(
        "Missing postId.",
      );
    },
  );
});

describe("assertValidCoordinates", () => {
  it.each([
    [0, 0],
    [-90, -180],
    [90, 180],
    [50.0614, 19.9383],
  ])(
    "accepts latitude %p and longitude %p",
    (latitude, longitude) => {
      expect(() =>
        assertValidCoordinates(
          latitude,
          longitude,
        ),
      ).not.toThrow();
    },
  );

  it.each([
    Number.NaN,
    Number.POSITIVE_INFINITY,
    Number.NEGATIVE_INFINITY,
    -90.01,
    90.01,
  ])(
    "rejects invalid latitude: %p",
    (latitude) => {
      const error = captureHttpsError(() =>
        assertValidCoordinates(
          latitude,
          20,
        ),
      );

      expect(error.code).toBe(
        "invalid-argument",
      );

      expect(error.message).toBe(
        "Invalid latitude.",
      );
    },
  );

  it.each([
    Number.NaN,
    Number.POSITIVE_INFINITY,
    Number.NEGATIVE_INFINITY,
    -180.01,
    180.01,
  ])(
    "rejects invalid longitude: %p",
    (longitude) => {
      const error = captureHttpsError(() =>
        assertValidCoordinates(
          50,
          longitude,
        ),
      );

      expect(error.code).toBe(
        "invalid-argument",
      );

      expect(error.message).toBe(
        "Invalid longitude.",
      );
    },
  );
});