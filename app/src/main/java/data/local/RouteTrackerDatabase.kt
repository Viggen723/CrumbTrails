package com.example.routetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.routetracker.data.local.track.TrackedRouteEntity
import com.example.routetracker.data.local.track.TrackedRouteDao

@Database(
    entities = [
        TrackedRouteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class RouteTrackerDatabase : RoomDatabase() {

    abstract fun trackedRouteDao(): TrackedRouteDao

    companion object {
        @Volatile
        private var INSTANCE: RouteTrackerDatabase? = null

        fun getDatabase(context: Context): RouteTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RouteTrackerDatabase::class.java,
                    "routetracker_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}