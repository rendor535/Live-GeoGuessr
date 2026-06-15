package com.example.livegeoguessr.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home : Screen("home", Icons.Default.Home)
    object Camera : Screen("camera", Icons.Default.CameraAlt)
    object Settings : Screen("settings", Icons.Default.Settings)
    object Profile : Screen("profile", Icons.Default.Person)
    object Friends : Screen("friends", Icons.Default.Person)
    object Login : Screen("login", Icons.Default.Person)
    object AddFriend : Screen("add_friend", Icons.Default.Person)
    object Posts : Screen("posts", Icons.Default.CameraAlt)
    object GuessedPosts : Screen("guessed_posts", Icons.Default.LocationOn)
    object Guess : Screen("guess/{postId}/{imageUrl}/{lat}/{lon}", Icons.Default.Home) {
        fun createRoute(
            postId: String,
            imageUrl: String,
            lat: Double,
            lon: Double
        ): String {
            return "guess/${Uri.encode(postId)}/${Uri.encode(imageUrl)}/$lat/$lon"
        }
    }

    object MyPostLocation : Screen(
        "my_post_location?postId={postId}&imageUrl={imageUrl}&lat={lat}&lon={lon}",
        Icons.Default.Home
    ) {
        fun createRoute(
            postId: String,
            imageUrl: String,
            lat: Double,
            lon: Double
        ): String {
            return "my_post_location" +
                    "?postId=${Uri.encode(postId)}" +
                    "&imageUrl=${Uri.encode(imageUrl)}" +
                    "&lat=$lat" +
                    "&lon=$lon"
        }
    }
}