package com.example.livegeoguessr.ui.screens.guess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.example.livegeoguessr.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.compose.material3.MaterialTheme

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon

@Composable
fun GuessScreen(
    postId: String,
    imageUrl: String,
    viewModel: GuessViewModel = hiltViewModel()
) {
    var isImageFullScreen by remember { mutableStateOf(true) }

    val useMiles by viewModel.useMiles.collectAsState()
    val guessUiState by viewModel.guessUiState.collectAsState()

    val guessedLocation = remember(
        guessUiState.selectedLatitude,
        guessUiState.selectedLongitude
    ) {
        val latitude = guessUiState.selectedLatitude
        val longitude = guessUiState.selectedLongitude

        if (latitude != null && longitude != null) {
            GeoPoint(latitude, longitude)
        } else {
            null
        }
    }

    val targetLocation = remember(guessUiState.result) {
        guessUiState.result?.let { result ->
            GeoPoint(result.realLatitude, result.realLongitude)
        }
    }

    val initialCenter = remember(
        guessUiState.initialMapCenterLatitude,
        guessUiState.initialMapCenterLongitude
    ) {
        val latitude = guessUiState.initialMapCenterLatitude
        val longitude = guessUiState.initialMapCenterLongitude

        if (latitude != null && longitude != null) {
            GeoPoint(latitude, longitude)
        } else {
            null
        }
    }

    LaunchedEffect(postId) {
        viewModel.loadGuessStatus(postId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map Component
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
            guessedLocation = guessedLocation,
            targetLocation = targetLocation,
            showResult = guessUiState.result != null,
            initialCenter = initialCenter,
            initialMapDiameterMeters = guessUiState.initialMapDiameterMeters,
            onLocationSelected = {
                viewModel.selectLocation(
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            },
            isInteractable = !isImageFullScreen,
            onClick = { if (isImageFullScreen) isImageFullScreen = false },
            modifier = mapModifier
        )

        // Image Component
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
            contentDescription = if (isImageFullScreen) stringResource(R.string.full_screen_image) else stringResource(R.string.preview_image),
            modifier = imageModifier,
            contentScale = if (isImageFullScreen) ContentScale.Fit else ContentScale.Crop
        )

        // UI Overlays (only visible when map is main view or showing result)
        if (!isImageFullScreen) {
            if (guessedLocation != null && guessUiState.result == null) {
                Button(
                    onClick = { viewModel.submitGuess(postId) },
                    enabled = !guessUiState.isSubmitting,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .height(40.dp)
                        .zIndex(3f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (guessUiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (guessUiState.isSubmitting) stringResource(R.string.submitting) else stringResource(R.string.confirm_guess),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            guessUiState.result?.let { result ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .zIndex(3f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val distance = if (useMiles) (result.distanceMeters / 1000) * 0.621371 else result.distanceMeters / 1000
                        val unitResId = if (useMiles) R.string.distance_result_miles else R.string.distance_result

                        Text(
                            text = stringResource(R.string.scored_points, result.points),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(unitResId, distance),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                    }
                }
            }

            guessUiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .zIndex(3f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Text(text = error, modifier = Modifier.padding(16.dp), color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun MapViewContainer(
    guessedLocation: GeoPoint?,
    modifier: Modifier = Modifier,
    targetLocation: GeoPoint? = null,
    showResult: Boolean = false,
    initialCenter: GeoPoint? = null,
    initialMapDiameterMeters: Double? = null,
    onLocationSelected: ((GeoPoint) -> Unit)? = null,
    isInteractable: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val yourGuessTitle = stringResource(R.string.your_guess)
    val actualLocationTitle = stringResource(R.string.actual_location)
    
    val mapView = remember { MapView(context) }
    
    val guessMarker = remember(mapView) { Marker(mapView) }
    val targetMarker = remember(mapView) { Marker(mapView) }
    val connectionLine = remember(mapView) { Polyline().apply { 
        outlinePaint.color = android.graphics.Color.RED
        outlinePaint.strokeWidth = 5f
    } }

    val updatedOnLocationSelected by rememberUpdatedState(onLocationSelected)

    var hasSetInitialCamera by remember(initialCenter, initialMapDiameterMeters) { mutableStateOf(false) }
    var hasZoomedToResult by remember(guessedLocation, targetLocation, showResult) { mutableStateOf(false) }

    LaunchedEffect(mapView, isInteractable) {
        mapView.setMultiTouchControls(isInteractable)
    }

    LaunchedEffect(mapView, initialCenter, initialMapDiameterMeters, hasSetInitialCamera, showResult) {
        if (!hasSetInitialCamera && !showResult && initialCenter != null && initialMapDiameterMeters != null) {
            val boundingBox = createBoundingBoxAround(initialCenter, initialMapDiameterMeters)
            mapView.zoomToBoundingBox(boundingBox, true, 150)
            hasSetInitialCamera = true
        }
    }

    LaunchedEffect(mapView, guessedLocation, targetLocation, showResult, hasZoomedToResult) {
        if (showResult && targetLocation != null && guessedLocation != null && !hasZoomedToResult) {
            val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(guessedLocation, targetLocation))
            mapView.zoomToBoundingBox(boundingBox, true, 150)
            hasZoomedToResult = true
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    controller.setZoom(5.0)
                    controller.setCenter(GeoPoint(52.0, 19.0))

                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            if (isInteractable) {
                                updatedOnLocationSelected?.invoke(p)
                            }
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    overlays.add(eventsOverlay)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                guessMarker.title = yourGuessTitle
                targetMarker.title = actualLocationTitle

                if (guessedLocation != null) {
                    guessMarker.position = guessedLocation
                    if (!view.overlays.contains(guessMarker)) view.overlays.add(guessMarker)
                }

                if (showResult && targetLocation != null && guessedLocation != null) {
                    targetMarker.position = targetLocation
                    if (!view.overlays.contains(targetMarker)) view.overlays.add(targetMarker)

                    connectionLine.setPoints(listOf(guessedLocation, targetLocation))
                    if (!view.overlays.contains(connectionLine)) view.overlays.add(connectionLine)
                }
                view.invalidate()
            }
        )

        // Overlay to block interaction and handle clicks when map is small
        if (!isInteractable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(onClick = { onClick?.invoke() })
            )
        }
    }
}


private fun createBoundingBoxAround(
    center: GeoPoint,
    diameterMeters: Double
): org.osmdroid.util.BoundingBox {
    val earthRadiusMeters = 6_371_000.0
    val radiusMeters = diameterMeters / 2.0

    val latitudeDelta =
        radiusMeters / earthRadiusMeters * 180.0 / Math.PI

    val longitudeDelta =
        radiusMeters /
                (earthRadiusMeters * kotlin.math.cos(Math.toRadians(center.latitude))) *
                180.0 /
                Math.PI

    val north = center.latitude + latitudeDelta
    val south = center.latitude - latitudeDelta
    val east = center.longitude + longitudeDelta
    val west = center.longitude - longitudeDelta

    return org.osmdroid.util.BoundingBox(
        north,
        east,
        south,
        west
    )
}