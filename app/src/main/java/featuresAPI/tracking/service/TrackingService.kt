package com.example.routetracker.featuresAPI.tracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.routetracker.MainActivity
import com.example.routetracker.data.local.RouteTrackerDatabase
import com.example.routetracker.data.local.track.TrackedRouteRepository
import com.example.routetracker.data.location.MapRepository
import com.google.android.gms.maps.model.LatLng
import featuresAPI.settings.data.TrackingPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
/**
 * Keeps recording the tracked path while the app is backgrounded (screen
 * off, another app in front). A ViewModel is only alive while its owning
 * Activity/Compose UI is around; a foreground service is not - it keeps
 * running (with a visible notification, as Android requires) independent
 * of whether anything is bound to it right now. TrackingViewModel binds to
 * this for live updates while the app is visible, and just leaves it
 * running when the app goes to the background.
 */
class TrackingService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // Not tied to the Activity/ViewModel lifecycle - a save triggered by
    // ACTION_STOP must finish even if nothing is bound at that moment.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val mapRepository by lazy { MapRepository(this) }
    private val trackedRouteRepository by lazy {
        TrackedRouteRepository(RouteTrackerDatabase.getDatabase(this).trackedRouteDao())
    }
    private val trackingPreferencesRepository by lazy {
        TrackingPreferencesRepository(this)
    }

    private val _pathPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val pathPoints: StateFlow<List<LatLng>> = _pathPoints.asStateFlow()

    private var trackingJob: Job? = null
    private var sessionStartedAtEpochMillis = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> {
                val tripName = intent.getStringExtra(EXTRA_TRIP_NAME) ?: "Untitled trip"
                stopTracking(tripName)
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (trackingJob != null) return // already tracking

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        _pathPoints.value = emptyList()
        sessionStartedAtEpochMillis = System.currentTimeMillis()

        trackingJob = serviceScope.launch {
            // Read once when tracking starts so mid-session changes do not mess with the route.
            val preferences = trackingPreferencesRepository.getPreferences()
            mapRepository.observeLocationUpdates(
                updateIntervalMillis = preferences.updateIntervalMillis,
                minUpdateDistanceMeters = preferences.minDistanceMeters
            ).collect { location ->
                _pathPoints.value = _pathPoints.value + location
            }
        }
    }

    private fun stopTracking(tripName: String) {
        trackingJob?.cancel()
        trackingJob = null

        val path = _pathPoints.value
        val startedAt = sessionStartedAtEpochMillis
        val endedAt = System.currentTimeMillis()

        if (path.isNotEmpty()) {
            serviceScope.launch {
                trackedRouteRepository.save(
                    startedAtEpochMillis = startedAt,
                    endedAtEpochMillis = endedAt,
                    path = path,
                    tripName = tripName
                )
            }
        }

        _pathPoints.value = emptyList()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RouteTracker")
            .setContentText("Recording your route")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Route tracking",
            NotificationManager.IMPORTANCE_LOW // Doesn't make a sound or anything
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.example.routetracker.action.START_TRACKING"
        const val ACTION_STOP = "com.example.routetracker.action.STOP_TRACKING"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "tracking_channel"

        // Intent key to get the string for the name of trip
        const val EXTRA_TRIP_NAME = "com.example.routetracker.extra.TRIP_NAME"
    }
}
