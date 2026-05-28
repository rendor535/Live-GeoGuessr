package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.data.remote.firebase.FirebaseModule
import com.example.livegeoguessr.domain.model.GameModeType
import com.example.livegeoguessr.domain.model.SubmitGuessResult
import com.example.livegeoguessr.domain.model.GuessMapPreview
import kotlinx.coroutines.tasks.await

class GuessRepository {

    private val functions = FirebaseModule.functions
    private val firestore = FirebaseModule.firestore
    private val auth = FirebaseModule.auth

    suspend fun submitGuess(
        postId: String,
        guessedLatitude: Double,
        guessedLongitude: Double,
        gameMode: GameModeType = GameModeType.CITY
    ): SubmitGuessResult {
        val data = hashMapOf(
            "postId" to postId,
            "guessedLatitude" to guessedLatitude,
            "guessedLongitude" to guessedLongitude,
            "gameMode" to gameMode.name
        )

        val response = functions
            .getHttpsCallable("submitGuess")
            .call(data)
            .await()

        val result = response.data as Map<*, *>

        return SubmitGuessResult(
            guessId = result["guessId"] as String,
            postId = result["postId"] as String,
            gameMode = GameModeType.valueOf(result["gameMode"] as String),
            distanceMeters = (result["distanceMeters"] as Number).toDouble(),
            points = (result["points"] as Number).toInt(),
            maxPoints = (result["maxPoints"] as Number).toInt(),
            maxScoringDistanceMeters = (result["maxScoringDistanceMeters"] as Number).toDouble(),
            scoringVersion = (result["scoringVersion"] as Number).toInt(),
            guessedLatitude = (result["guessedLatitude"] as Number).toDouble(),
            guessedLongitude = (result["guessedLongitude"] as Number).toDouble(),
            realLatitude = (result["realLatitude"] as Number).toDouble(),
            realLongitude = (result["realLongitude"] as Number).toDouble()
        )
    }

    suspend fun getMyGuessForPost(postId: String): SubmitGuessResult? {
        val uid = auth.currentUser?.uid ?: return null

        val snapshot = firestore
            .collection("guesses")
            .document("${uid}_${postId}")
            .get()
            .await()

        if (!snapshot.exists()) {
            return null
        }

        return SubmitGuessResult(
            guessId = snapshot.getString("id") ?: snapshot.id,
            postId = snapshot.getString("postId") ?: postId,
            gameMode = GameModeType.valueOf(snapshot.getString("gameMode") ?: GameModeType.CITY.name),
            distanceMeters = snapshot.getDouble("distanceMeters") ?: 0.0,
            points = snapshot.getLong("points")?.toInt() ?: 0,
            maxPoints = snapshot.getLong("maxPoints")?.toInt() ?: 10_000,
            maxScoringDistanceMeters = snapshot.getDouble("maxScoringDistanceMeters") ?: 2_000.0,
            scoringVersion = snapshot.getLong("scoringVersion")?.toInt() ?: 1,
            guessedLatitude = snapshot.getDouble("guessedLatitude") ?: 0.0,
            guessedLongitude = snapshot.getDouble("guessedLongitude") ?: 0.0,
            realLatitude = snapshot.getDouble("realLatitude") ?: 0.0,
            realLongitude = snapshot.getDouble("realLongitude") ?: 0.0
        )
    }

    suspend fun getGuessMapPreview(
        postId: String,
        gameMode: GameModeType
    ): GuessMapPreview {
        val data = hashMapOf(
            "postId" to postId,
            "gameMode" to gameMode.name
        )

        val result = functions
            .getHttpsCallable("getGuessMapPreview")
            .call(data)
            .await()

        val map = result.data as Map<*, *>

        return GuessMapPreview(
            postId = map["postId"] as String,
            gameMode = GameModeType.valueOf(map["gameMode"] as String),
            initialMapCenterLatitude =
                (map["initialMapCenterLatitude"] as Number).toDouble(),
            initialMapCenterLongitude =
                (map["initialMapCenterLongitude"] as Number).toDouble(),
            initialMapDiameterMeters =
                (map["initialMapDiameterMeters"] as Number).toDouble()
        )
    }
}