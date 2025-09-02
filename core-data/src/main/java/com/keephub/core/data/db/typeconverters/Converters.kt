package com.keephub.core.data.db.typeconverters

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

@ProvidedTypeConverter
class Converters(private val json: Json = Json) {

    @TypeConverter fun listToJson(value: List<String>?): String =
        json.encodeToString(ListSerializer(String.serializer()), value ?: emptyList())

    @TypeConverter fun jsonToList(value: String?): List<String> =
        value?.let { json.decodeFromString(ListSerializer(String.serializer()), it) } ?: emptyList()

    @TypeConverter fun instantToLong(i: Instant?): Long? = i?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun localDateToStr(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun strToLocalDate(s: String?): LocalDate? = s?.let(LocalDate::parse)
}
