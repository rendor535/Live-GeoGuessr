package com.example.livegeoguessr.ui.screens.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.livegeoguessr.ui.components.PostItem
import com.example.livegeoguessr.ui.navigation.Screen
import androidx.compose.ui.res.stringResource
import com.example.livegeoguessr.R

@Composable
fun PostsScreen(
    navController: NavController,
    viewModel: PostsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.errorMessage != null -> {
            Text(
                text = uiState.errorMessage ?: stringResource(R.string.unknown_error),
                modifier = Modifier.padding(16.dp)
            )
        }

        uiState.posts.isEmpty() -> {
            Text(
                text = stringResource(R.string.no_posts),
                modifier = Modifier.padding(16.dp)
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.posts) { post ->
                    PostItem(
                        post = post,
                        onClick = {
                            navController.navigate(
                                Screen.MyPostLocation.createRoute(
                                    imageUrl = post.imageUrl,
                                    lat = post.latitude,
                                    lon = post.longitude
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}