package com.example.routetracker.featuresAPI.history.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routetracker.featuresAPI.history.viewModel.HistoryViewModel
import com.example.routetracker.featuresAPI.history.viewModel.ShareStatus
import data.local.track.TrackedRoute

@Composable
fun HistoryUI(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val shareStatus by viewModel.shareStatus.collectAsState()
    var selectedSession by remember { mutableStateOf<TrackedRoute?>(null) }

    // keep the selected session around while the delete dialog is open
    var sessionPendingDeletion by remember { mutableStateOf<TrackedRoute?>(null) }
    var sessionPendingShare by remember { mutableStateOf<TrackedRoute?>(null) }
    var shareCaption by remember { mutableStateOf("") }

    var activePhotoSessionId by remember { mutableStateOf<Any?>(null) }
    val sessionPhotosMap = remember { mutableStateMapOf<Any, List<Uri>>() }

    if (sessions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                    text = "No tracked history...",
                    style = MaterialTheme.typography.titleMedium
                    )
                Text(
                    text = "Start tracking to save one!",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    // Dialog when pressing the delete trash can
    sessionPendingDeletion?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionPendingDeletion = null },
            title = { Text(text = "Delete Trip") },
            text = { Text(text = "Are you sure you want to permanently delete ${session.tripName}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(session.id) // Execute deletion here
                        sessionPendingDeletion = null // Reset state to close dialog
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDeletion = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    sessionPendingShare?.let { session ->
        AlertDialog(
            onDismissRequest = {
                sessionPendingShare = null
                shareCaption = ""
            },
            title = { Text(text = "Share Trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Add a caption for ${session.tripName}.")
                    OutlinedTextField(
                        value = shareCaption,
                        onValueChange = { shareCaption = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Caption") },
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedPhotoUris = sessionPhotosMap[session.id].orEmpty()
                        viewModel.shareRouteToFeed(session, shareCaption, selectedPhotoUris)
                        sessionPendingShare = null
                        shareCaption = ""
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sessionPendingShare = null
                        shareCaption = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        ShareStatusText(shareStatus = shareStatus)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                val attachedPhotoCount = sessionPhotosMap[session.id]?.size ?: 0

                SessionCard(
                    session = session,
                    photoCount = attachedPhotoCount,
                    onClick = { selectedSession = session },
                    onDelete = { sessionPendingDeletion = session },
                    onShare = {
                        sessionPendingShare = session
                        shareCaption = ""
                    },
                    onTriggerEmbeddedPhoto = { activePhotoSessionId = session.id },
                    onLegacyPhotoSelection = { uris ->
                        sessionPhotosMap[session.id] = (sessionPhotosMap[session.id] ?: emptyList()) + uris
                    }
                )
            }
        }
    }

    selectedSession?.let { session ->
        SessionMapDialog(session = session, onDismiss = { selectedSession = null })
    }

    if (activePhotoSessionId != null && isEmbeddedPhotoPickerSupported()) {
        EmbeddedPhotoPickerSheet(
            onUriGranted = { uris ->
                activePhotoSessionId?.let { id ->
                    sessionPhotosMap[id] = (sessionPhotosMap[id] ?: emptyList()) + uris
                }
            },
            onUriRevoked = { uris ->
                activePhotoSessionId?.let { id ->
                    sessionPhotosMap[id] = (sessionPhotosMap[id] ?: emptyList()) - uris
                }
            },
            onDismiss = { activePhotoSessionId = null }
        )
    }
}

@Composable
private fun ShareStatusText(shareStatus: ShareStatus) {
    val message = when (shareStatus) {
        ShareStatus.Idle -> null
        ShareStatus.Loading -> "Sharing..."
        ShareStatus.Success -> "Shared successfully."
        is ShareStatus.Error -> "Share failed: ${shareStatus.message}"
    }

    message?.let {
        Text(
            text = it,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = when (shareStatus) {
                is ShareStatus.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
