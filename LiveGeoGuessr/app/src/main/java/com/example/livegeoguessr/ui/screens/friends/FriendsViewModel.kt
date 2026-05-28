package com.example.livegeoguessr.ui.screens.friends

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FriendUI(
    val id: String,
    val displayName: String,
    val profileImageUrl: String?,
    val isOnline: Boolean,
    val points: Int
)

data class FriendsUiState(
    val friends: List<FriendUI> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FriendsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        // Load mock data for frontend demonstration
        _uiState.value = FriendsUiState(
            friends = listOf(
                FriendUI("1", "Marcin", null, true, 1250),
                FriendUI("2", "Kamil", null, false, 850),
                FriendUI("3", "Ola", null, true, 2100),
                FriendUI("4", "Piotr", null, true, 1500),
                FriendUI("5", "Zosia", null, false, 320)
            )
        )
    }
}
