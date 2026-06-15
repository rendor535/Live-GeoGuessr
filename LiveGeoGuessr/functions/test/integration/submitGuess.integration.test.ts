import {
  adminDb,
  createAuthenticatedClient,
  expectCallableError,
  invokeCallable,
  resetEmulators,
  seedPost,
  seedUser,
} from "./helpers/firebaseTestEnvironment";

type SubmitGuessResponse = {
  guessId: string;
  postId: string;
  gameMode: string;
  distanceMeters: number;
  points: number;
  realLatitude: number;
  realLongitude: number;
  guessedLatitude: number;
  guessedLongitude: number;
};

beforeEach(resetEmulators);
afterAll(resetEmulators);

describe("submitGuess", () => {
  it("creates guess and updates user stats", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);
    await seedUser("owner-1");

    await seedPost(
      "post-1",
      "owner-1",
      {
        latitude: 50,
        longitude: 20,
      },
    );

    const result =
      await invokeCallable<
        {
          postId: string;
          guessedLatitude: number;
          guessedLongitude: number;
          gameMode: string;
        },
        SubmitGuessResponse
      >(
        client.functions,
        "submitGuess",
        {
          postId: "post-1",
          guessedLatitude: 50,
          guessedLongitude: 20,
          gameMode: "CITY",
        },
      );

    expect(result.guessId).toBe(
      `${client.uid}_post-1`,
    );

    expect(result.distanceMeters)
      .toBeCloseTo(0, 5);

    expect(result.points).toBe(10000);

    const guessSnapshot =
      await adminDb
        .collection("guesses")
        .doc(`${client.uid}_post-1`)
        .get();

    expect(guessSnapshot.exists)
      .toBe(true);

    expect(
      guessSnapshot.get("userUid"),
    ).toBe(client.uid);

    expect(
      guessSnapshot.get("postId"),
    ).toBe("post-1");

    expect(
      guessSnapshot.get("points"),
    ).toBe(10000);

    const userSnapshot =
      await adminDb
        .collection("users")
        .doc(client.uid)
        .get();

    expect(
      userSnapshot.get(
        "stats.pointsTotal",
      ),
    ).toBe(10000);

    expect(
      userSnapshot.get(
        "stats.guessesCount",
      ),
    ).toBe(1);

    expect(
      userSnapshot.get(
        "stats.bestGuessMeters",
      ),
    ).toBeCloseTo(0, 5);
  });

  it("does not replace better best guess with worse result", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);
    await seedUser("owner-1");

    await seedPost(
      "post-best",
      "owner-1",
      {
        latitude: 50,
        longitude: 20,
      },
    );

    await seedPost(
      "post-worse",
      "owner-1",
      {
        latitude: 50,
        longitude: 20,
      },
    );

    await invokeCallable(
      client.functions,
      "submitGuess",
      {
        postId: "post-best",
        guessedLatitude: 50,
        guessedLongitude: 20,
        gameMode: "CITY",
      },
    );

    await invokeCallable(
      client.functions,
      "submitGuess",
      {
        postId: "post-worse",
        guessedLatitude: 50.01,
        guessedLongitude: 20,
        gameMode: "CITY",
      },
    );

    const userSnapshot =
      await adminDb
        .collection("users")
        .doc(client.uid)
        .get();

    expect(
      userSnapshot.get(
        "stats.guessesCount",
      ),
    ).toBe(2);

    expect(
      userSnapshot.get(
        "stats.bestGuessMeters",
      ),
    ).toBeCloseTo(0, 5);
  });

  it("blocks second guess for the same post", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);
    await seedUser("owner-1");
    await seedPost("post-1", "owner-1");

    const request = {
      postId: "post-1",
      guessedLatitude: 50,
      guessedLongitude: 20,
      gameMode: "CITY",
    };

    await invokeCallable(
      client.functions,
      "submitGuess",
      request,
    );

    await expectCallableError(
      invokeCallable(
        client.functions,
        "submitGuess",
        request,
      ),
      "already-exists",
    );
  });

  it("blocks guessing own post", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await seedPost(
      "own-post",
      client.uid,
    );

    await expectCallableError(
      invokeCallable(
        client.functions,
        "submitGuess",
        {
          postId: "own-post",
          guessedLatitude: 50,
          guessedLongitude: 20,
          gameMode: "CITY",
        },
      ),
      "permission-denied",
    );
  });

  it("rejects user without Firestore profile", async () => {
    const client =
      await createAuthenticatedClient();

    await seedPost(
      "post-1",
      "owner-1",
    );

    await expectCallableError(
      invokeCallable(
        client.functions,
        "submitGuess",
        {
          postId: "post-1",
          guessedLatitude: 50,
          guessedLongitude: 20,
          gameMode: "CITY",
        },
      ),
      "failed-precondition",
    );
  });

  it("returns not-found for missing post", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await expectCallableError(
      invokeCallable(
        client.functions,
        "submitGuess",
        {
          postId: "missing-post",
          guessedLatitude: 50,
          guessedLongitude: 20,
          gameMode: "CITY",
        },
      ),
      "not-found",
    );
  });

  it("rejects mismatched game mode", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);
    await seedUser("owner-1");

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
        "submitGuess",
        {
          postId: "post-mode",
          guessedLatitude: 50,
          guessedLongitude: 20,
          gameMode: "CITY",
        },
      ),
      "failed-precondition",
    );
  });
});