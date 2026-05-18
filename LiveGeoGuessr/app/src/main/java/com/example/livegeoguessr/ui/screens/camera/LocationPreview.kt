package com.example.livegeoguessr.ui.screens.camera

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.livegeoguessr.R
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPreview(
    onLocationAcquired: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    if (locationPermissionState.status.isGranted) {
        LocationMapContent(onLocationAcquired, modifier)
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.location_permission_rationale))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun LocationMapContent(
    onLocationAcquired: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            loc?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                location = latLng
                onLocationAcquired(latLng)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (location == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val geoPoint = remember(location) { GeoPoint(location!!.latitude, location!!.longitude) }
            
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(geoPoint)
                        
                        val marker = Marker(this)
                        marker.position = geoPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = ctx.getString(R.string.your_location)
                        overlays.add(marker)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.controller.setCenter(geoPoint)
                }
            )
        }
    }
}
