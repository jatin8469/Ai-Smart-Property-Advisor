package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PropertyType
import org.json.JSONArray

class PropertyTypeConverters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        val jsonArray = JSONArray()
        list?.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(value)
            val result = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                result.add(jsonArray.getString(i))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromPropertyType(type: PropertyType): String {
        return type.name
    }

    @TypeConverter
    fun toPropertyType(name: String): PropertyType {
        return try {
            PropertyType.valueOf(name)
        } catch (e: Exception) {
            PropertyType.APARTMENT
        }
    }
}
