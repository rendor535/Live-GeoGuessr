import {HttpsError} from "firebase-functions/v2/https";

export function requireString(
  value: unknown,
  fieldName: string,
): string {
  const result = String(value ?? "").trim();

  if (!result) {
    throw new HttpsError(
      "invalid-argument",
      `Missing ${fieldName}.`,
    );
  }

  return result;
}

export function assertValidCoordinates(
  latitude: number,
  longitude: number,
): void {
  if (
    Number.isNaN(latitude) ||
    latitude < -90 ||
    latitude > 90
  ) {
    throw new HttpsError(
      "invalid-argument",
      "Invalid latitude.",
    );
  }

  if (
    Number.isNaN(longitude) ||
    longitude < -180 ||
    longitude > 180
  ) {
    throw new HttpsError(
      "invalid-argument",
      "Invalid longitude.",
    );
  }
}
