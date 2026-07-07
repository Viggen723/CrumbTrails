package com.example.routetracker.data.local.track

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedRouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: TrackedRouteEntity)

    // Will get the entities from recent
    @Query("SELECT * FROM tracked_routes ORDER BY startedAtEpochMillis DESC")
    fun getAll(): Flow<List<TrackedRouteEntity>>

    // This function will be used for transferring the routes to the Firebase server (I think it will)
    @Query("SELECT * FROM tracked_routes WHERE id = :id")
    suspend fun getById(id: String): TrackedRouteEntity?

    @Query("DELETE FROM tracked_routes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE tracked_routes SET photoPaths = :photoPaths WHERE id = :id")
    suspend fun updatePhotoPaths(id: String, photoPaths: List<String>)
}