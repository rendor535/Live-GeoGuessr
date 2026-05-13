package com.example.livegeoguessr.ui.screens.home

import com.example.livegeoguessr.domain.model.Post

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null


)