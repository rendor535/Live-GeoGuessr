package com.example.livegeoguessr.domain.model

data class GameModeConfig(
    val type: GameModeType,
    val displayName: String,
    val maxScoringDistanceMeters: Double,
    val maxPoints: Int,
    val initialMapDiameterMeters: Double,
    val initialMapOffsetMaxMeters: Double,
    val scoringVersion: Int
)

// możliwość dodawania innych trybów z innymi stałymi
object GameModeConfigs {
    fun getConfig(type: GameModeType): GameModeConfig {
        return when (type) {
            GameModeType.CITY -> GameModeConfig(
                type = GameModeType.CITY,
                displayName = "Miasto",
                maxScoringDistanceMeters = 2_000.0,
                maxPoints = 10_000,
                initialMapDiameterMeters = 36000.0,
                initialMapOffsetMaxMeters = 30000.0,
                scoringVersion = 1
            )
        }
    }
}