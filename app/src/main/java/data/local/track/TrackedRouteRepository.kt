package com.example.routetracker.data.local.track

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

// TODO Make the tracked route its own data class outside of this
/** A completed session, decoded back into the shape the UI actually wants. */
data class TrackedRoute(
    val id: String,
    val tripName: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val trackedRoute: List<LatLng>
)

class TrackedRouteRepository(private val dao: TrackedRouteDao) {

    val allRoutes: Flow<List<TrackedRoute>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun save(
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long,
        path: List<LatLng>
    ) {
        withContext(Dispatchers.IO) {
            dao.insert(
                TrackedRouteEntity(
                    id = UUID.randomUUID().toString(),
                    startedAtEpochMillis = startedAtEpochMillis,
                    endedAtEpochMillis = endedAtEpochMillis,
                    routeString = PolyUtil.encode(path)
                )
            )
        }
    }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            dao.delete(id)
        }
    }

    private fun TrackedRouteEntity.toDomain() = TrackedRoute(
        id = id,
        tripName = tripName,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        trackedRoute = if (routeString.isEmpty()) emptyList() else PolyUtil.decode(routeString)
    )
}