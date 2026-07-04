package com.example.routetracker.featuresAPI.history.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.routetracker.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import data.local.track.TrackedRoute
import featuresAPI.shared.ui.MapPhotoPin
import featuresAPI.shared.ui.PhotoThumbnailMarkers
import featuresAPI.shared.ui.fallbackRoutePosition

@Composable
fun SessionMapDialog(
    session: TrackedRoute,
    photoUris: List<Uri> = emptyList(),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val cameraPositionState = remember(session.id) {
                val bounds = LatLngBounds.builder().apply {
                    session.trackedRoute.forEach { include(it) }
                }.build()
                CameraPositionState(
                    position = CameraPosition.fromLatLngZoom(bounds.center, 15f)
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                if (session.trackedRoute.size > 1) {
                    Polyline(
                        points = session.trackedRoute,
                        color = colorResource(id = R.color.route_path),
                        width = 15f
                    )
                }

                val photoPins = photoUris.toHistoryPhotoPins(session.trackedRoute)
                PhotoThumbnailMarkers(pins = photoPins)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
    }
}

private fun List<Uri>.toHistoryPhotoPins(routePoints: List<com.google.android.gms.maps.model.LatLng>): List<MapPhotoPin> {
    return mapIndexedNotNull { index, uri ->
        fallbackRoutePosition(routePoints, index, size)?.let { position ->
            MapPhotoPin(
                imageSource = uri,
                position = position
            )
        }
    }
}
