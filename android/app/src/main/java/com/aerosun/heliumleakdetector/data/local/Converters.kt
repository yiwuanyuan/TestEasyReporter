package com.aerosun.heliumleakdetector.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room 类型转换器。
 *
 * 将复杂类型（List, Map 等）与 SQLite 兼容的字符串（JSON）之间互相转换。
 */
object Converters {

    private val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun fromStringList(value: List<String>?): String =
        gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    @JvmStatic
    fun toStringList(value: String): List<String> {
        return try {
            // 优先按 JSON 数组解析
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            // 兼容旧格式：管道符 "|" 分隔的字符串
            if (value.isNotBlank()) value.split("|").map { it.trim() }
            else emptyList()
        }
    }
}
