import {setGlobalOptions} from "firebase-functions";
import {onRequest} from "firebase-functions/https";
import * as logger from "firebase-functions/logger";

setGlobalOptions({maxInstances: 10});

export const health = onRequest((request, response) => {
  logger.info("Health check called");

  response.json({
    status: "ok",
    app: "Live GeoGuessr",
    backend: "Firebase Cloud Functions",
  });
});
