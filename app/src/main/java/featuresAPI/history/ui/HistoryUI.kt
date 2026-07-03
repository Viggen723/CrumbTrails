package com.example.routetracker.featuresAPI.history.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.routetracker.data.local.track.TrackedRoute
import com.example.routetracker.featuresAPI.history.viewModel.HistoryViewModel

@Composable
fun HistoryUI(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    var selectedSession by remember { mutableStateOf<TrackedRoute?>(null) }

    var activePhotoSessionId by remember { mutableStateOf<Any?>(null) }
    val sessionPhotosMap = remember { mutableStateMapOf<Any, List<Uri>>() }

    if (sessions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No tracked routes yet - start tracking to save one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
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
                onDelete = { viewModel.delete(session.id) },
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