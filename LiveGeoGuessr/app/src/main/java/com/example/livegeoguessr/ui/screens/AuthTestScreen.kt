package com.example.livegeoguessr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.livegeoguessr.auth.AuthManager
import kotlinx.coroutines.launch

@Composable
fun AuthTestScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var text by remember {
        mutableStateOf("Niezalogowany")
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text)

        Button(
            onClick = {
                scope.launch {
                    try {

                        val user = AuthManager.loginWithGoogle(context)

                        text = if (user != null) {
                            "Zalogowano: ${user.nickname}"
                        } else {
                            "Brak profilu"
                        }

                    } catch (e: Exception) {
                        text = "Błąd: ${e.message}"
                    }
                }
            }
        ) {
            Text("Google Login")
        }

        Button(
            onClick = {
                AuthManager.logout()
                text = "Wylogowano"
            }
        ) {
            Text("Logout")
        }
    }
}