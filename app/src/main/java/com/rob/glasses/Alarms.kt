package com.rob.glasses

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** Timers keep working when the app is closed or the phone is asleep. */
object Alarms {
    private fun intent(ctx: Context, id: String, label: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id.hashCode(),
            Intent(ctx, AlarmReceiver::class.java).putExtra("label", label).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(ctx: Context, id: String, atMillis: Long, label: String) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = intent(ctx, id, label)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun cancel(ctx: Context, id: String) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(intent(ctx, id, ""))
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, data: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val label = data.getStringExtra("label") ?: "Timer"
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel("timers", "Timers", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
        }
        nm.createNotificationChannel(channel)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, "timers")
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("⏰ $label")
            .setContentText("Time's up!")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .build()
        nm.notify((data.getStringExtra("id") ?: label).hashCode(), n)
    }
}
