package com.example.livegeoguessr.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.FriendRepository
import com.example.livegeoguessr.data.repository.FriendRequestData
import com.example.livegeoguessr.domain.model.UserProfile
import com.example.livegeoguessr.ui.state.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendUI(
    val id: String,
    val displayName: String,
    val profileImageUrl: String?,
    val isOnline: Boolean,
    val points: Int,
    val isLoading: Boolean = false
)

data class FriendRequestUI(
    val id: String,
    val userUid: String,
    val displayName: String,
    val nickname: String,
    val profileImageUrl: String?,
    val isProcessing: Boolean = false
)

data class FriendsData(
    val friends: List<FriendUI> = emptyList(),
    val incomingRequests: List<FriendRequestUI> = emptyList(),
    val outgoingRequests: List<FriendRequestUI> = emptyList()
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState<FriendsData>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<FriendsData>> = _uiState.asStateFlow()

    init {
        loadFriendsData()
    }

    fun loadFriendsData() {
        viewModelScope.launch {
            if (_uiState.value !is ScreenState.Content) {
                _uiState.value = ScreenState.Loading
            }

            try {
                val friends = friendRepository.getFriends()
                val incomingRequests = friendRepository.getIncomingRequests()
                val outgoingRequests = friendRepository.getOutgoingRequests()

                val data = FriendsData(
                    friends = friends.map { it.toFriendUI() },
                    incomingRequests = incomingRequests.map { it.toIncomingRequestUI() },
                    outgoingRequests = outgoingRequests.map { it.toOutgoingRequestUI() }
                )

                _uiState.value = if (data.friends.isEmpty() && 
                    data.incomingRequests.isEmpty() && 
                    data.outgoingRequests.isEmpty()) {
                    ScreenState.Empty()
                } else {
                    ScreenState.Content(data)
                }
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(
                    message = e.message ?: "Failed to load friends data"
                )
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        val currentState = _uiState.value
        val currentData = when (currentState) {
            is ScreenState.Content -> currentState.data
            is ScreenState.Empty -> FriendsData()
            else -> return
        }

        viewModelScope.launch {
            _uiState.value = ScreenState.Content(
                currentData.copy(
                    incomingRequests = currentData.incomingRequests.map {
                        if (it.id == requestId) it.copy(isProcessing = true) else it
                    }
                )
            )
            try {
                friendRepository.acceptFriendRequest(requestId)
                loadFriendsData()
            } catch (e: Exception) {
                android.util.Log.e("FriendsViewModel", "Failed to accept friend request", e)
                _uiState.value = ScreenState.Error(message = e.message ?: "Failed to accept request")
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        val currentState = _uiState.value
        val currentData = when (currentState) {
            is ScreenState.Content -> currentState.data
            is ScreenState.Empty -> FriendsData()
            else -> return
        }

        viewModelScope.launch {
            _uiState.value = ScreenState.Content(
                currentData.copy(
                    incomingRequests = currentData.incomingRequests.map {
                        if (it.id == requestId) it.copy(isProcessing = true) else it
                    }
                )
            )
            try {
                friendRepository.rejectFriendRequest(requestId)
                loadFriendsData()
            } catch (e: Exception) {
                android.util.Log.e("FriendsViewModel", "Failed to reject friend request", e)
                _uiState.value = ScreenState.Error(message = e.message ?: "Failed to reject request")
            }
        }
    }

    fun removeFriend(friendUid: String) {
        val currentState = _uiState.value
        val currentData = when (currentState) {
            is ScreenState.Content -> currentState.data
            is ScreenState.Empty -> FriendsData()
            else -> return
        }

        viewModelScope.launch {
            _uiState.value = ScreenState.Content(
                currentData.copy(
                    friends = currentData.friends.map {
                        if (it.id == friendUid) it.copy(isLoading = true) else it
                    }
                )
            )
            try {
                friendRepository.removeFriend(friendUid)
                loadFriendsData()
            } catch (e: Exception) {
                android.util.Log.e("FriendsViewModel", "Failed to remove friend", e)
                _uiState.value = ScreenState.Error(message = e.message ?: "Failed to remove friend")
            }
        }
    }

    private fun UserProfile.toFriendUI(): FriendUI {
        return FriendUI(
            id = uid,
            displayName = displayName.ifBlank { nickname },
            profileImageUrl = photoUrl,
            isOnline = false,
            points = stats.pointsTotal
        )
    }

    private fun FriendRequestData.toIncomingRequestUI(): FriendRequestUI {
        return FriendRequestUI(
            id = id,
            userUid = fromUid,
            displayName = fromDisplayName.ifBlank { fromNickname },
            nickname = fromNickname,
            profileImageUrl = fromPhotoUrl
        )
    }

    private fun FriendRequestData.toOutgoingRequestUI(): FriendRequestUI {
        return FriendRequestUI(
            id = id,
            userUid = toUid,
            displayName = toDisplayName.ifBlank { toNickname },
            nickname = toNickname,
            profileImageUrl = toPhotoUrl
        )
    }
}