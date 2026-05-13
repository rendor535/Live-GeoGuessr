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
    object Login : Screen("login", Icons.Default.Person)
}