package com.example.livegeoguessr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.livegeoguessr.ui.screens.home.HomeScreen
import com.example.livegeoguessr.ui.screens.auth.AuthScreen
import com.example.livegeoguessr.ui.screens.camera.CameraScreen
import com.example.livegeoguessr.ui.screens.settings.SettingsScreen
import com.example.livegeoguessr.ui.screens.settings.ProfileScreen
import com.example.livegeoguessr.ui.screens.guess.GuessScreen
import com.example.livegeoguessr.ui.screens.friends.FriendsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Camera.route) {
            CameraScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.Friends.route) {
            FriendsScreen(navController = navController)
        }
        composable (Screen.Login.route) {
            AuthScreen(
                onSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Guess.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""

            GuessScreen(
                postId = postId,
                imageUrl = imageUrl,
            )
        }
    }
}
