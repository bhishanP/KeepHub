package com.keephub.app

import android.app.Application
import androidx.work.ExistingWorkPolicy
import com.keephub.app.notifications.ReviewNotification
import com.keephub.app.work.DueReviewWorker
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KeepHubApp : Application() {

    @Inject lateinit var settingsStore: SettingsStore

    override fun onCreate() {
        super.onCreate()
        // Create channel early (safe no-op if exists)
        ReviewNotification.ensureChannel(this)

        // Ensure daily reminder is scheduled using the user's saved notifyHour.
        // ExistingWorkPolicy.KEEP ensures active schedules are preserved without resetting countdown.
        CoroutineScope(Dispatchers.Default).launch {
            val hour = runCatching { settingsStore.notifyHour.first() }.getOrDefault(19)
            DueReviewWorker.scheduleDailyReminder(
                ctx = this@KeepHubApp,
                hour24 = hour,
                policy = ExistingWorkPolicy.KEEP
            )
        }
    }
}
