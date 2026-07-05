package data.local.track

import com.google.android.gms.maps.model.LatLng

data class TrackedRoute(
    val id: String,
    val tripName: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val trackedRoute: List<LatLng>,
    val photoPaths: List<String> = emptyList()
)