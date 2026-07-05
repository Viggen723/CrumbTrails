package com.example.routetracker.featuresAPI.history.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routetracker.data.local.RouteTrackerDatabase
import com.example.routetracker.data.local.track.TrackedRouteRepository
import com.google.firebase.auth.FirebaseAuth
import data.local.track.TrackedRoute
import featuresAPI.feed.data.FeedRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ShareStatus {
    data object Idle : ShareStatus
    data object Loading : ShareStatus
    data object Success : ShareStatus
    data class Error(val message: String) : ShareStatus
}

// https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
// developer.android.com/topic/architecture/ui-layer/state-production
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RouteTrackerDatabase.getDatabase(application)
    private val trackedRouteRepository = TrackedRouteRepository(database.trackedRouteDao())
    private val feedRepository = FeedRepository(contentResolver = application.contentResolver)

    val sessions: StateFlow<List<TrackedRoute>> = trackedRouteRepository.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _shareStatus = MutableStateFlow<ShareStatus>(ShareStatus.Idle)
    val shareStatus: StateFlow<ShareStatus> = _shareStatus.asStateFlow()

    private var shareStatusResetJob: Job? = null

    fun delete(id: String) {
        viewModelScope.launch {
            trackedRouteRepository.delete(id)
        }
    }

    // Downsizes each picked photo via Coil, saves the result to app-private
    // storage, then persists the saved paths onto the existing route in Room.
    fun attachPhotos(routeId: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val savedPaths = uris.mapNotNull { uri ->
                com.example.routetracker.utils.PhotoDownsizer.downsizeAndSave(
                    context = context,
                    sourceUri = uri,
                    routeId = routeId
                )
            }
            trackedRouteRepository.addPhotoPaths(routeId, savedPaths)
        }
    }

    // Removes a single photo (by its saved local file path) from a route
    fun removePhoto(routeId: String, photoPath: String) {
        viewModelScope.launch {
            trackedRouteRepository.removePhotoPath(routeId, photoPath)
        }
    }

    fun shareRouteToFeed(route: TrackedRoute, caption: String, photoUris: List<Uri>) {
        shareStatusResetJob?.cancel() // a new share is priority over any pending auto clear

        viewModelScope.launch {
            _shareStatus.value = ShareStatus.Loading
            val result = feedRepository.uploadSharedRoute(
                route = route,
                caption = caption,
                userId = FirebaseAuth.getInstance().currentUser?.uid,
                photoUris = photoUris
            )
            _shareStatus.value = if (result.isSuccess) {
                ShareStatus.Success
            } else {
                ShareStatus.Error(result.exceptionOrNull().toSafeShareMessage())
            }

            // hide the status message 3 seconds after it appears
            shareStatusResetJob = viewModelScope.launch {
                delay(3000)
                _shareStatus.value = ShareStatus.Idle
            }
        }
    }

    private fun Throwable?.toSafeShareMessage(): String {
        val name = this?.javaClass?.simpleName ?: "unknown error"
        return "Upload failed ($name). Check Logcat for details."
    }
}