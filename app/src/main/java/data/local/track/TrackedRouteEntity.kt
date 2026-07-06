package com.example.routetracker.data.local.track

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_routes")
data class TrackedRouteEntity(
    @PrimaryKey
    val id: String,
    val tripName: String = "Untitled trip",
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val routeString: String, // This is the encoded string of the path
    val photoPaths: List<String> = emptyList() // Empty as it is only set when shared
)