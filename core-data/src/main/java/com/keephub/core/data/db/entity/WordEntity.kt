package com.keephub.core.data.db.entity

import androidx.room.*
import java.time.Instant

@Entity(
    tableName = "words",
    indices = [Index(value = ["normalizedTerm"], unique = true)]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val normalizedTerm: String,           // lowercase, diacritic-stripped (dup check later)
    val baseLang: String = "en",
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
