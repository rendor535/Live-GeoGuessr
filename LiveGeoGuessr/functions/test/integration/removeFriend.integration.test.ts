import {
  adminDb,
  createAuthenticatedClient,
  expectCallableError,
  getFriendRequestId,
  invokeCallable,
  resetEmulators,
  seedFriendRequest,
  seedUser,
} from "./helpers/firebaseTestEnvironment";

beforeEach(resetEmulators);
afterAll(resetEmulators);

describe("removeFriend", () => {
  it("removes both relations and decrements counters", async () => {
    const client =
      await createAuthenticatedClient();

    const friendUid = "friend-user";

    await seedUser(client.uid, {
      stats: {
        friendsCount: 1,
      },
    });

    await seedUser(friendUid, {
      stats: {
        friendsCount: 1,
      },
    });

    await adminDb
      .collection("users")
      .doc(client.uid)
      .collection("friends")
      .doc(friendUid)
      .set({
        uid: friendUid,
      });

    await adminDb
      .collection("users")
      .doc(friendUid)
      .collection("friends")
      .doc(client.uid)
      .set({
        uid: client.uid,
      });

    const requestId =
      getFriendRequestId(
        client.uid,
        friendUid,
      );

    await seedFriendRequest(
      requestId,
      client.uid,
      friendUid,
      "accepted",
    );

    const result =
      await invokeCallable<
        {friendUid: string},
        {success: boolean}
      >(
        client.functions,
        "removeFriend",
        {
          friendUid,
        },
      );

    expect(result.success).toBe(true);

    const myFriend =
      await adminDb
        .collection("users")
        .doc(client.uid)
        .collection("friends")
        .doc(friendUid)
        .get();

    const otherFriend =
      await adminDb
        .collection("users")
        .doc(friendUid)
        .collection("friends")
        .doc(client.uid)
        .get();

    expect(myFriend.exists).toBe(false);
    expect(otherFriend.exists).toBe(false);

    const myUser =
      await adminDb
        .collection("users")
        .doc(client.uid)
        .get();

    const otherUser =
      await adminDb
        .collection("users")
        .doc(friendUid)
        .get();

    expect(
      myUser.get("stats.friendsCount"),
    ).toBe(0);

    expect(
      otherUser.get(
        "stats.friendsCount",
      ),
    ).toBe(0);

    const request =
      await adminDb
        .collection("friendRequests")
        .doc(requestId)
        .get();

    expect(
      request.get("status"),
    ).toBe("removed");
  });

  it("blocks removing yourself", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await expectCallableError(
      invokeCallable(
        client.functions,
        "removeFriend",
        {
          friendUid: client.uid,
        },
      ),
      "invalid-argument",
    );
  });

  it("returns not-found when relation does not exist", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);
    await seedUser("other-user");

    await expectCallableError(
      invokeCallable(
        client.functions,
        "removeFriend",
        {
          friendUid: "other-user",
        },
      ),
      "not-found",
    );
  });
});