import "./shared/firebase";

import * as logger from "firebase-functions/logger";
import {onRequest} from "firebase-functions/v2/https";

export {
  sendFriendRequest,
} from "./friends/sendFriendRequest";

export {
  acceptFriendRequest,
} from "./friends/acceptFriendRequest";

export {
  rejectFriendRequest,
} from "./friends/rejectFriendRequest";

export {
  removeFriend,
} from "./friends/removeFriend";

export {
  submitGuess,
} from "./guesses/submitGuess";

export {
  getGuessMapPreview,
} from "./guesses/getGuessMapPreview";

export {
  deleteAccount,
} from "./accounts/deleteAccount";

export const health = onRequest(
  (_request, response) => {
    logger.info("Health check called");

    response.json({
      status: "ok",
      app: "Live GeoGuessr",
      backend:
        "Firebase Cloud Functions",
    });
  },
);
