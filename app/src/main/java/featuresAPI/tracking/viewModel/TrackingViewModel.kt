package com.example.routetracker.featuresAPI.tracking.viewModel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routetracker.data.location.MapRepository
import com.example.routetracker.featuresAPI.tracking.service.TrackingService
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val mapRepository = MapRepository(application)

    val cameraPositionState = CameraPositionState(
        position = CameraPosition.fromLatLngZoom(LatLng(35.6812, 139.7671), 15f)
    )

    var isTracking by mutableStateOf(false)
        private set

    // The path drawn on the map, kept live from TrackingService's own
    // StateFlow while we're bound to it (see connection below).
    val pathPoints = mutableStateListOf<LatLng>()

    private var trackingService: TrackingService? = null
    private var isBound = false
    private var collectJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as TrackingService.LocalBinder).getService()
            trackingService = service
            isBound = true

            collectJob = viewModelScope.launch {
                service.pathPoints.collect { points ->
                    pathPoints.clear()
                    pathPoints.addAll(points)
                    points.lastOrNull()?.let { last ->
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(last, cameraPositionState.position.zoom)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            trackingService = null
            collectJob?.cancel()
        }
    }

    init {
        // Bind (without starting) so get live updates whenever the
        // service happens to be running like when reopening the app mid-walk.
        val intent = Intent(application, TrackingService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /** Centers the map on the user before a session starts. */
    fun loadUserLocation() {
        viewModelScope.launch {
            val latLng = mapRepository.getCurrentLocation()
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
        }
    }

    fun startTracking() {
        if (isTracking) return
        isTracking = true
        pathPoints.clear()

        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)

        pathPoints.clear()
    }

    override fun onCleared() {
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
        collectJob?.cancel()
        super.onCleared()
    }
}