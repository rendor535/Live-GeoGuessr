package com.example.livegeoguessr.ui.screens.guess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.GuessRepository
import com.example.livegeoguessr.data.repository.SettingsRepository
import com.example.livegeoguessr.domain.model.GameModeType
import com.example.livegeoguessr.domain.model.SubmitGuessResult
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GuessUiState(
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val initialMapCenterLatitude: Double? = null,
    val initialMapCenterLongitude: Double? = null,
    val initialMapDiameterMeters: Double? = null,
    val isSubmitting: Boolean = false,
    val result: SubmitGuessResult? = null,
    val errorMessage: String? = null
) {
    val hasAlreadyGuessed: Boolean
        get() = result != null
}

@HiltViewModel
class GuessViewModel @Inject constructor(
    repository: SettingsRepository
) : ViewModel() {

    private val guessRepository = GuessRepository()

    val useMiles = repository.useMilesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _guessUiState = MutableStateFlow(GuessUiState())

    val guessUiState: StateFlow<GuessUiState> =
        _guessUiState.asStateFlow()

    fun loadGuessStatus(postId: String) {
        viewModelScope.launch {
            try {
                val mapPreview =
                    guessRepository.getGuessMapPreview(
                        postId = postId,
                        gameMode = GameModeType.CITY
                    )

                val existingGuess =
                    guessRepository.getMyGuessForPost(postId)

                _guessUiState.value =
                    _guessUiState.value.copy(
                        initialMapCenterLatitude = mapPreview.initialMapCenterLatitude,
                        initialMapCenterLongitude = mapPreview.initialMapCenterLongitude,
                        initialMapDiameterMeters = mapPreview.initialMapDiameterMeters,
                        result = existingGuess,
                        errorMessage = null
                    )

            } catch (e: Exception) {
                _guessUiState.value =
                    _guessUiState.value.copy(
                        errorMessage = e.message
                    )
            }
        }
    }
    fun selectLocation(
        latitude: Double,
        longitude: Double
    ) {
        val state = _guessUiState.value

        if (state.hasAlreadyGuessed || state.isSubmitting) {
            return
        }

        _guessUiState.value =
            state.copy(
                selectedLatitude = latitude,
                selectedLongitude = longitude,
                errorMessage = null
            )
    }

    fun submitGuess(postId: String) {
        val state = _guessUiState.value

        if (state.hasAlreadyGuessed) {
            _guessUiState.value =
                state.copy(
                    errorMessage = "Ten post został już zgadnięty."
                )
            return
        }

        val latitude = state.selectedLatitude
        val longitude = state.selectedLongitude

        if (latitude == null || longitude == null) {
            _guessUiState.value =
                state.copy(
                    errorMessage = "Najpierw wybierz miejsce na mapie."
                )
            return
        }

        viewModelScope.launch {

            _guessUiState.value =
                state.copy(
                    isSubmitting = true,
                    errorMessage = null
                )

            try {

                val result =
                    guessRepository.submitGuess(
                        postId = postId,
                        guessedLatitude = latitude,
                        guessedLongitude = longitude,
                        gameMode = GameModeType.CITY
                    )

                _guessUiState.value =
                    _guessUiState.value.copy(
                        isSubmitting = false,
                        result = result,
                        errorMessage = null
                    )

            } catch (e: FirebaseFunctionsException) {

                val message =
                    when (e.code) {

                        FirebaseFunctionsException.Code.ALREADY_EXISTS ->
                            "Ten post został już zgadnięty."

                        FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                            "Musisz być zalogowany."

                        FirebaseFunctionsException.Code.NOT_FOUND ->
                            "Nie znaleziono posta."

                        else ->
                            e.message
                                ?: "Nie udało się wysłać zgadnięcia."
                    }

                _guessUiState.value =
                    _guessUiState.value.copy(
                        isSubmitting = false,
                        errorMessage = message
                    )

            } catch (e: Exception) {

                _guessUiState.value =
                    _guessUiState.value.copy(
                        isSubmitting = false,
                        errorMessage = e.message
                            ?: "Nieznany błąd."
                    )
            }
        }
    }
}