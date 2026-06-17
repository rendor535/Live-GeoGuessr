package com.example.livegeoguessr.ui.screens.camera

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val title = stringResource(R.string.camera_permission_title)
                    val description = if (cameraPermissionState.status.shouldShowRationale) {
                        stringResource(R.string.camera_permission_rationale)
                    } else {
                        stringResource(R.string.camera_permission_required)
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.request_permission))
                    }
                }
            }
        }
    }
}

@Composable
fun CameraContent(
    viewModel: CameraViewModel,
    initialBitmap: Bitmap? = null,
    initialConfirmed: Boolean = false
) {
    var capturedBitmap by remember { mutableStateOf(initialBitmap) }
    var isPhotoConfirmed by remember { mutableStateOf(initialConfirmed) }
    var location by remember { mutableStateOf<LatLng?>(null) }

    val uploadState by viewModel.uploadState.collectAsState()

    BackHandler(enabled = capturedBitmap != null) {
        if (isPhotoConfirmed) {
            isPhotoConfirmed = false
            location = null
        } else {
            capturedBitmap = null
        }
    }

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

        // Close / Back Button (Always Visible)
        FilledIconButton(
            onClick = {
                if (capturedBitmap == null) {
                    // Navigate back or just reset? 
                    // Since it's a tab, maybe we don't need a back button in CameraPreview 
                    // unless it's a separate screen. 
                    // But if the user says "always visible", I'll provide a Close/Back action.
                    // For now, if capturedBitmap is null, let's assume it does nothing 
                    // or we could use it to reset.
                    // Actually, if we are in a tab, we probably don't want a Close button 
                    // that exits the app.
                } else {
                    capturedBitmap = null
                    isPhotoConfirmed = false
                    location = null
                    viewModel.resetUploadState()
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            enabled = capturedBitmap != null && !uploadState.isUploading
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel_retake),
                modifier = Modifier.size(28.dp)
            )
        }

        // Main Action Button (Next / Send)
        if (capturedBitmap != null) {
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
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(80.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uploadState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
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
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        uploadState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}