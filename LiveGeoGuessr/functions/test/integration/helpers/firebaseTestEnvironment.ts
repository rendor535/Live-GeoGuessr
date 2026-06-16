import {randomUUID} from "node:crypto";

import {
  getApps as getAdminApps,
  initializeApp as initializeAdminApp,
} from "firebase-admin/app";
import {getAuth as getAdminAuth} from "firebase-admin/auth";
import {
  getFirestore,
  Timestamp,
} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";

import type {
  FirebaseApp as ClientFirebaseApp,
} from "firebase/app";
import {
  deleteApp as deleteClientApp,
  initializeApp as initializeClientApp,
} from "firebase/app";
import type {Auth} from "firebase/auth";
import {
  connectAuthEmulator,
  createUserWithEmailAndPassword,
  getAuth as getClientAuth,
} from "firebase/auth";
import type {Functions} from "firebase/functions";
import {
  connectFunctionsEmulator,
  getFunctions,
  httpsCallable,
} from "firebase/functions";

export const PROJECT_ID =
  "demo-live-geoguessr";

/*
 * Musi odpowiadać regionowi z:
 *
 * setGlobalOptions({
 *   region: "us-central1"
 * })
 */
export const REGION = "us-central1";

export const STORAGE_BUCKET =
  `${PROJECT_ID}.appspot.com`;

const AUTH_EMULATOR_HOST =
  process.env.FIREBASE_AUTH_EMULATOR_HOST ??
  "127.0.0.1:9099";

const FIRESTORE_EMULATOR_HOST =
  process.env.FIRESTORE_EMULATOR_HOST ??
  "127.0.0.1:8080";

const FUNCTIONS_EMULATOR_HOST =
  "127.0.0.1:5001";

const ADMIN_APP_NAME =
  "integration-tests-admin";

function splitHost(
  value: string,
): {
  host: string;
  port: number;
} {
  const normalized = value
    .replace("http://", "")
    .replace("https://", "");

  const [host, port] =
    normalized.split(":");

  return {
    host,
    port: Number(port),
  };
}

const existingAdminApp =
  getAdminApps().find(
    (app) =>
      app.name === ADMIN_APP_NAME,
  );

export const adminApp =
  existingAdminApp ??
  initializeAdminApp(
    {
      projectId: PROJECT_ID,
      storageBucket: STORAGE_BUCKET,
    },
    ADMIN_APP_NAME,
  );

export const adminDb =
  getFirestore(adminApp);

export const adminAuth =
  getAdminAuth(adminApp);

export const adminBucket =
  getStorage(adminApp).bucket(
    STORAGE_BUCKET,
  );

export type TestClient = {
  app: ClientFirebaseApp;
  auth: Auth;
  functions: Functions;
  uid?: string;
};

const clientApps: ClientFirebaseApp[] = [];

function createBaseClient(): TestClient {
  const app = initializeClientApp(
    {
      projectId: PROJECT_ID,
      apiKey: "fake-api-key",
      authDomain:
        `${PROJECT_ID}.firebaseapp.com`,
      storageBucket: STORAGE_BUCKET,
      appId: `test-${randomUUID()}`,
    },
    `client-${randomUUID()}`,
  );

  clientApps.push(app);

  const auth = getClientAuth(app);

  connectAuthEmulator(
    auth,
    `http://${AUTH_EMULATOR_HOST}`,
    {
      disableWarnings: true,
    },
  );

  const functions =
    getFunctions(app, REGION);

  const functionsHost =
    splitHost(
      FUNCTIONS_EMULATOR_HOST,
    );

  connectFunctionsEmulator(
    functions,
    functionsHost.host,
    functionsHost.port,
  );

  return {
    app,
    auth,
    functions,
  };
}

export function createAnonymousClient():
TestClient {
  return createBaseClient();
}

export async function
createAuthenticatedClient(
  prefix = "user",
): Promise<TestClient & {uid: string}> {
  const client = createBaseClient();

  const email =
    `${prefix}-${randomUUID()}@test.local`;

  const credentials =
    await createUserWithEmailAndPassword(
      client.auth,
      email,
      "Test123456!",
    );

  return {
    ...client,
    uid: credentials.user.uid,
  };
}

export async function invokeCallable<
  RequestData,
  ResponseData,
