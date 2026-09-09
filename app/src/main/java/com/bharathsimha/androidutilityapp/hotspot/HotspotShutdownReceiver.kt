package com.bharathsimha.androidutilityapp.hotspot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

class HotspotShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = HotspotScheduleStore(context)
                val schedule = store.schedule.first()
                if (!schedule.enabled) return@launch

                val now = ZonedDateTime.now()
                if (now.dayOfWeek !in schedule.days) return@launch

                val controller = HotspotController(context)
                controller.openHotspotSettings()
                showNotification(context)
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
        val pending = PendingIntentCompat.getActivity(
            context, 100, openIntent, 0, false
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_wifi)
            .setContentTitle("Hotspot auto-off")
            .setContentText("Android requires system-level access to switch off an existing hotspot. Hotspot settings are open for you to turn it off.")
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
