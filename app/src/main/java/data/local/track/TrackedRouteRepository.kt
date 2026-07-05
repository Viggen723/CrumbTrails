package com.example.routetracker.data.local.track

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import data.local.track.TrackedRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class TrackedRouteRepository(private val dao: TrackedRouteDao) {

    val allRoutes: Flow<List<TrackedRoute>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun save(
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long,
        path: List<LatLng>,
        tripName: String
    ) {
        withContext(Dispatchers.IO) {
            dao.insert(
                TrackedRouteEntity(
                    id = UUID.randomUUID().toString(),
                    startedAtEpochMillis = startedAtEpochMillis,
                    endedAtEpochMillis = endedAtEpochMillis,
                    routeString = PolyUtil.encode(path),
                    tripName = tripName
                )
            )
        }
    }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            dao.delete(id)
        }
    }

    suspend fun addPhotoPaths(id: String, newPaths: List<String>) {
        if (newPaths.isEmpty()) return
        withContext(Dispatchers.IO) {
            val existingPaths = dao.getById(id)?.photoPaths.orEmpty()
            dao.updatePhotoPaths(id, existingPaths + newPaths)
        }
    }

    // Removes a single photo path from this route's persisted list, and best-effort
    // deletes the downsized copy from disk so removed photos don't stay in storage.
    suspend fun removePhotoPath(id: String, pathToRemove: String) {
        withContext(Dispatchers.IO) {
            val existingPaths = dao.getById(id)?.photoPaths.orEmpty()
            dao.updatePhotoPaths(id, existingPaths - pathToRemove)
            runCatching { File(pathToRemove).delete() } // https://proandroiddev.com/kotlin-tips-and-tricks-you-may-not-know-7-goodbye-try-catch-hello-trycatching-7135cb382609
        }
    }

    private fun TrackedRouteEntity.toDomain() = TrackedRoute(
        id = id,
        tripName = tripName,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        trackedRoute = if (routeString.isEmpty())
        {
            emptyList()
        }
        else
        {
            PolyUtil.decode(routeString)
        },
        photoPaths = photoPaths
    )
}