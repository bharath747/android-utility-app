package com.bharathsimha.androidutilityapp.hotspot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class HotspotScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleNext(schedule: HotspotSchedule) {
        cancel()
        if (!schedule.enabled || schedule.days.isEmpty()) return

        val now = ZonedDateTime.now()
        val next = (0..7).asSequence()
            .map { now.plusDays(it.toLong()).withHour(schedule.hour).withMinute(schedule.minute).withSecond(0).withNano(0) }
            .firstOrNull { it.isAfter(now) && it.dayOfWeek in schedule.days }
            ?: return

        val intent = Intent(context, HotspotShutdownReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val trigger = next.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancel() {
        val intent = Intent(context, HotspotShutdownReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val REQUEST_CODE = 2001
    }
}
