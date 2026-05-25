package com.example.livegeoguessr.domain.model

data class SubmitGuessResult(
    val guessId: String,
    val postId: String,
    val gameMode: GameModeType,
    val distanceMeters: Double,
    val points: Int,
    val maxPoints: Int,
    val maxScoringDistanceMeters: Double,
    val scoringVersion: Int,
    val realLatitude: Double,
    val realLongitude: Double,
)