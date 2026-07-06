package featuresAPI.settings.data

import android.content.Context

data class TrackingPreferences(
    val updateIntervalMillis: Long = TrackingPreferencesRepository.DEFAULT_UPDATE_INTERVAL_MILLIS,
    val minDistanceMeters: Float = TrackingPreferencesRepository.DEFAULT_MIN_DISTANCE_METERS
)

// Keep these local since tracking preferences are phone-specific.
class TrackingPreferencesRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getPreferences(): TrackingPreferences {
        return TrackingPreferences(
            updateIntervalMillis = preferences.getLong(
                KEY_UPDATE_INTERVAL_MILLIS,
                DEFAULT_UPDATE_INTERVAL_MILLIS
            ),
            minDistanceMeters = preferences.getFloat(
                KEY_MIN_DISTANCE_METERS,
                DEFAULT_MIN_DISTANCE_METERS
            )
        )
    }

    fun saveUpdateIntervalMillis(updateIntervalMillis: Long) {
        preferences.edit()
            .putLong(KEY_UPDATE_INTERVAL_MILLIS, updateIntervalMillis)
            .apply()
    }

    fun saveMinDistanceMeters(minDistanceMeters: Float) {
        preferences.edit()
            .putFloat(KEY_MIN_DISTANCE_METERS, minDistanceMeters)
            .apply()
    }

    companion object {
        // Defaults match the old hardcoded tracking behavior.
        const val DEFAULT_UPDATE_INTERVAL_MILLIS = 5_000L
        const val DEFAULT_MIN_DISTANCE_METERS = 5f

        private const val PREFERENCES_NAME = "tracking_preferences"
        private const val KEY_UPDATE_INTERVAL_MILLIS = "tracking_update_interval_millis"
        private const val KEY_MIN_DISTANCE_METERS = "tracking_min_distance_meters"
    }
}
