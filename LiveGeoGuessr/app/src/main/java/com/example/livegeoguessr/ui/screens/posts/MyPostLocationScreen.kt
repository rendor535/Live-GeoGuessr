package com.example.livegeoguessr.ui.screens.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import com.example.livegeoguessr.ui.screens.guess.MapViewContainer
import com.example.livegeoguessr.ui.state.ScreenState
import org.osmdroid.util.GeoPoint

import androidx.compose.ui.res.stringResource
import com.example.livegeoguessr.R

@Composable
fun MyPostLocationScreen(
    postId: String,
    imageUrl: String,
    latitude: Double,
    longitude: Double,
    onPostDeleted: () -> Unit,
    viewModel: PostsViewModel
) {
    var isImageFullScreen by remember { mutableStateOf(false) }

    val postLocation = remember(latitude, longitude) {
        GeoPoint(latitude, longitude)
    }
    val uiState by viewModel.uiState.collectAsState()
    val isDeleting = (uiState as? ScreenState.Content)?.data?.isDeleting ?: false
    val deleteErrorMessage = (uiState as? ScreenState.Content)?.data?.deleteErrorMessage

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        val mapModifier = if (isImageFullScreen) {
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(150.dp)
                .zIndex(2f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { isImageFullScreen = false }
        } else {
            Modifier
                .fillMaxSize()
                .zIndex(1f)
        }

        MapViewContainer(
            guessedLocation = postLocation,
            targetLocation = null,
            showResult = false,
            initialCenter = postLocation,
            initialMapDiameterMeters = 1000.0,
            onLocationSelected = null,
            isInteractable = !isImageFullScreen,
            onClick = { if (isImageFullScreen) isImageFullScreen = false },
            modifier = mapModifier
        )

        val imageModifier = if (isImageFullScreen) {
            Modifier
                .fillMaxSize()
                .zIndex(1f)
                .background(Color.Black)
                .clickable { isImageFullScreen = false }
        } else {
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(150.dp)
                .zIndex(2f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { isImageFullScreen = true }
        }
        LaunchedEffect(imageUrl) {
            android.util.Log.d("MyPostLocationScreen", "imageUrl=$imageUrl")
        }
        AsyncImage(
            model = imageUrl.takeIf { it.isNotBlank() },
            contentDescription = stringResource(R.string.post_image_description),
            modifier = imageModifier,
            contentScale = if (isImageFullScreen) ContentScale.Fit else ContentScale.Crop,
            onError = {
                android.util.Log.e(
                    "MyPostLocationScreen",
                    "Image load failed: $imageUrl",
                    it.result.throwable
                )
            }
        )
        if (!isImageFullScreen) {
            Button(
                onClick = {
                    showDeleteDialog = true
                },
                enabled = !isDeleting,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 174.dp,
                        end = 16.dp
                    )
                    .width(150.dp)
                    .zIndex(3f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("DELETE")
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteDialog = false
                }
            },
            title = {
                Text("Delete post?")
            },
            text = {
                Text(
                    deleteErrorMessage
                        ?: "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost(postId) {
                            showDeleteDialog = false
                            onPostDeleted()
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("DELETE")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    },
                    enabled = !isDeleting
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}