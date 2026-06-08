package com.example.livegeoguessr.ui.screens.guess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun GuessScreen(
    postId: String,
    imageUrl: String,
    viewModel: GuessViewModel = hiltViewModel()
) {
    var isImageFullScreen by remember { mutableStateOf(false) }

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
        if (isImageFullScreen) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.full_screen_image),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { isImageFullScreen = false },
                contentScale = ContentScale.Fit
            )

            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(150.dp)
                    .clickable { isImageFullScreen = false },
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                MapViewContainer(
                    guessedLocation = guessedLocation,
                    modifier = Modifier.fillMaxSize(),
                    targetLocation = targetLocation,
                    showResult = guessUiState.result != null,
                    initialCenter = initialCenter,
                    initialMapDiameterMeters = guessUiState.initialMapDiameterMeters,
                    onLocationSelected = null
                )
            }
        } else {
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
                modifier = Modifier.fillMaxSize()
            )


            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(150.dp)
                    .clickable { isImageFullScreen = true },
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.preview_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (
                guessedLocation != null &&
                guessUiState.result == null
            ) {
                Button(
                    onClick = {
                        viewModel.submitGuess(postId)
                    },
                    enabled = !guessUiState.isSubmitting,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = if (guessUiState.isSubmitting) {
                            "Wysyłanie..."
                        } else {
                            stringResource(R.string.confirm_guess)
                        }
                    )
                }
            }

            guessUiState.result?.let { result ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val distance = if (useMiles) {
                            (result.distanceMeters / 1000) * 0.621371
                        } else {
                            result.distanceMeters / 1000
                        }

                        val unitResId = if (useMiles) {
                            R.string.distance_result_miles
                        } else {
                            R.string.distance_result
                        }

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
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black
                    )
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
    onLocationSelected: ((GeoPoint) -> Unit)? = null
) {
    val context = LocalContext.current
    val yourGuessTitle = stringResource(R.string.your_guess)
    val actualLocationTitle = stringResource(R.string.actual_location)
    val mapView = remember { MapView(context) }
    val guessMarker = remember { Marker(mapView).apply { title = yourGuessTitle } }
    val targetMarker = remember { Marker(mapView).apply { title = actualLocationTitle } }
    val connectionLine = remember { Polyline().apply { 
        outlinePaint.color = android.graphics.Color.RED
        outlinePaint.strokeWidth = 5f
    } }
    var hasZoomedToResult by remember(guessedLocation, targetLocation, showResult) { mutableStateOf(false) }
    var hasSetInitialCamera by remember(initialCenter, initialMapDiameterMeters) {
        mutableStateOf(false)
    }
    AndroidView(
        factory = {
            mapView.apply {
                setMultiTouchControls(true)

                controller.setZoom(5.0)
                controller.setCenter(GeoPoint(52.0, 19.0))

                val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        onLocationSelected?.invoke(p)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint): Boolean = false
                })
                overlays.add(eventsOverlay)
            }
        },
        modifier = modifier,
        update = { view ->
            guessMarker.title = yourGuessTitle
            targetMarker.title = actualLocationTitle

            if (
                !hasSetInitialCamera &&
                !showResult &&
                initialCenter != null &&
                initialMapDiameterMeters != null
            ) {
                val boundingBox = createBoundingBoxAround(
                    center = initialCenter,
                    diameterMeters = initialMapDiameterMeters
                )

                view.zoomToBoundingBox(boundingBox, true, 150)
                hasSetInitialCamera = true
            }


            if (guessedLocation != null) {
                guessMarker.position = guessedLocation
                if (!view.overlays.contains(guessMarker)) {
                    view.overlays.add(guessMarker)
                }
            }

            if (showResult && targetLocation != null && guessedLocation != null) {
                targetMarker.position = targetLocation
                if (!view.overlays.contains(targetMarker)) {
                    view.overlays.add(targetMarker)
                }

                connectionLine.setPoints(listOf(guessedLocation, targetLocation))
                if (!view.overlays.contains(connectionLine)) {
                    view.overlays.add(connectionLine)
                }
                
                // Zoom to show both points only once
                if (!hasZoomedToResult) {
                    val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(guessedLocation, targetLocation))
                    view.zoomToBoundingBox(boundingBox, true, 150)
                    hasZoomedToResult = true
                }
            }
            view.invalidate()
        }
    )
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