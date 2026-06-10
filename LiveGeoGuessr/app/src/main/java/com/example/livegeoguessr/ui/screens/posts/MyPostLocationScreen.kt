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
import coil.compose.AsyncImage
import com.example.livegeoguessr.ui.screens.guess.MapViewContainer
import org.osmdroid.util.GeoPoint

import androidx.compose.ui.res.stringResource
import com.example.livegeoguessr.R

@Composable
fun MyPostLocationScreen(
    imageUrl: String,
    latitude: Double,
    longitude: Double
) {
    var isImageFullScreen by remember { mutableStateOf(false) }

    val postLocation = remember(latitude, longitude) {
        GeoPoint(latitude, longitude)
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

        AsyncImage(
            model = imageUrl,
            contentDescription = stringResource(R.string.post_image_description),
            modifier = imageModifier,
            contentScale = if (isImageFullScreen) ContentScale.Fit else ContentScale.Crop
        )
    }
}