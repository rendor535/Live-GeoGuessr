package com.example.livegeoguessr.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.FriendRepository
import com.example.livegeoguessr.data.repository.FriendRequestData
import com.example.livegeoguessr.domain.model.UserProfile
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
    val points: Int
)

data class FriendRequestUI(
    val id: String,
    val userUid: String,
    val displayName: String,
    val nickname: String,
    val profileImageUrl: String?
)

data class FriendsUiState(
    val friends: List<FriendUI> = emptyList(),
    val incomingRequests: List<FriendRequestUI> = emptyList(),
    val outgoingRequests: List<FriendRequestUI> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        loadFriendsData()
    }

    fun loadFriendsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val friends = friendRepository.getFriends()
                val incomingRequests = friendRepository.getIncomingRequests()
                val outgoingRequests = friendRepository.getOutgoingRequests()

                _uiState.value = _uiState.value.copy(
                    friends = friends.map { user -> user.toFriendUI() },
                    incomingRequests = incomingRequests.map { request ->
                        request.toIncomingRequestUI()
                    },
                    outgoingRequests = outgoingRequests.map { request ->
                        request.toOutgoingRequestUI()
                    },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Nie udało się pobrać znajomych"
                )
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                friendRepository.acceptFriendRequest(requestId)
                loadFriendsData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Nie udało się zaakceptować zaproszenia"
                )
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                friendRepository.rejectFriendRequest(requestId)
                loadFriendsData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Nie udało się odrzucić zaproszenia"
                )
            }
        }
    }

    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            try {
                friendRepository.removeFriend(friendUid)
                loadFriendsData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Nie udało się usunąć znajomego"
                )
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