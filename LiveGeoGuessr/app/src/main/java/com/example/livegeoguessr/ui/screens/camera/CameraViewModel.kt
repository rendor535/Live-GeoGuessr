package com.example.livegeoguessr.ui.screens.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livegeoguessr.data.repository.PostRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUploadState(
    val isUploading: Boolean = false,
    val isUploaded: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uploadState = MutableStateFlow(CameraUploadState())
    val uploadState: StateFlow<CameraUploadState> = _uploadState.asStateFlow()

    fun uploadPost(
        bitmap: Bitmap,
        location: LatLng
    ) {
        viewModelScope.launch {
            _uploadState.value = CameraUploadState(isUploading = true)

            try {
                postRepository.addPost(
                    bitmap = bitmap,
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                _uploadState.value = CameraUploadState(isUploaded = true)
            } catch (e: Exception) {
                _uploadState.value = CameraUploadState(
                    isUploading = false,
                    errorMessage = e.message ?: "Upload failed"
                )
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = CameraUploadState()
    }
}