import {
  HttpsError,
} from "firebase-functions/v2/https";
import {
  calculateDistanceMeters,
  calculatePoints,
  createApproximateMapCenter,
  getGameModeConfig,
} from "../../src/guesses/scoring";

describe("getGameModeConfig", () => {
  it("returns CITY configuration", () => {
    const config =
      getGameModeConfig("CITY");

    expect(config).toEqual({
      type: "CITY",
      displayName: "Miasto",
      maxScoringDistanceMeters: 2000,
      maxPoints: 10000,
      initialMapDiameterMeters: 36000,
      initialMapOffsetMaxMeters: 30000,
      scoringVersion: 1,
    });
  });

  it("rejects unsupported game mode", () => {
    expect(() =>
      getGameModeConfig("COUNTRY"),
    ).toThrow(HttpsError);

    try {
      getGameModeConfig("COUNTRY");
    } catch (error) {
      expect(error).toBeInstanceOf(
        HttpsError,
      );

      expect(
        (error as HttpsError).code,
      ).toBe("invalid-argument");
    }
  });
});

describe("calculateDistanceMeters", () => {
  it("returns zero for identical coordinates", () => {
    const distance =
      calculateDistanceMeters(
        50.0614,
        19.9383,
        50.0614,
        19.9383,
      );

    expect(distance).toBe(0);
  });

  it("calculates approximately 111.2 km for one latitude degree", () => {
    const distance =
      calculateDistanceMeters(
        0,
        0,
        1,
        0,
      );

    expect(distance).toBeCloseTo(
      111194.93,
      0,
    );
  });

  it("returns the same distance in both directions", () => {
    const first =
      calculateDistanceMeters(
        50.0614,
        19.9383,
        52.2297,
        21.0122,
      );

    const second =
      calculateDistanceMeters(
        52.2297,
        21.0122,
        50.0614,
        19.9383,
      );

    expect(first).toBeCloseTo(
      second,
      6,
    );
  });

  it("handles crossing the 180 degree meridian", () => {
    const distance =
      calculateDistanceMeters(
        0,
        179.9,
        0,
        -179.9,
      );

    expect(distance).toBeCloseTo(
      22238.99,
      0,
    );
  });
});

describe("calculatePoints", () => {
  const config =
    getGameModeConfig("CITY");

  it("returns maximum points for zero distance", () => {
    expect(
      calculatePoints(0, config),
    ).toBe(10000);
  });

  it("returns zero at maximum scoring distance", () => {
    expect(
      calculatePoints(2000, config),
    ).toBe(0);
  });

  it("returns zero above maximum scoring distance", () => {
    expect(
      calculatePoints(5000, config),
    ).toBe(0);
  });

  it("returns 8100 points at half maximum distance", () => {
    expect(
      calculatePoints(1000, config),
    ).toBe(8100);
  });

  it("always returns points inside valid range", () => {
    const distances = [
      0,
      1,
      100,
      500,
      1000,
      1500,
      1999,
      2000,
      5000,
    ];

    for (const distance of distances) {
      const points = calculatePoints(
        distance,
        config,
      );

      expect(points).toBeGreaterThanOrEqual(
        0,
      );

      expect(points).toBeLessThanOrEqual(
        config.maxPoints,
      );
    }
  });

  it("decreases score when distance increases", () => {
    const nearPoints =
      calculatePoints(250, config);

    const middlePoints =
      calculatePoints(1000, config);

    const farPoints =
      calculatePoints(1750, config);

    expect(nearPoints).toBeGreaterThan(
      middlePoints,
    );

    expect(middlePoints).toBeGreaterThan(
      farPoints,
    );
  });
});

describe("createApproximateMapCenter", () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("returns original position when maximum offset is zero", () => {
    const result =
      createApproximateMapCenter(
        50,
        20,
        0,
      );

    expect(result.latitude).toBe(50);
    expect(result.longitude).toBe(20);
  });

  it("limits generated offset to maximum distance", () => {
    jest
      .spyOn(Math, "random")
      .mockReturnValueOnce(0)
      .mockReturnValueOnce(1e-100)
      .mockReturnValueOnce(0.5);

    const result =
      createApproximateMapCenter(
        50,
        20,
        30000,
      );

    const distance =
      calculateDistanceMeters(
        50,
        20,
        result.latitude,
        result.longitude,
      );

    expect(distance).toBeGreaterThan(
      29999,
    );

    expect(distance).toBeLessThanOrEqual(
      30001,
    );
  });

  it("normalizes longitude after crossing 180 degrees", () => {
    jest
      .spyOn(Math, "random")
      .mockReturnValueOnce(0.25)
      .mockReturnValueOnce(1e-100)
      .mockReturnValueOnce(0.5);

    const result =
      createApproximateMapCenter(
        0,
        179.9,
        30000,
      );

    expect(result.longitude).toBeGreaterThanOrEqual(
      -180,
    );

    expect(result.longitude).toBeLessThanOrEqual(
      180,
    );

    expect(result.longitude).toBeLessThan(
      0,
    );
  });
});