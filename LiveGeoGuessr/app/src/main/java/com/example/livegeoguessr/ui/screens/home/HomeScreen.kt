package com.example.livegeoguessr.ui.screens.home

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            CircularProgressIndicator()
        }

        uiState.posts.isEmpty() -> {
            Text("No posts")
        }

        else -> {
            LazyColumn {
                items(uiState.posts) { post ->
                    Text(post.user)
                }
            }
        }
    }
}