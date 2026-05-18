package com.example.livegeoguessr.domain.model

data class UserStats(
    val pointsTotal: Int = 0,
    val guessesCount: Int = 0,
    val postsCount: Int = 0,
    val friendsCount: Int = 0,
    val bestGuessMeters: Double? = null
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val nickname: String = "",
    val photoUrl: String? = null,
    val avatarPath: String? = null,
    val isBanned: Boolean = false,
    val stats: UserStats = UserStats()
)