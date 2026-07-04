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

    fun delete(id: String) {
        viewModelScope.launch {
            trackedRouteRepository.delete(id)
        }
    }

    fun shareRouteToFeed(route: TrackedRoute, caption: String, photoUris: List<Uri>) {
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
        }
    }

    private fun Throwable?.toSafeShareMessage(): String {
        val name = this?.javaClass?.simpleName ?: "unknown error"
        return "Upload failed ($name). Check Logcat for details."
    }
}
