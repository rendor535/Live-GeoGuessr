package com.example.livegeoguessr.ui.screens.guessedposts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.PostRepository
import com.example.livegeoguessr.domain.model.GuessedPost
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuessedPostsUiState(
    val guessedPosts: List<GuessedPost> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GuessedPostsViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuessedPostsUiState())
    val uiState: StateFlow<GuessedPostsUiState> = _uiState.asStateFlow()

    init {
        loadGuessedPosts()
    }

    fun loadGuessedPosts() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            try {
                val posts = postRepository.getMyGuessedPosts()

                _uiState.update {
                    it.copy(
                        guessedPosts = posts,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Couldn't load guessed posts"
                    )
                }
            }
        }
    }
}