package com.keephub.core.data.srs

import com.keephub.core.data.db.entity.SrsStateEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SM-2 style scheduler.
 * quality: 0..5 (5 = perfect recall). 0-2 = lapse.
 */
object SrsEngine {

    data class Result(
        val next: SrsStateEntity,
        val graduated: Boolean,       // became interval >= 1+? not strictly needed, useful for UI
        val lapsed: Boolean
    )

    fun review(today: LocalDate, current: SrsStateEntity?, quality: Int, json: Json = Json): Result {
        val q = quality.coerceIn(0, 5)
        val base = current ?: SrsStateEntity(
            wordId = -1,                 // caller must fill correct wordId later
            easiness = 2.5,
            intervalDays = 0,
            repetitions = 0,
            dueDate = today,
            lapses = 0,
            historyJson = "[]"
        )

        val history = json.decodeFromString<List<Int>>(base.historyJson)
        val newHistory = history + q

        if (q < 3) {
            // Lapse
            val newE = max(1.3, base.easiness - 0.8 + 0.28 * q - 0.02 * q * q)
            val next = base.copy(
                easiness = newE,
                intervalDays = 1,          // immediate short interval
                repetitions = 0,
                dueDate = today.plusDays(1),
                lapses = base.lapses + 1,
                historyJson = json.encodeToString(newHistory)
            )
            return Result(next, graduated = false, lapsed = true)
        } else {
            // Success
            val newReps = base.repetitions + 1
            val newE = max(1.3, base.easiness - 0.8 + 0.28 * q - 0.02 * q * q)
            val newInterval = when (newReps) {
                1 -> 1
                2 -> 6
                else -> (base.intervalDays * newE).roundToInt().coerceAtLeast(1)
            }
            val next = base.copy(
                easiness = newE,
                intervalDays = newInterval,
                repetitions = newReps,
                dueDate = today.plusDays(newInterval.toLong()),
                historyJson = json.encodeToString(newHistory)
            )
            val graduated = newReps >= 2
            return Result(next, graduated = graduated, lapsed = false)
        }
    }
}
