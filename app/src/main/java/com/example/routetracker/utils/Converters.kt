package com.example.routetracker.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// https://developer.android.com/training/data-storage/room/referencing-data
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null)
        {
            return null
        }
        else
        {
            return gson.toJson(value)
        }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {

        // When the list is empty (Not shared yet route) return null
        if (value.isNullOrEmpty()) return null

        return try
        {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType)
        }
        catch (e: Exception)
        {
            null
        }
    }
}