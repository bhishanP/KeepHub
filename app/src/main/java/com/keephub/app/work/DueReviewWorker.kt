package com.keephub.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.keephub.app.notifications.ReviewNotification
import com.keephub.core.data.daily.DailyWordsManager
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class DueReviewWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppDeps {
        fun repo(): WordRepository
        fun settings(): SettingsStore
        fun dailyWordsManager(): DailyWordsManager
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        ReviewNotification.ensureChannel(ctx)

        val deps = EntryPointAccessors.fromApplication(ctx, AppDeps::class.java)
        val repo = deps.repo()
        val settings = deps.settings()
        val dailyWordsManager = deps.dailyWordsManager()

        val today = LocalDate.now()
        // Auto-enroll today's 5 words if enabled and not yet processed
        runCatching { dailyWordsManager.ensureDailyWordsAutoAdded(today) }

        val goal = settings.dailyGoal.first()           // e.g., 20
        val ids = runCatching { repo.generateDailyQueue(goal, today) }.getOrElse { emptyList() }
        if (ids.isNotEmpty()) {
            ReviewNotification.show(ctx, ids.size)
        }

        // Schedule the next run for the chosen hour (guaranteed next day)
        val hour = settings.notifyHour.first()          // e.g., 19
        scheduleDailyReminder(ctx, hour, policy = ExistingWorkPolicy.REPLACE, forceNextDay = true)

        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "keephub_due_review"

        fun scheduleDailyReminder(
            ctx: Context,
            hour24: Int,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
            forceNextDay: Boolean = false
        ) {
            val delayMs = computeDelayMs(hour24, forceNextDay)
            val req = OneTimeWorkRequestBuilder<DueReviewWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork(UNIQUE_NAME, policy, req)
        }

        fun computeDelayMs(
            hour24: Int,
            forceNextDay: Boolean = false,
            now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
        ): Long {
            var next = now.withHour(hour24.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
            if (forceNextDay || !next.isAfter(now.plusMinutes(2))) {
                next = next.plusDays(1)
            }
            return Duration.between(now, next).toMillis().coerceAtLeast(0)
        }

        fun enqueueForHour(ctx: Context, hour24: Int) {
            scheduleDailyReminder(ctx, hour24, ExistingWorkPolicy.REPLACE, forceNextDay = false)
        }
    }
}
