package com.example.livegeoguessr.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel() ) {
    val darkMode by viewModel.darkMode.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement = Arrangement.Top
    ) {
        Text("Dark mode")
        Switch(
            checked = darkMode,
            onCheckedChange = { viewModel.updateDarkMode(it) }
        )
    }
}