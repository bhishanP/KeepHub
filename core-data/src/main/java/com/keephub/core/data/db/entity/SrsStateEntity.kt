package com.keephub.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "srs_state")
data class SrsStateEntity(
    @PrimaryKey val wordId: Long,
    val easiness: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueDate: LocalDate = LocalDate.now(),
    val lapses: Int = 0,
    val historyJson: String = "[]"
)
