package com.example.livegeoguessr.domain.model

data class GuessMapPreview(
    val postId: String,
    val gameMode: GameModeType,
    val initialMapCenterLatitude: Double,
    val initialMapCenterLongitude: Double,
    val initialMapDiameterMeters: Double
)