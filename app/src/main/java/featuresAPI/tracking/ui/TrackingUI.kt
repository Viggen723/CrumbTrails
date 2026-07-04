package com.example.routetracker.featuresAPI.tracking.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routetracker.R
import com.example.routetracker.featuresAPI.tracking.viewModel.TrackingViewModel
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline

@Composable
fun TrackingUI(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = viewModel()
) {
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // The tracking service is required to show a persistent notification
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) viewModel.loadUserLocation()
    }

    // Could not get this to work even with internet research and AI help. Need assistance
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            viewModel.loadUserLocation()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // Got to make a separate variable to pass through the GoogleMaps object
        val uiSettings = remember {
            MapUiSettings(zoomControlsEnabled = false)
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            uiSettings = uiSettings,
            cameraPositionState = viewModel.cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
        ) {
            if (viewModel.pathPoints.size > 1) {
                Polyline(
                    points = viewModel.pathPoints,
                    color = colorResource(id = R.color.route_path),
                    width = 15f
                )
            }
        }

        // The title is modified here
        Text(
            modifier = Modifier
                .align(alignment = Alignment.TopCenter)
                .padding(16.dp),
            text = "Crumb Trails",
            style = MaterialTheme.typography.titleMedium,
        )

        TrackingButton(
            isTracking = viewModel.isTracking,
            onClick = {
                if (viewModel.isTracking)
                {
                    viewModel.stopTracking()
                }
                else
                {
                    viewModel.startTracking()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun TrackingButton(
    isTracking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp).size(width = 240.dp, height = 80.dp),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isTracking) {
                colorResource(id = R.color.track_recording)
            } else {
                colorResource(id = R.color.teal_700)
            }
        )
    ) {
        Text(
            text = if (isTracking)
            {
                "Stop tracking!"
            }
            else
            {
                "Start tracking!"
            },
            style = MaterialTheme.typography.titleMedium
        )
    }
}