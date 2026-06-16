import {
  adminDb,
  createAuthenticatedClient,
  expectCallableError,
  getFriendRequestId,
  invokeCallable,
  resetEmulators,
  seedUser,
} from "./helpers/firebaseTestEnvironment";

beforeEach(resetEmulators);
afterAll(resetEmulators);

describe("sendFriendRequest", () => {
  it("creates pending friend request with public data", async () => {
    const sender =
      await createAuthenticatedClient(
        "sender",
      );

    await seedUser(sender.uid, {
      nickname: "senderNick",
      displayName: "Sender Name",
      photoUrl: "sender.jpg",
    });

    await seedUser("target-user", {
      nickname: "targetNick",
      displayName: "Target Name",
      photoUrl: "target.jpg",
    });

    const result =
      await invokeCallable<
        {toUid: string},
        {
          success: boolean;
          requestId: string;
        }
      >(
        sender.functions,
        "sendFriendRequest",
        {
          toUid: "target-user",
        },
      );

    const expectedRequestId =
      getFriendRequestId(
        sender.uid,
        "target-user",
      );

    expect(result).toEqual({
      success: true,
      requestId: expectedRequestId,
    });

    const requestSnapshot =
      await adminDb
        .collection("friendRequests")
        .doc(expectedRequestId)
        .get();

    expect(requestSnapshot.exists)
      .toBe(true);

    expect(
      requestSnapshot.get("status"),
    ).toBe("pending");

    expect(
      requestSnapshot.get(
        "fromNickname",
      ),
    ).toBe("senderNick");

    expect(
      requestSnapshot.get(
        "toNickname",
      ),
    ).toBe("targetNick");

    expect(
      requestSnapshot.get(
        "fromDisplayName",
      ),
    ).toBe("Sender Name");

    expect(
      requestSnapshot.get(
        "toDisplayName",
      ),
    ).toBe("Target Name");
  });

  it("blocks friend request to yourself", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await expectCallableError(
      invokeCallable(
        client.functions,
        "sendFriendRequest",
        {
          toUid: client.uid,
        },
      ),
      "invalid-argument",
    );
  });

  it("blocks duplicate pending request", async () => {
    const sender =
      await createAuthenticatedClient();

    await seedUser(sender.uid);
    await seedUser("target-user");

    const request = {
      toUid: "target-user",
    };

    await invokeCallable(
      sender.functions,
      "sendFriendRequest",
      request,
    );

    await expectCallableError(
      invokeCallable(
        sender.functions,
        "sendFriendRequest",
        request,
      ),
      "already-exists",
    );
  });

  it("blocks request when users are already friends", async () => {
    const sender =
      await createAuthenticatedClient();

    await seedUser(sender.uid);
    await seedUser("target-user");

    await adminDb
      .collection("users")
      .doc(sender.uid)
      .collection("friends")
      .doc("target-user")
      .set({
        uid: "target-user",
      });

    await expectCallableError(
      invokeCallable(
        sender.functions,
        "sendFriendRequest",
        {
          toUid: "target-user",
        },
      ),
      "already-exists",
    );
  });

  it("returns not-found when target user does not exist", async () => {
    const sender =
      await createAuthenticatedClient();

    await seedUser(sender.uid);

    await expectCallableError(
      invokeCallable(
        sender.functions,
        "sendFriendRequest",
        {
          toUid: "missing-user",
        },
      ),
      "not-found",
    );
  });
});