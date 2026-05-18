package com.example.livegeoguessr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.livegeoguessr.ui.navigation.AppNavGraph
import com.example.livegeoguessr.ui.navigation.BottomBar
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

            LiveGeoGuessrTheme(darkTheme = darkMode) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { paddingValues ->
        AppNavGraph(navController, modifier = Modifier.padding(paddingValues))
    }
}