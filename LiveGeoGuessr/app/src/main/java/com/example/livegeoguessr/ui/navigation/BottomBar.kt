package com.example.livegeoguessr.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomBar(
    navController: NavHostController
) {
    val screens = listOf(
        Screen.Home,
        Screen.Camera,
        Screen.Settings
    )

    val currentBackStack =
        navController.currentBackStackEntryAsState()

    val currentRoute =
        currentBackStack.value?.destination?.route

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.route
                    )
                },
                label = { Text(text = screen.route.replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
