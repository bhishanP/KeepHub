package com.keephub.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.keephub.app.MainActivity
import com.keephub.app.R

object ReviewNotification {
    const val CHANNEL_ID = "keephub_review"
    private const val CHANNEL_NAME = "KeepHub Reviews"
    private const val NOTIF_ID = 1001

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                )
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
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = "KeepHub — Reviews due"
        val text = if (dueCount <= 0) "Time to review your words" else "You have $dueCount words to review today"
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name) // add a small drawable; fallback to app icon if missing
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(ctx))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notif)
    }
}
