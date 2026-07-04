package com.example.routetracker.featuresAPI.history.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog // Added
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routetracker.data.local.track.TrackedRoute
import com.example.routetracker.featuresAPI.history.viewModel.HistoryViewModel

@Composable
fun HistoryUI(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    var selectedSession by remember { mutableStateOf<TrackedRoute?>(null) }

    // Help from AI to make this deletion conformation
    var sessionPendingDeletion by remember { mutableStateOf<TrackedRoute?>(null) }

    var activePhotoSessionId by remember { mutableStateOf<Any?>(null) }
    val sessionPhotosMap = remember { mutableStateMapOf<Any, List<Uri>>() }

    if (sessions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No tracked routes yet... Start tracking to save one!",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Dialog when pressing the delete trash can
    sessionPendingDeletion?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionPendingDeletion = null },
            title = { Text(text = "Delete Trip") },
            text = { Text(text = "Are you sure you want to permanently delete \"${session.tripName}\"? This action cannot be undone.") },
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            val attachedPhotoCount = sessionPhotosMap[session.id]?.size ?: 0

            SessionCard(
                session = session,
                photoCount = attachedPhotoCount,
                onClick = { selectedSession = session },
                // 3. Changed: Stage the session for deletion instead of instantly calling the ViewModel
                onDelete = { sessionPendingDeletion = session },
                onTriggerEmbeddedPhoto = { activePhotoSessionId = session.id },
                onLegacyPhotoSelection = { uris ->
                    sessionPhotosMap[session.id] = (sessionPhotosMap[session.id] ?: emptyList()) + uris
                }
            )
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