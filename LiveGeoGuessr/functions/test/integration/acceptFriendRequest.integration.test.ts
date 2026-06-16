import {
  adminDb,
  createAuthenticatedClient,
  expectCallableError,
  invokeCallable,
  resetEmulators,
  seedFriendRequest,
  seedUser,
} from "./helpers/firebaseTestEnvironment";

beforeEach(resetEmulators);
afterAll(resetEmulators);

describe("acceptFriendRequest", () => {
  it("creates friend relation on both sides and increments counters", async () => {
    const receiver =
      await createAuthenticatedClient(
        "receiver",
      );

    const senderUid = "sender-user";

    await seedUser(senderUid);
    await seedUser(receiver.uid);

    await seedFriendRequest(
      "request-1",
      senderUid,
      receiver.uid,
    );

    const result =
      await invokeCallable<
        {requestId: string},
        {success: boolean}
      >(
        receiver.functions,
        "acceptFriendRequest",
        {
          requestId: "request-1",
        },
      );

    expect(result.success).toBe(true);

    const senderFriend =
      await adminDb
        .collection("users")
        .doc(senderUid)
        .collection("friends")
        .doc(receiver.uid)
        .get();

    const receiverFriend =
      await adminDb
        .collection("users")
        .doc(receiver.uid)
        .collection("friends")
        .doc(senderUid)
        .get();

    expect(senderFriend.exists).toBe(true);
    expect(receiverFriend.exists).toBe(true);

    const requestSnapshot =
      await adminDb
        .collection("friendRequests")
        .doc("request-1")
        .get();

    expect(
      requestSnapshot.get("status"),
    ).toBe("accepted");

    const senderSnapshot =
      await adminDb
        .collection("users")
        .doc(senderUid)
        .get();

    const receiverSnapshot =
      await adminDb
        .collection("users")
        .doc(receiver.uid)
        .get();

    expect(
      senderSnapshot.get(
        "stats.friendsCount",
      ),
    ).toBe(1);

    expect(
      receiverSnapshot.get(
        "stats.friendsCount",
      ),
    ).toBe(1);
  });

  it("blocks acceptance by sender", async () => {
    const sender =
      await createAuthenticatedClient(
        "sender",
      );

    await seedUser(sender.uid);
    await seedUser("receiver-user");

    await seedFriendRequest(
      "request-1",
      sender.uid,
      "receiver-user",
    );

    await expectCallableError(
      invokeCallable(
        sender.functions,
        "acceptFriendRequest",
        {
          requestId: "request-1",
        },
      ),
      "permission-denied",
    );
  });

  it("blocks request that is not pending", async () => {
    const receiver =
      await createAuthenticatedClient();

    await seedUser("sender-user");
    await seedUser(receiver.uid);

    await seedFriendRequest(
      "request-1",
      "sender-user",
      receiver.uid,
      "rejected",
    );

    await expectCallableError(
      invokeCallable(
        receiver.functions,
        "acceptFriendRequest",
        {
          requestId: "request-1",
        },
      ),
      "failed-precondition",
    );
  });

  it("does not increment counters after second acceptance attempt", async () => {
    const receiver =
      await createAuthenticatedClient();

    await seedUser("sender-user");
    await seedUser(receiver.uid);

    await seedFriendRequest(
      "request-1",
      "sender-user",
      receiver.uid,
    );

    await invokeCallable(
      receiver.functions,
      "acceptFriendRequest",
      {
        requestId: "request-1",
      },
    );

    await expectCallableError(
      invokeCallable(
        receiver.functions,
        "acceptFriendRequest",
        {
          requestId: "request-1",
        },
      ),
      "failed-precondition",
    );

    const senderSnapshot =
      await adminDb
        .collection("users")
        .doc("sender-user")
        .get();

    const receiverSnapshot =
      await adminDb
        .collection("users")
        .doc(receiver.uid)
        .get();

    expect(
      senderSnapshot.get(
        "stats.friendsCount",
      ),
    ).toBe(1);

    expect(
      receiverSnapshot.get(
        "stats.friendsCount",
      ),
    ).toBe(1);
  });
});