package com.example.livegeoguessr.ui.screens.guessedposts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.livegeoguessr.ui.navigation.Screen
import com.example.livegeoguessr.ui.screens.home.PostItem

import androidx.compose.ui.res.stringResource
import com.example.livegeoguessr.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessedPostsScreen(
    navController: NavController,
    viewModel: GuessedPostsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.loadGuessedPosts() },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading && uiState.guessedPosts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.guessedPosts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )

                        Text(
                            text = stringResource(R.string.no_guessed_posts),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = stringResource(R.string.no_guessed_posts_description),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.guessedPosts) { guessedPost ->
                        val post = guessedPost.post

                        PostItem(
                            post = post,
                            distanceMeters = guessedPost.distanceMeters,
                            points = guessedPost.points,
                            onClick = {
                                navController.navigate(
                                    Screen.Guess.createRoute(
                                        postId = post.id,
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
}