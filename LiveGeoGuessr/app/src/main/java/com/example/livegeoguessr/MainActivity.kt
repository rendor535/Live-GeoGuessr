package com.example.livegeoguessr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.livegeoguessr.ui.navigation.AppNavGraph
import com.example.livegeoguessr.ui.navigation.BottomBar
import com.example.livegeoguessr.ui.navigation.Screen
import com.example.livegeoguessr.ui.screens.settings.SettingsViewModel
import com.example.livegeoguessr.ui.theme.LiveGeoGuessrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SettingsViewModel = hiltViewModel()
            val darkMode by viewModel.darkMode.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()

            LiveGeoGuessrTheme(darkTheme = darkMode) {
                if (isLoggedIn != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f,
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.primary,
                                androidx.compose.ui.graphics.BlendMode.Color
                            )
                        )
                        MainScreen(isLoggedIn!!)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
        bottomBar = {
            if (currentRoute != Screen.Login.route) {
                BottomBar(navController)
            }
        }
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            isLoggedIn = isLoggedIn,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
