package com.example.routetracker.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import featuresAPI.settings.data.TrackingPreferencesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// This class is mainly just for getting the current location of the user
class MapRepository(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // Tells to ignore the missing permission to set default
    suspend fun getCurrentLocation(): LatLng = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume(LatLng(location.latitude, location.longitude))
            } else {
                // Default to Tokyo station if location is null
                continuation.resume(LatLng(35.6812, 139.7671))
            }
        }.addOnFailureListener {
            continuation.resume(LatLng(35.6812, 139.7671))
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(
        updateIntervalMillis: Long = TrackingPreferencesRepository.DEFAULT_UPDATE_INTERVAL_MILLIS,
        minUpdateDistanceMeters: Float = TrackingPreferencesRepository.DEFAULT_MIN_DISTANCE_METERS
    ): Flow<LatLng> = callbackFlow {
        // These come from Settings now; Android still wants milliseconds and meters here.
        val safeUpdateIntervalMillis = updateIntervalMillis.coerceAtLeast(1_000L)
        val safeMinUpdateDistanceMeters = minUpdateDistanceMeters.coerceAtLeast(0f)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, safeUpdateIntervalMillis)
            // Use the selected interval for both values so 30s really means "not faster than 30s".
            .setMinUpdateIntervalMillis(safeUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(safeMinUpdateDistanceMeters)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(LatLng(location.latitude, location.longitude))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
