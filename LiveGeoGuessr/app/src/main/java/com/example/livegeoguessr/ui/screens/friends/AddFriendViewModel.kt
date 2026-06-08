package com.example.livegeoguessr.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.FriendRepository
import com.example.livegeoguessr.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddFriendUserUI(
    val uid: String,
    val displayName: String,
    val nickname: String,
    val profileImageUrl: String?,
    val points: Int,
    val requestSent: Boolean = false
)

data class AddFriendUiState(
    val query: String = "",
    val users: List<AddFriendUserUI> = emptyList(),
    val filteredUsers: List<AddFriendUserUI> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val users = friendRepository.getUsersForFriendSearch()
                    .map { user -> user.toAddFriendUserUI() }

                _uiState.update {
                    it.copy(
                        users = users,
                        filteredUsers = filterUsers(users, it.query),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Nie udało się pobrać użytkowników"
                    )
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                filteredUsers = filterUsers(it.users, query)
            )
        }
    }

    fun sendFriendRequest(userUid: String) {
        viewModelScope.launch {
            try {
                friendRepository.sendFriendRequest(userUid)

                _uiState.update { state ->
                    val updatedUsers = state.users.map { user ->
                        if (user.uid == userUid) {
                            user.copy(requestSent = true)
                        } else {
                            user
                        }
                    }

                    state.copy(
                        users = updatedUsers,
                        filteredUsers = filterUsers(updatedUsers, state.query),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Nie udało się wysłać zaproszenia"
                    )
                }
            }
        }
    }

    private fun filterUsers(
        users: List<AddFriendUserUI>,
        query: String
    ): List<AddFriendUserUI> {
        val normalizedQuery = query.trim().lowercase()

        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        return users.filter { user ->
            user.nickname.lowercase().contains(normalizedQuery) ||
                    user.displayName.lowercase().contains(normalizedQuery)
        }
    }

    private fun UserProfile.toAddFriendUserUI(): AddFriendUserUI {
        return AddFriendUserUI(
            uid = uid,
            displayName = displayName.ifBlank { nickname },
            nickname = nickname,
            profileImageUrl = photoUrl,
            points = stats.pointsTotal
        )
    }
}