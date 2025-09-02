package com.keephub.core.data.db.entity

import androidx.room.*

@Entity(
    tableName = "senses",
    foreignKeys = [ForeignKey(
        entity = WordEntity::class,
        parentColumns = ["id"],
        childColumns = ["wordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("wordId")]
)
data class SenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val pos: String?,
    val definition: String,
    val ipa: String? = null,
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val audioUrls: List<String> = emptyList()
)
