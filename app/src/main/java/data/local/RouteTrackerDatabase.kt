package com.example.routetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.routetracker.data.local.track.TrackedRouteEntity
import com.example.routetracker.data.local.track.TrackedRouteDao
import com.example.routetracker.utils.Converters

@Database(
    entities = [
        TrackedRouteEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
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