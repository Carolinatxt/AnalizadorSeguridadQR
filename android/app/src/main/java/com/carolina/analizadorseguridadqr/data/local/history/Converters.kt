package com.carolina.analizadorseguridadqr.data.local.history

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromReasons(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toReasons(value: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(value, type) ?: emptyList()
        } catch (_: Exception) {
            // Si el contenido no es parseable, devolvemos una lista segura vacía.
            emptyList()
        }
    }
}
