package com.example.livegeoguessr.ui.screens.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.PostRepository
import com.example.livegeoguessr.domain.model.Post
import com.example.livegeoguessr.ui.state.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostsData(
    val posts: List<Post> = emptyList(),
    val isDeleting: Boolean = false,
    val deleteErrorMessage: String? = null
)

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<PostsData>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<PostsData>> = _uiState.asStateFlow()

    init {
        loadMyPosts()
    }

    fun loadMyPosts() {
        viewModelScope.launch {
            if (_uiState.value !is ScreenState.Content) {
                _uiState.value = ScreenState.Loading
            }

            try {
                val posts = postRepository.getMyPosts()

                _uiState.value = if (posts.isEmpty()) {
                    ScreenState.Empty()
                } else {
                    ScreenState.Content(PostsData(posts = posts))
                }
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(
                    message = e.message ?: "Failed to fetch posts"
                )
            }
        }
    }

    fun deletePost(
        postId: String,
        onDeleted: () -> Unit
    ) {
        val currentState = _uiState.value
        if (currentState !is ScreenState.Content) return
        if (currentState.data.isDeleting) return

        viewModelScope.launch {
            _uiState.value = ScreenState.Content(
                currentState.data.copy(
                    isDeleting = true,
                    deleteErrorMessage = null
                )
            )

            try {
                postRepository.deleteMyPost(postId)

                val updatedPosts = currentState.data.posts.filterNot { it.id == postId }
                _uiState.value = if (updatedPosts.isEmpty()) {
                    ScreenState.Empty()
                } else {
                    ScreenState.Content(PostsData(posts = updatedPosts))
                }

                onDeleted()
            } catch (e: Exception) {
                android.util.Log.e(
                    "PostsViewModel",
                    "Failed to delete post: $postId",
                    e
                )
                _uiState.value = ScreenState.Content(
                    currentState.data.copy(
                        isDeleting = false,
                        deleteErrorMessage = e.message ?: "Failed to delete post"
                    )
                )
            }
        }
    }
}