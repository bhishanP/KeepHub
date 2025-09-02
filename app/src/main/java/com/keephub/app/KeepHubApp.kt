package com.keephub.app

import android.app.Application
import com.keephub.app.notifications.ReviewNotification
import com.keephub.app.work.DueReviewWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class KeepHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create channel early (safe no-op if exists)
        ReviewNotification.ensureChannel(this)

        // Kick scheduling using a default hour (19) until SettingsStore emits in the worker.
        CoroutineScope(Dispatchers.Default).launch {
            DueReviewWorker.enqueueForHour(this@KeepHubApp, 19)

        }
    }
}
