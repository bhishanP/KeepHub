package com.keephub.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.keephub.app.MainActivity
import com.keephub.app.R

object ReviewNotification {
    const val CHANNEL_ID = "keephub_review"
    private const val CHANNEL_NAME = "KeepHub Reviews"
    private const val NOTIF_ID = 1001
    private const val TEST_NOTIF_ID = 1002

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily vocabulary review reminders"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun contentIntent(ctx: Context): PendingIntent? {
        val intent = Intent(ctx, MainActivity::class.java)
            .putExtra("extra_open_review", true)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return TaskStackBuilder.create(ctx)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(0, piFlags)
    }

    fun show(ctx: Context, dueCount: Int) {
        val nmc = NotificationManagerCompat.from(ctx)
        if (!nmc.areNotificationsEnabled()) return

        val title = "KeepHub — Reviews due"
        val text = if (dueCount <= 0) "Time to review your words" else "You have $dueCount words to review today"
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_review)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(ctx))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            nmc.notify(NOTIF_ID, notif)
        } catch (_: SecurityException) {
            // Android 13+ permission check safety
        }
    }

    fun showTest(ctx: Context) {
        ensureChannel(ctx)
        val nmc = NotificationManagerCompat.from(ctx)
        if (!nmc.areNotificationsEnabled()) return

        val title = "KeepHub — Test Notification"
        val text = "Your daily review reminder is working properly! Tap here to review words."
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_review)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(ctx))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            nmc.notify(TEST_NOTIF_ID, notif)
        } catch (_: SecurityException) {
            // Android 13+ permission check safety
        }
    }
}
