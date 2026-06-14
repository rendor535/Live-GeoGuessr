import {getApp, getApps, initializeApp} from "firebase-admin/app";
import {setGlobalOptions} from "firebase-functions/v2";

setGlobalOptions({
  region: "us-central1",
  maxInstances: 10,
});

export const firebaseApp =
  getApps().length > 0 ? getApp() : initializeApp();
