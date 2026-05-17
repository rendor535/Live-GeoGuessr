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
            HomeScreen()
        }
        composable(Screen.Camera.route) {
            CameraScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
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
    }
}
