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

describe("rejectFriendRequest", () => {
  it("sets request status to rejected without creating friends", async () => {
    const receiver =
      await createAuthenticatedClient();

    await seedUser("sender-user");
    await seedUser(receiver.uid);

    await seedFriendRequest(
      "request-1",
      "sender-user",
      receiver.uid,
    );

    const result =
      await invokeCallable<
        {requestId: string},
        {success: boolean}
      >(
        receiver.functions,
        "rejectFriendRequest",
        {
          requestId: "request-1",
        },
      );

    expect(result.success).toBe(true);

    const requestSnapshot =
      await adminDb
        .collection("friendRequests")
        .doc("request-1")
        .get();

    expect(
      requestSnapshot.get("status"),
    ).toBe("rejected");

    expect(
      requestSnapshot.get("updatedAt"),
    ).toBeDefined();

    const senderFriend =
      await adminDb
        .collection("users")
        .doc("sender-user")
        .collection("friends")
        .doc(receiver.uid)
        .get();

    const receiverFriend =
      await adminDb
        .collection("users")
        .doc(receiver.uid)
        .collection("friends")
        .doc("sender-user")
        .get();

    expect(senderFriend.exists).toBe(false);
    expect(receiverFriend.exists).toBe(false);
  });

  it("blocks rejection by sender", async () => {
    const sender =
      await createAuthenticatedClient();

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
        "rejectFriendRequest",
        {
          requestId: "request-1",
        },
      ),
      "permission-denied",
    );
  });

  it("blocks rejection of accepted request", async () => {
    const receiver =
      await createAuthenticatedClient();

    await seedUser("sender-user");
    await seedUser(receiver.uid);

    await seedFriendRequest(
      "request-1",
      "sender-user",
      receiver.uid,
      "accepted",
    );

    await expectCallableError(
      invokeCallable(
        receiver.functions,
        "rejectFriendRequest",
        {
          requestId: "request-1",
        },
      ),
      "failed-precondition",
    );
  });
});