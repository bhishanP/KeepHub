package com.keephub.app

import com.google.common.truth.Truth.assertThat
import com.keephub.app.ui.screens.WordListViewModel
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 9)

    private fun instantFor(year: Int, month: Int, day: Int, hour: Int = 12): Instant =
        LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(zone)
            .toInstant()

    @Test
    fun emptyTimestamps_returnsZero() {
        val streak = WordListViewModel.calculateStreak(emptyList(), today)
        assertThat(streak).isEqualTo(0)
    }

    @Test
    fun reviewedTodayOnly_returnsOne() {
        val timestamps = listOf(instantFor(2026, 11, 9))
        val streak = WordListViewModel.calculateStreak(timestamps, today)
        assertThat(streak).isEqualTo(1)
    }

    @Test
    fun reviewedYesterdayOnly_returnsOne() {
        // User has not reviewed yet today, but streak from yesterday is still alive
        val timestamps = listOf(instantFor(2026, 11, 8))
        val streak = WordListViewModel.calculateStreak(timestamps, today)
        assertThat(streak).isEqualTo(1)
    }

    @Test
    fun consecutiveThreeDays_returnsThree() {
        val timestamps = listOf(
            instantFor(2026, 11, 9, 10),
            instantFor(2026, 11, 9, 15), // multiple on same day
            instantFor(2026, 11, 8, 12),
            instantFor(2026, 11, 7, 18)
        )
        val streak = WordListViewModel.calculateStreak(timestamps, today)
        assertThat(streak).isEqualTo(3)
    }

    @Test
    fun missedDay_streakResets() {
        val timestamps = listOf(
            instantFor(2026, 11, 9),
            instantFor(2026, 11, 7), // missed Nov 8!
            instantFor(2026, 11, 6)
        )
        val streak = WordListViewModel.calculateStreak(timestamps, today)
        assertThat(streak).isEqualTo(1)
    }

    @Test
    fun oldReviewsOnly_returnsZero() {
        val timestamps = listOf(
            instantFor(2026, 11, 5),
            instantFor(2026, 11, 4)
        )
        val streak = WordListViewModel.calculateStreak(timestamps, today)
        assertThat(streak).isEqualTo(0)
    }
}