>(
  functions: Functions,
  functionName: string,
  data: RequestData,
): Promise<ResponseData> {
  const callable = httpsCallable<
    RequestData,
    ResponseData
  >(
    functions,
    functionName,
  );

  const result = await callable(data);

  return result.data;
}

export async function expectCallableError(
  promise: Promise<unknown>,
  expectedCode: string,
): Promise<void> {
  try {
    await promise;
  } catch (error) {
    expect(
      (error as {code?: string}).code,
    ).toBe(`functions/${expectedCode}`);

    return;
  }

  throw new Error(
    `Expected functions/${expectedCode}.`,
  );
}

type UserStats = {
  pointsTotal: number;
  guessesCount: number;
  postsCount: number;
  friendsCount: number;
  bestGuessMeters: number | null;
};

export async function seedUser(
  uid: string,
  overrides:
    Record<string, unknown> = {},
): Promise<void> {
  const defaultStats: UserStats = {
    pointsTotal: 0,
    guessesCount: 0,
    postsCount: 0,
    friendsCount: 0,
    bestGuessMeters: null,
  };

  const overrideStats =
    typeof overrides.stats === "object" &&
    overrides.stats !== null ?
      overrides.stats as
        Partial<UserStats> :
      {};

  const {
    stats: _ignoredStats,
    ...otherOverrides
  } = overrides;

  await adminDb
    .collection("users")
    .doc(uid)
    .set({
      uid,
      email: `${uid}@test.local`,
      nickname: `nickname-${uid}`,
      displayName: `User ${uid}`,
      photoUrl: null,
      avatarPath: null,
      isBanned: false,
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now(),
      ...otherOverrides,
      stats: {
        ...defaultStats,
        ...overrideStats,
      },
    });
}

export async function seedPost(
  postId: string,
  ownerUid: string,
  overrides:
    Record<string, unknown> = {},
): Promise<void> {
  await adminDb
    .collection("posts")
    .doc(postId)
    .set({
      userId: ownerUid,
      user: `User ${ownerUid}`,
      imageUrl:
        `https://example.test/${postId}.jpg`,
      latitude: 50.0614,
      longitude: 19.9383,
      gameMode: "CITY",
      createdAt: Timestamp.now(),
      ...overrides,
    });
}

export async function seedFriendRequest(
  requestId: string,
  fromUid: string,
  toUid: string,
  status = "pending",
): Promise<void> {
  await adminDb
    .collection("friendRequests")
    .doc(requestId)
    .set({
      fromUid,
      toUid,
      fromNickname:
        `nickname-${fromUid}`,
      fromDisplayName:
        `User ${fromUid}`,
      fromPhotoUrl: null,
      toNickname:
        `nickname-${toUid}`,
      toDisplayName:
        `User ${toUid}`,
      toPhotoUrl: null,
      status,
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now(),
    });
}

export function getFriendRequestId(
  uidA: string,
  uidB: string,
): string {
  return [uidA, uidB]
    .sort()
    .join("_");
}

async function clearFirestore():
Promise<void> {
  const response = await fetch(
    `http://${FIRESTORE_EMULATOR_HOST}` +
    `/emulator/v1/projects/${PROJECT_ID}` +
    "/databases/(default)/documents",
    {
      method: "DELETE",
    },
  );

  if (!response.ok) {
    throw new Error(
      `Cannot clear Firestore: ${
        response.status
      }`,
    );
  }
}

async function clearAuthentication():
Promise<void> {
  const response = await fetch(
    `http://${AUTH_EMULATOR_HOST}` +
    `/emulator/v1/projects/${PROJECT_ID}` +
    "/accounts",
    {
      method: "DELETE",
    },
  );

  if (!response.ok) {
    throw new Error(
      `Cannot clear Auth: ${
        response.status
      }`,
    );
  }
}

async function clearStorage():
Promise<void> {
  const [files] =
    await adminBucket.getFiles();

  await Promise.all(
    files.map((file) => file.delete()),
  );
}

async function closeClientApps():
Promise<void> {
  const apps =
    clientApps.splice(
      0,
      clientApps.length,
    );

  await Promise.all(
    apps.map((app) =>
      deleteClientApp(app),
    ),
  );
}

export async function resetEmulators():
Promise<void> {
  await closeClientApps();

  await clearFirestore();
  await clearAuthentication();
  await clearStorage();
}