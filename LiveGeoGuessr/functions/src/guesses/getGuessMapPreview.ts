import {
  HttpsError,
  onCall,
} from "firebase-functions/v2/https";
import {requireAuthenticatedUid} from "../shared/authorization";
import {db} from "../shared/firestore";
import {
  assertValidCoordinates,
  requireString,
} from "../shared/validation";
import {
  createApproximateMapCenter,
  getGameModeConfig,
} from "./scoring";

type GetGuessMapPreviewData = {
  postId?: unknown;
  gameMode?: unknown;
};

export const getGuessMapPreview =
  onCall<GetGuessMapPreviewData>(
    async (request) => {
      requireAuthenticatedUid(request);

      const postId = requireString(
        request.data?.postId,
        "postId",
      );

      const gameMode = String(
        request.data?.gameMode ?? "CITY",
      );

      const config =
        getGameModeConfig(gameMode);

      const postSnapshot = await db
        .collection("posts")
        .doc(postId)
        .get();

      if (!postSnapshot.exists) {
        throw new HttpsError(
          "not-found",
          "Post not found.",
        );
      }

      const post = postSnapshot.data();

      if (!post) {
        throw new HttpsError(
          "not-found",
          "Post not found.",
        );
      }

      const realLatitude = Number(post.latitude);
      const realLongitude =
        Number(post.longitude);

      assertValidCoordinates(
        realLatitude,
        realLongitude,
      );

      const postGameMode = String(
        post.gameMode ?? "CITY",
      );

      if (postGameMode !== config.type) {
        throw new HttpsError(
          "failed-precondition",
          "Game mode does not match post game mode.",
        );
      }

      const approximateCenter =
        createApproximateMapCenter(
          realLatitude,
          realLongitude,
          config.initialMapOffsetMaxMeters,
        );

      return {
        postId,
        gameMode: config.type,
        initialMapCenterLatitude:
          approximateCenter.latitude,
        initialMapCenterLongitude:
          approximateCenter.longitude,
        initialMapDiameterMeters:
          config.initialMapDiameterMeters,
      };
    },
  );
