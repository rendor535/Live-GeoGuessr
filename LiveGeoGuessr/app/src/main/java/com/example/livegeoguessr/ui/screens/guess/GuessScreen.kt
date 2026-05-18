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
import kotlin.math.*

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun GuessScreen(
    imageUrl: String,
    targetLat: Double,
    targetLon: Double,
    viewModel: GuessViewModel = hiltViewModel()
) {
    var isImageFullScreen by remember { mutableStateOf(false) }
    var guessedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var resultDistance by remember { mutableStateOf<Double?>(null) }
    val useMiles by viewModel.useMiles.collectAsState()
    val targetLocation = remember(targetLat, targetLon) { GeoPoint(targetLat, targetLon) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isImageFullScreen) {
            // Full Screen Image
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.full_screen_image),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { isImageFullScreen = false },
                contentScale = ContentScale.Fit
            )
            
            // Preview Map in Top Right
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
                    showResult = resultDistance != null,
                    onLocationSelected = null
                )
            }
        } else {
            // Full Screen Map
            MapViewContainer(
                guessedLocation = guessedLocation,
                targetLocation = targetLocation,
                showResult = resultDistance != null,
                onLocationSelected = { if (resultDistance == null) guessedLocation = it },
                modifier = Modifier.fillMaxSize()
            )
            
            // Preview Image in Top Right
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

            // Guess Button
            if (guessedLocation != null && resultDistance == null) {
                Button(
                    onClick = {
                        resultDistance = calculateDistance(
                            guessedLocation!!.latitude, guessedLocation!!.longitude,
                            targetLat, targetLon
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Text(stringResource(R.string.confirm_guess))
                }
            }

            // Result Info
            if (resultDistance != null) {
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
                            (resultDistance!! / 1000) * 0.621371
                        } else {
                            resultDistance!! / 1000
                        }
                        
                        val unitResId = if (useMiles) {
                            R.string.distance_result_miles
                        } else {
                            R.string.distance_result
                        }

                        Text(
                            text = stringResource(unitResId, distance),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371e3 // Earth radius in meters
    val p1 = lat1 * PI / 180
    val p2 = lat2 * PI / 180
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(p1) * cos(p2) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return r * c
}

@Composable
fun MapViewContainer(
    guessedLocation: GeoPoint?,
    modifier: Modifier = Modifier,
    targetLocation: GeoPoint? = null,
    showResult: Boolean = false,
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
    var hasZoomedToResult by remember { mutableStateOf(false) }

    AndroidView(
        factory = {
            mapView.apply {
                setMultiTouchControls(true)
                controller.setZoom(5.0)
                controller.setCenter(GeoPoint(52.2297, 21.0122)) // Default center
                
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
