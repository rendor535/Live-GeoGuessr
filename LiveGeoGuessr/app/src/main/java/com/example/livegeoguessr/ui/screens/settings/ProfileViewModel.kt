package com.example.livegeoguessr.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.R
import com.example.livegeoguessr.data.repository.AuthRepository
import com.example.livegeoguessr.data.repository.SettingsRepository
import com.example.livegeoguessr.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val profileImageUrl: String? = null,
    val isLoading: Boolean = false,
    val errorResId: Int? = null,
    val isSuccess: Boolean = false,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uid = authRepository.currentUid()
            if (uid != null) {
                val profile = userRepository.getUserProfile(uid)
                _uiState.update { it.copy(
                    displayName = profile?.nickname ?: "",
                    profileImageUrl = profile?.photoUrl,
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorResId = R.string.user_not_found) }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        _uiState.update { it.copy(displayName = newName) }
    }

    fun updateProfilePicture(uri: Uri) {
        _uiState.update { it.copy(profileImageUrl = uri.toString()) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorResId = null) }
            // TODO: Implement actual update in UserRepository
            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            settingsRepository.setLoggedIn(false)
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: Implement account deletion
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
