package com.example.livegeoguessr.domain.model

data class GuessedPost(
    val post: Post,
    val distanceMeters: Double,
    val points: Int
)