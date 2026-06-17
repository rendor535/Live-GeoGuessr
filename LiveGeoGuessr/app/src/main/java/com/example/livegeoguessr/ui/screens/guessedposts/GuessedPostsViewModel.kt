package com.example.livegeoguessr.ui.screens.guessedposts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.PostRepository
import com.example.livegeoguessr.domain.model.GuessedPost
import com.example.livegeoguessr.ui.state.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GuessedPostsViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<List<GuessedPost>>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<List<GuessedPost>>> = _uiState.asStateFlow()

    init {
        loadGuessedPosts()
    }

    fun loadGuessedPosts() {
        viewModelScope.launch {
            if (_uiState.value !is ScreenState.Content) {
                _uiState.value = ScreenState.Loading
            }

            try {
                val posts = postRepository.getMyGuessedPosts()

                _uiState.value = if (posts.isEmpty()) {
                    ScreenState.Empty()
                } else {
                    ScreenState.Content(posts)
                }
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(
                    message = e.message ?: "Couldn't load guessed posts"
                )
            }
        }
    }
}