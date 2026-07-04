package com.example.routetracker.featuresAPI.history.viewModel

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routetracker.data.local.RouteTrackerDatabase
import com.example.routetracker.data.local.track.TrackedRoute
import com.example.routetracker.data.local.track.TrackedRouteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
// developer.android.com/topic/architecture/ui-layer/state-production
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RouteTrackerDatabase.getDatabase(application)
    private val trackedRouteRepository = TrackedRouteRepository(database.trackedRouteDao())

    val sessions: StateFlow<List<TrackedRoute>> = trackedRouteRepository.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch {
            trackedRouteRepository.delete(id)
        }
    }
}
