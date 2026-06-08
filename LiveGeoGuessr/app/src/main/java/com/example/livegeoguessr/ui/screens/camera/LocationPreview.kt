package com.example.livegeoguessr.ui.screens.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.livegeoguessr.R
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
        Box(
            modifier = modifier.fillMaxSize(),
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
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val title = stringResource(R.string.location_permission_title)
                    val description = if (locationPermissionState.status.shouldShowRationale) {
                        stringResource(R.string.location_permission_rationale)
                    } else {
                        stringResource(R.string.location_permission_rationale) // Or use a shorter string if preferred
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { locationPermissionState.launchPermissionRequest() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
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
    
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var isGpsEnabled by remember { 
        mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
    }

    val checkGpsStatus = {
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    checkGpsStatus()
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {
        checkGpsStatus()
    }

    LaunchedEffect(isGpsEnabled) {
        if (isGpsEnabled) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    location = latLng
                    onLocationAcquired(latLng)
                } else {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { currentLoc ->
                            currentLoc?.let {
                                val latLng = LatLng(it.latitude, it.longitude)
                                location = latLng
                                onLocationAcquired(latLng)
                            }
                        }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!isGpsEnabled) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
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
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.gps_required_message),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.turn_on_gps))
                    }
                }
            }
        } else if (location == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Acquiring location...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
