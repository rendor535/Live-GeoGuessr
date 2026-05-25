package com.example.livegeoguessr.ui.screens.camera

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.livegeoguessr.R
import com.google.android.gms.maps.model.LatLng
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        CameraContent(viewModel = viewModel)
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                stringResource(R.string.camera_permission_rationale)
            } else {
                stringResource(R.string.camera_permission_required)
            }

            Text(textToShow, modifier = Modifier.padding(16.dp))

            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text(stringResource(R.string.request_permission))
            }
        }
    }
}

@Composable
private fun CameraContent(
    viewModel: CameraViewModel
) {
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPhotoConfirmed by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<LatLng?>(null) }

    val uploadState by viewModel.uploadState.collectAsState()

    LaunchedEffect(uploadState.isUploaded) {
        if (uploadState.isUploaded) {
            capturedBitmap = null
            isPhotoConfirmed = false
            location = null
            viewModel.resetUploadState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            capturedBitmap == null -> {
                CameraPreview(
                    onPhotoCaptured = { bitmap ->
                        capturedBitmap = bitmap
                        isPhotoConfirmed = false
                        location = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            !isPhotoConfirmed -> {
                PhotoPreview(
                    bitmap = capturedBitmap!!,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LocationPreview(
                    onLocationAcquired = { location = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (capturedBitmap != null) {
            FilledIconButton(
                onClick = {
                    capturedBitmap = null
                    isPhotoConfirmed = false
                    location = null
                    viewModel.resetUploadState()
                },
                enabled = !uploadState.isUploading,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel_retake),
                    modifier = Modifier.size(24.dp)
                )
            }

            FilledIconButton(
                onClick = {
                    if (!isPhotoConfirmed) {
                        isPhotoConfirmed = true
                    } else {
                        val bitmap = capturedBitmap
                        val currentLocation = location

                        if (bitmap != null && currentLocation != null) {
                            viewModel.uploadPost(
                                bitmap = bitmap,
                                location = currentLocation
                            )
                        }
                    }
                },
                enabled = !uploadState.isUploading && (!isPhotoConfirmed || location != null),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
                    .size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uploadState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPhotoConfirmed) {
                            Icons.AutoMirrored.Filled.Send
                        } else {
                            Icons.AutoMirrored.Filled.ArrowForward
                        },
                        contentDescription = if (isPhotoConfirmed) {
                            stringResource(R.string.post)
                        } else {
                            stringResource(R.string.confirm_photo)
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            uploadState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}