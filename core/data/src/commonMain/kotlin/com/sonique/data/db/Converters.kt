package com.sonique.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.metadata.Line
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ProvidedTypeConverter
class Converters {
     
    val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    @TypeConverter
    fun fromString(value: String?): List<String>? =
        try {
            value?.let { json.decodeFromString<List<String>>(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromArrayList(list: List<String>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromStringToListTrack(value: String?): List<Track>? =
        try {
            value?.let {
                json.decodeFromString<List<Track>>(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromListTrackToString(list: List<Track>?): String? =
        list?.let {
            json.encodeToString(it)
        }

    @TypeConverter
    fun fromListLineToString(list: List<Line>?): String? =
        list?.let {
            json.encodeToString(it)
        }

    @TypeConverter
    fun fromStringToListLine(value: String?): List<Line>? =
        try {
            value?.let {
                json.decodeFromString<List<Line>>(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromStringNull(value: String?): List<String?>? =
        try {
            value?.let {
                json.decodeFromString<List<String?>>(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromArrayListNull(list: List<String?>?): String? =
        list?.let {
            json.encodeToString(it)
        }

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        if (value != null) {
            Instant.fromEpochMilliseconds(value).toLocalDateTime(TimeZone.UTC)
        } else {
            null
        }

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? = date?.toInstant(TimeZone.UTC)?.toEpochMilliseconds()

    @TypeConverter
    fun fromListMapToString(list: List<Map<String, String>>): String = json.encodeToString(list)

    @TypeConverter
    fun fromStringToListMap(value: String): List<Map<String, String>> = json.decodeFromString(value)
}

