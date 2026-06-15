package com.example.livegeoguessr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.livegeoguessr.ui.navigation.AppNavGraph
import com.example.livegeoguessr.ui.navigation.BottomBar
import com.example.livegeoguessr.ui.navigation.Screen
import com.example.livegeoguessr.ui.screens.settings.SettingsViewModel
import com.example.livegeoguessr.ui.theme.LiveGeoGuessrTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


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
                        val backgroundRes = if (darkMode) R.drawable.app_background_dark else R.drawable.app_background
                        Image(
                            painter = painterResource(id = backgroundRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f,
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f),
                                androidx.compose.ui.graphics.BlendMode.Darken
                            )
                        )
                        MainScreen(isLoggedIn!!, darkMode)
                        ConnectionStatus()
                    }
                }
            }
        }
    }
}
suspend fun isInternetReachable(): Boolean {
    return try {
        val url = URL("https://clients3.google.com/generate_204")
        val connection = withContext(Dispatchers.IO) {
            url.openConnection()
        } as HttpURLConnection
        connection.connectTimeout = 1500
        connection.readTimeout = 1500
        withContext(Dispatchers.IO) {
            connection.connect()
        }

        connection.responseCode == 204
    } catch (_: Exception) {
        false
    }
}
@Composable
fun rememberInternetState(): State<Boolean> {
    return produceState(initialValue = true) {
        while (true) {
            value = withContext(Dispatchers.IO) {
                isInternetReachable()
            }
            delay(10000)
        }
    }
}
@Composable
fun ConnectionStatus() {
    val isOnline by rememberInternetState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .zIndex(10f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = !isOnline,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "No Internet Connection",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
@Composable
fun MainScreen(isLoggedIn: Boolean, darkMode: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = if (darkMode) {
            MaterialTheme.colorScheme.background.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.background.copy(alpha = 0.4f)
        },
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
