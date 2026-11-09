package com.keephub.app

import com.keephub.app.work.DueReviewWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class DueReviewWorkerTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun testTargetHourInFutureSameDay() {
        // Given current time is 10:00:00 UTC and target is 19:00 (7 PM)
        val now = ZonedDateTime.of(2026, 11, 10, 10, 0, 0, 0, zone)
        val delayMs = DueReviewWorker.computeDelayMs(hour24 = 19, forceNextDay = false, now = now)

        // Expected delay is 9 hours
        val expectedMs = TimeUnit.HOURS.toMillis(9)
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun testTargetHourInPastAdvancesToTomorrow() {
        // Given current time is 20:30:00 UTC and target is 19:00 (which passed today)
        val now = ZonedDateTime.of(2026, 11, 10, 20, 30, 0, 0, zone)
        val delayMs = DueReviewWorker.computeDelayMs(hour24 = 19, forceNextDay = false, now = now)

        // Expected delay is from 20:30 today to 19:00 tomorrow: 22 hours and 30 minutes
        val expectedMs = TimeUnit.HOURS.toMillis(22) + TimeUnit.MINUTES.toMillis(30)
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun testForceNextDayWhenWorkerRunsAtTargetHour() {
        // Given current time is exactly 19:00:00 UTC (worker just fired for today)
        val now = ZonedDateTime.of(2026, 11, 10, 19, 0, 0, 0, zone)
        val delayMs = DueReviewWorker.computeDelayMs(hour24 = 19, forceNextDay = true, now = now)

        // Expected delay is exactly 24 hours (for tomorrow's reminder)
        val expectedMs = TimeUnit.HOURS.toMillis(24)
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun testNearTargetExecutionAdvancesToNextDay() {
        // Given current time is 18:59:00 UTC (1 minute before target 19:00)
        // With a 2-minute safety window, it should advance to tomorrow to prevent immediate double-execution
        val now = ZonedDateTime.of(2026, 11, 10, 18, 59, 0, 0, zone)
        val delayMs = DueReviewWorker.computeDelayMs(hour24 = 19, forceNextDay = false, now = now)

        // Expected delay is 24 hours + 1 minute (tomorrow at 19:00)
        val expectedMs = TimeUnit.HOURS.toMillis(24) + TimeUnit.MINUTES.toMillis(1)
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun testMidnightTarget() {
        // Current time: 22:00, target: 0 (midnight)
        val now = ZonedDateTime.of(2026, 11, 10, 22, 0, 0, 0, zone)
        val delayMs = DueReviewWorker.computeDelayMs(hour24 = 0, forceNextDay = false, now = now)

        // Expected delay is 2 hours (00:00:00 next day)
        val expectedMs = TimeUnit.HOURS.toMillis(2)
        assertEquals(expectedMs, delayMs)
    }

    @Test
    fun testHourCoercion() {
        // If hour passed is out of range like 25, it should be coerced to 23
        val now = ZonedDateTime.of(2026, 11, 10, 20, 0, 0, 0, zone)
        val delayMs = DueReviewWorker.computeDelayMs(hour24 = 25, forceNextDay = false, now = now)

        // 20:00 to 23:00 is 3 hours
        val expectedMs = TimeUnit.HOURS.toMillis(3)
        assertEquals(expectedMs, delayMs)
    }
}
