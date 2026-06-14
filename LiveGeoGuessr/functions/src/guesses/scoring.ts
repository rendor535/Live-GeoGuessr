import {HttpsError} from "firebase-functions/v2/https";

export type GameModeType = "CITY";

export type GameModeConfig = {
  type: GameModeType;
  displayName: string;
  maxScoringDistanceMeters: number;
  maxPoints: number;
  initialMapDiameterMeters: number;
  initialMapOffsetMaxMeters: number;
  scoringVersion: number;
};

const CITY_CONFIG: GameModeConfig = {
  type: "CITY",
  displayName: "Miasto",
  maxScoringDistanceMeters: 2000,
  maxPoints: 10000,
  initialMapDiameterMeters: 36000,
  initialMapOffsetMaxMeters: 30000,
  scoringVersion: 1,
};

export function getGameModeConfig(
  type: string,
): GameModeConfig {
  switch (type) {
    case "CITY":
      return CITY_CONFIG;

    default:
      throw new HttpsError(
        "invalid-argument",
        "Invalid game mode.",
      );
  }
}

function toRadians(degrees: number): number {
  return degrees * Math.PI / 180;
}

export function calculateDistanceMeters(
  realLatitude: number,
  realLongitude: number,
  guessedLatitude: number,
  guessedLongitude: number,
): number {
  const earthRadiusMeters = 6371000;

  const latitudeDifference = toRadians(
    guessedLatitude - realLatitude,
  );

  const longitudeDifference = toRadians(
    guessedLongitude - realLongitude,
  );

  const realLatitudeRadians =
    toRadians(realLatitude);

  const guessedLatitudeRadians =
    toRadians(guessedLatitude);

  const a =
    Math.sin(latitudeDifference / 2) ** 2 +
    Math.cos(realLatitudeRadians) *
      Math.cos(guessedLatitudeRadians) *
      Math.sin(longitudeDifference / 2) ** 2;

  const c = 2 * Math.atan2(
    Math.sqrt(a),
    Math.sqrt(1 - a),
  );

  return earthRadiusMeters * c;
}

export function calculatePoints(
  distanceMeters: number,
  config: GameModeConfig,
): number {
  if (distanceMeters <= 0) {
    return config.maxPoints;
  }

  if (
    distanceMeters >=
    config.maxScoringDistanceMeters
  ) {
    return 0;
  }

  const distanceRatio =
    distanceMeters /
    config.maxScoringDistanceMeters;

  const scoreRatio =
    2 / (10 * distanceRatio - 10) + 1.21;

  const safeScoreRatio = Math.max(
    0,
    Math.min(1, scoreRatio),
  );

  return Math.round(
    safeScoreRatio * config.maxPoints,
  );
}

function randomGaussian(): number {
  let firstValue = 0;
  let secondValue = 0;

  while (firstValue === 0) {
    firstValue = Math.random();
  }

  while (secondValue === 0) {
    secondValue = Math.random();
  }

  return Math.sqrt(
    -2 * Math.log(firstValue),
  ) * Math.cos(
    2 * Math.PI * secondValue,
  );
}

function normalizeLongitude(
  longitude: number,
): number {
  if (longitude > 180) {
    return longitude - 360;
  }

  if (longitude < -180) {
    return longitude + 360;
  }

  return longitude;
}

export function createApproximateMapCenter(
  realLatitude: number,
  realLongitude: number,
  maxOffsetMeters: number,
): {
  latitude: number;
  longitude: number;
} {
  const earthRadiusMeters = 6371000;
  const angle = Math.random() * 2 * Math.PI;

  const gaussianDistanceMeters =
    Math.abs(randomGaussian()) *
    (maxOffsetMeters / 3);

  const distanceMeters = Math.min(
    gaussianDistanceMeters,
    maxOffsetMeters,
  );

  const northOffsetMeters =
    Math.cos(angle) * distanceMeters;

  const eastOffsetMeters =
    Math.sin(angle) * distanceMeters;

  const latitudeOffset =
    northOffsetMeters /
    earthRadiusMeters *
    180 /
    Math.PI;

  const longitudeOffset =
    eastOffsetMeters /
    (
      earthRadiusMeters *
      Math.cos(toRadians(realLatitude))
    ) *
    180 /
    Math.PI;

  const latitude = Math.max(
    -90,
    Math.min(
      90,
      realLatitude + latitudeOffset,
    ),
  );

  const longitude = normalizeLongitude(
    realLongitude + longitudeOffset,
  );

  return {
    latitude,
    longitude,
  };
}
