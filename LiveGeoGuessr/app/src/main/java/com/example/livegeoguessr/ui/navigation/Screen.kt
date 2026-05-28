package com.example.livegeoguessr.ui.navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home : Screen("home", Icons.Default.Home)
    object Camera : Screen("camera", Icons.Default.CameraAlt)
    object Settings : Screen("settings", Icons.Default.Settings)
    object Profile : Screen("profile", Icons.Default.Person)
    object Friends : Screen("friends", Icons.Default.Person)
    object Login : Screen("login", Icons.Default.Person)
    object Guess : Screen("guess/{postId}/{imageUrl}/{lat}/{lon}", Icons.Default.Home) {
        fun createRoute(
            postId: String,
            imageUrl: String,
            lat: Double,
            lon: Double
        ): String {
            val encodedPostId = java.net.URLEncoder.encode(postId, "UTF-8")
            val encodedUrl = java.net.URLEncoder.encode(imageUrl, "UTF-8")

            return "guess/$encodedPostId/$encodedUrl/$lat/$lon"
        }
    }
}