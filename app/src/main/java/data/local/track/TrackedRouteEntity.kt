package com.example.routetracker.data.local.track

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_routes")
data class TrackedRouteEntity(
    @PrimaryKey
    val id: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val routeString: String // This is the encoded string of the path
)