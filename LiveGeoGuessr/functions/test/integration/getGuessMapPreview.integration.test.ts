import {
  calculateDistanceMeters,
} from "../../src/guesses/scoring";
import {
  createAnonymousClient,
  createAuthenticatedClient,
  expectCallableError,
  invokeCallable,
  resetEmulators,
  seedPost,
  seedUser,
} from "./helpers/firebaseTestEnvironment";

type PreviewResponse = {
  postId: string;
  gameMode: string;
  initialMapCenterLatitude: number;
  initialMapCenterLongitude: number;
  initialMapDiameterMeters: number;
};

beforeEach(resetEmulators);
afterAll(resetEmulators);

describe("getGuessMapPreview", () => {
  it("rejects unauthenticated user", async () => {
    const client =
      createAnonymousClient();

    await expectCallableError(
      invokeCallable(
        client.functions,
        "getGuessMapPreview",
        {
          postId: "post-1",
          gameMode: "CITY",
        },
      ),
      "unauthenticated",
    );
  });

  it("rejects missing postId", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await expectCallableError(
      invokeCallable(
        client.functions,
        "getGuessMapPreview",
        {
          gameMode: "CITY",
        },
      ),
      "invalid-argument",
    );
  });

  it("returns not-found for missing post", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await expectCallableError(
      invokeCallable(
        client.functions,
        "getGuessMapPreview",
        {
          postId: "missing-post",
          gameMode: "CITY",
        },
      ),
      "not-found",
    );
  });

  it("rejects invalid post coordinates", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await seedPost(
      "post-invalid",
      "owner-1",
      {
        latitude: 100,
      },
    );

    await expectCallableError(
      invokeCallable(
        client.functions,
        "getGuessMapPreview",
        {
          postId: "post-invalid",
          gameMode: "CITY",
        },
      ),
      "invalid-argument",
    );
  });

  it("rejects mismatched game mode", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await seedPost(
      "post-mode",
      "owner-1",
      {
        gameMode: "OTHER",
      },
    );

    await expectCallableError(
      invokeCallable(
        client.functions,
        "getGuessMapPreview",
        {
          postId: "post-mode",
          gameMode: "CITY",
        },
      ),
      "failed-precondition",
    );
  });

  it("returns valid approximate map center", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    const realLatitude = 50.0614;
    const realLongitude = 19.9383;

    await seedPost(
      "post-success",
      "owner-1",
      {
        latitude: realLatitude,
        longitude: realLongitude,
      },
    );

    const result =
      await invokeCallable<
        {
          postId: string;
          gameMode: string;
        },
        PreviewResponse
      >(
        client.functions,
        "getGuessMapPreview",
        {
          postId: "post-success",
          gameMode: "CITY",
        },
      );

    expect(result.postId).toBe(
      "post-success",
    );

    expect(result.gameMode).toBe(
      "CITY",
    );

    expect(
      result.initialMapDiameterMeters,
    ).toBe(36000);

    const offsetMeters =
      calculateDistanceMeters(
        realLatitude,
        realLongitude,
        result.initialMapCenterLatitude,
        result.initialMapCenterLongitude,
      );

    expect(offsetMeters)
      .toBeLessThanOrEqual(30001);
  });
});