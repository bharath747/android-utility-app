package com.bharathsimha.androidutilityapp.hotspot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class HotspotShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedule = HotspotScheduleStore(context).schedule.first()
                if (!schedule.enabled) return@launch

                val now = ZonedDateTime.now()
                if (now.dayOfWeek in schedule.days) {
                    showNotification(context)
                }
                HotspotScheduler(context).scheduleNext(schedule)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Hotspot automation", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val openIntent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
        val pending = PendingIntent.getActivity(
            context, 100, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hotspot auto-off time reached")
            .setContentText("Tap to open wireless settings and turn off the hotspot.")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "hotspot_automation"
        const val NOTIFICATION_ID = 2002
    }
}
