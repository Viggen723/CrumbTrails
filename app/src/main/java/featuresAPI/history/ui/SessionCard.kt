package com.example.routetracker.featuresAPI.history.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.local.track.TrackedRoute
import java.text.DateFormat
import java.util.Date

@Composable
fun SessionCard(
    session: TrackedRoute,
    photoCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onTriggerEmbeddedPhoto: () -> Unit,
    onLegacyPhotoSelection: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) onLegacyPhotoSelection(uris)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.tripName
                )

                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(session.startedAtEpochMillis)),
                    style = MaterialTheme.typography.titleSmall
                )

                val trailingLabel = if (photoCount > 0) " • $photoCount photos" else ""
                Text(
                    text = "${session.trackedRoute.size} points recorded$trailingLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShare

                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share to feed",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        if (isEmbeddedPhotoPickerSupported()) {
                            onTriggerEmbeddedPhoto()
                        } else {
                            legacyPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = "Attach photos to session",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete session",
                        tint = MaterialTheme.colorScheme.error // Red
                    )
                }
            }
        }
    }
}
