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
        val id = intent?.getLongExtra(HotspotScheduler.EXTRA_SCHEDULE_ID, -1L) ?: -1L

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedules = HotspotScheduleStore(context).schedules.first()
                val schedule = schedules.firstOrNull { it.id == id && it.enabled }
                if (schedule != null && ZonedDateTime.now().dayOfWeek in schedule.days) {
                    val turnedOff = HotspotAutoOffController(context).tryTurnOff()
                    if (!turnedOff) {
                        showNotification(context, schedule.id)
                    } else {
                        showSuccessNotification(context, schedule.id)
                    }
                    HotspotScheduler(context).scheduleNext(schedule)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, scheduleId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Hotspot automation", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val pending = PendingIntent.getActivity(
            context, 100,
            Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hotspot auto-off time reached")
            .setContentText("Automatic shutdown is unavailable. Tap to open wireless settings.")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .addAction(android.R.drawable.ic_menu_manage, "Open settings", pending)
            .build()
        manager.notify(scheduleId.hashCode(), notification)
    }

    private fun showSuccessNotification(context: Context, scheduleId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Hotspot automation", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hotspot turned off")
            .setContentText("The scheduled hotspot shutdown completed successfully.")
            .setAutoCancel(true)
            .build()
        manager.notify(scheduleId.hashCode(), notification)
    }

    companion object { const val CHANNEL_ID = "hotspot_automation" }
}
