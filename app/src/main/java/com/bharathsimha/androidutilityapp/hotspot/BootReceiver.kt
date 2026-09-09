package com.bharathsimha.androidutilityapp.hotspot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedules = HotspotScheduleStore(context).schedules.first()
                HotspotScheduler(context).scheduleAll(schedules)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
