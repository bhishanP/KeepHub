package com.keephub.core.data.db.entity

import androidx.room.*

@Entity(
    tableName = "translations",
    foreignKeys = [ForeignKey(
        entity = WordEntity::class,
        parentColumns = ["id"],
        childColumns = ["wordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["wordId","languageCode"], unique = true)]
)
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val languageCode: String,
    val text: String
)
