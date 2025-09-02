package com.keephub.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.keephub.app.notifications.ReviewNotification
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
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        ReviewNotification.ensureChannel(ctx)

        val deps = EntryPointAccessors.fromApplication(ctx, AppDeps::class.java)
        val repo = deps.repo()
        val settings = deps.settings()

        val today = LocalDate.now()
        val goal = settings.dailyGoal.first()           // e.g., 20
        val ids = runCatching { repo.generateDailyQueue(goal, today) }.getOrElse { emptyList() }
        if (ids.isNotEmpty()) {
            ReviewNotification.show(ctx, ids.size)
        }

        // Schedule the next run for the chosen hour
        val hour = settings.notifyHour.first()          // e.g., 19
        enqueueForHour(ctx, hour)

        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "keephub_due_review"

        fun enqueueForHour(ctx: Context, hour24: Int) {
            val delayMs = computeDelayMs(hour24)
            val req = OneTimeWorkRequestBuilder<DueReviewWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, req)
        }

        private fun computeDelayMs(hour24: Int): Long {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            var next = now.withHour(hour24.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next).toMillis()
        }
    }
}
