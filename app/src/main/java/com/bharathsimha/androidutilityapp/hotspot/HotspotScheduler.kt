package com.bharathsimha.androidutilityapp.hotspot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZonedDateTime

class HotspotScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleAll(schedules: List<HotspotSchedule>) {
        schedules.forEach { scheduleNext(it) }
    }

    fun scheduleNext(schedule: HotspotSchedule) {
        cancel(schedule.id)
        if (!schedule.enabled || schedule.days.isEmpty()) return
        val now = ZonedDateTime.now()
        val next = (0..7).asSequence()
            .map { now.plusDays(it.toLong()).withHour(schedule.hour).withMinute(schedule.minute).withSecond(0).withNano(0) }
            .firstOrNull { it.isAfter(now) && it.dayOfWeek in schedule.days } ?: return
        val intent = Intent(context, HotspotShutdownReceiver::class.java).apply { putExtra(EXTRA_SCHEDULE_ID, schedule.id) }
        val pending = PendingIntent.getBroadcast(context, schedule.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val trigger = next.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun cancel(id: Long) {
        val intent = Intent(context, HotspotShutdownReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) alarmManager.cancel(pending)
    }

    companion object { const val EXTRA_SCHEDULE_ID = "schedule_id" }
}
