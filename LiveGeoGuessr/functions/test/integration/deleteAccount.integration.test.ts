import {
  Timestamp,
} from "firebase-admin/firestore";
import {
  adminAuth,
  adminBucket,
  adminDb,
  createAuthenticatedClient,
  expectCallableError,
  invokeCallable,
  resetEmulators,
  seedFriendRequest,
  seedPost,
  seedUser,
} from "./helpers/firebaseTestEnvironment";

beforeEach(resetEmulators);
afterAll(resetEmulators);

describe("deleteAccount", () => {
  it("requires confirm true", async () => {
    const client =
      await createAuthenticatedClient();

    await seedUser(client.uid);

    await expectCallableError(
      invokeCallable(
        client.functions,
        "deleteAccount",
        {
          confirm: false,
        },
      ),
      "invalid-argument",
    );

    const userSnapshot =
      await adminDb
        .collection("users")
        .doc(client.uid)
        .get();

    expect(userSnapshot.exists).toBe(true);
  });

  it("deletes account and all related data", async () => {
    const deletedUser =
      await createAuthenticatedClient(
        "deleted",
      );

    const friend =
      await createAuthenticatedClient(
        "friend",
      );

    const deletedUid =
      deletedUser.uid;

    const friendUid =
      friend.uid;

    await seedUser(deletedUid, {
      stats: {
        friendsCount: 1,
        guessesCount: 1,
        pointsTotal: 500,
        bestGuessMeters: 100,
      },
    });

    await seedUser(friendUid, {
      stats: {
        friendsCount: 1,
        guessesCount: 2,
        pointsTotal: 1000,
        bestGuessMeters: 20,
      },
    });

    await seedUser("third-user");

    await seedPost(
      "deleted-post",
      deletedUid,
    );

    await seedPost(
      "surviving-post",
      "third-user",
    );

    /*
     * Zgadywanie wykonane przez
     * usuwanego użytkownika.
     */
    await adminDb
      .collection("guesses")
      .doc(`${deletedUid}_surviving-post`)
      .set({
        id:
          `${deletedUid}_surviving-post`,
        userUid: deletedUid,
        postId: "surviving-post",
        points: 500,
        distanceMeters: 100,
        createdAt: Timestamp.now(),
      });

    /*
     * Wynik znajomego dotyczący posta
     * usuwanego użytkownika.
     * Powinien zostać usunięty.
     */
    await adminDb
      .collection("guesses")
      .doc(`${friendUid}_deleted-post`)
      .set({
        id:
          `${friendUid}_deleted-post`,
        userUid: friendUid,
        postId: "deleted-post",
        points: 400,
        distanceMeters: 20,
        createdAt: Timestamp.now(),
      });

    /*
     * Wynik znajomego, który powinien
     * pozostać i zostać użyty do
     * ponownego obliczenia statystyk.
     */
    await adminDb
      .collection("guesses")
      .doc(`${friendUid}_surviving-post`)
      .set({
        id:
          `${friendUid}_surviving-post`,
        userUid: friendUid,
        postId: "surviving-post",
        points: 600,
        distanceMeters: 50,
        createdAt: Timestamp.now(),
      });

    await adminDb
      .collection("users")
      .doc(deletedUid)
      .collection("friends")
      .doc(friendUid)
      .set({
        uid: friendUid,
      });

    await adminDb
      .collection("users")
      .doc(friendUid)
      .collection("friends")
      .doc(deletedUid)
      .set({
        uid: deletedUid,
      });

    await seedFriendRequest(
      "sent-request",
      deletedUid,
      "third-user",
    );

    await seedFriendRequest(
      "received-request",
      "third-user",
      deletedUid,
    );

    await adminBucket
      .file(
        `avatars/${deletedUid}/avatar.jpg`,
      )
      .save(
        Buffer.from("avatar"),
        {
          contentType: "image/jpeg",
        },
      );

    await adminBucket
      .file(
        `posts/${deletedUid}/post.jpg`,
      )
      .save(
        Buffer.from("post"),
        {
          contentType: "image/jpeg",
        },
      );

    const result =
      await invokeCallable<
        {confirm: boolean},
        {success: boolean}
      >(
        deletedUser.functions,
        "deleteAccount",
        {
          confirm: true,
        },
      );

    expect(result.success).toBe(true);

    await expect(
      adminAuth.getUser(deletedUid),
    ).rejects.toMatchObject({
      code: "auth/user-not-found",
    });

    const deletedProfile =
      await adminDb
        .collection("users")
        .doc(deletedUid)
        .get();

    expect(deletedProfile.exists)
      .toBe(false);

    const deletedPost =
      await adminDb
        .collection("posts")
        .doc("deleted-post")
        .get();

    expect(deletedPost.exists)
      .toBe(false);

    const ownGuess =
      await adminDb
        .collection("guesses")
        .doc(
          `${deletedUid}_surviving-post`,
        )
        .get();

    expect(ownGuess.exists).toBe(false);

    const guessForDeletedPost =
      await adminDb
        .collection("guesses")
        .doc(
          `${friendUid}_deleted-post`,
        )
        .get();

    expect(
      guessForDeletedPost.exists,
    ).toBe(false);

    const survivingGuess =
      await adminDb
        .collection("guesses")
        .doc(
          `${friendUid}_surviving-post`,
        )
        .get();

    expect(survivingGuess.exists)
      .toBe(true);

    const sentRequest =
      await adminDb
        .collection("friendRequests")
        .doc("sent-request")
        .get();

    const receivedRequest =
      await adminDb
        .collection("friendRequests")
        .doc("received-request")
        .get();

    expect(sentRequest.exists).toBe(false);
    expect(receivedRequest.exists)
      .toBe(false);

    const reciprocalFriend =
      await adminDb
        .collection("users")
        .doc(friendUid)
        .collection("friends")
        .doc(deletedUid)
        .get();

    expect(reciprocalFriend.exists)
      .toBe(false);

    const friendProfile =
      await adminDb
        .collection("users")
        .doc(friendUid)
        .get();

    expect(
      friendProfile.get(
        "stats.friendsCount",
      ),
    ).toBe(0);

    expect(
      friendProfile.get(
        "stats.guessesCount",
      ),
    ).toBe(1);

    expect(
      friendProfile.get(
        "stats.pointsTotal",
      ),
    ).toBe(600);

    expect(
      friendProfile.get(
        "stats.bestGuessMeters",
      ),
    ).toBe(50);

    const [avatarFiles] =
      await adminBucket.getFiles({
        prefix:
          `avatars/${deletedUid}/`,
      });

    const [postFiles] =
      await adminBucket.getFiles({
        prefix:
          `posts/${deletedUid}/`,
      });

    expect(avatarFiles).toHaveLength(0);
    expect(postFiles).toHaveLength(0);
  });
});