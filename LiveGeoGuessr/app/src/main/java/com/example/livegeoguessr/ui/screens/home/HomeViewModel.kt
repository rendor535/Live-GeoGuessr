package com.example.livegeoguessr.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.PostRepository
import com.example.livegeoguessr.domain.model.Post
import com.example.livegeoguessr.ui.state.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ScreenState<List<Post>>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<List<Post>>> = _uiState.asStateFlow()

    init {
        loadPosts()
    }
    fun loadPosts() {
        viewModelScope.launch {
            if (_uiState.value !is ScreenState.Content) {
                _uiState.value = ScreenState.Loading
            }

            try {
                val posts = postRepository.getPosts()

                _uiState.value = if (posts.isEmpty()) {
                    ScreenState.Empty()
                } else {
                    ScreenState.Content(posts)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                _uiState.value = ScreenState.Error(
                    message = e.message ?: "Failed to fetch posts"
                )
            }
        }
    }
}