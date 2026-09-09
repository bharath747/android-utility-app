package com.bharathsimha.androidutilityapp.hotspot

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.hotspotDataStore by preferencesDataStore("hotspot_settings")

class HotspotScheduleStore(private val context: Context) {
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val hour = intPreferencesKey("hour")
        val minute = intPreferencesKey("minute")
        val days = stringPreferencesKey("days")
    }

    val schedule: Flow<HotspotSchedule> = context.hotspotDataStore.data.map { p ->
        val days = p[Keys.days].orEmpty().split(",")
            .mapNotNull { it.toIntOrNull() }
            .mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }
            .toSet()
        HotspotSchedule(
            enabled = p[Keys.enabled] ?: false,
            hour = p[Keys.hour] ?: 23,
            minute = p[Keys.minute] ?: 30,
            days = if (days.isEmpty()) DayOfWeek.values().toSet() else days
        )
    }

    suspend fun save(schedule: HotspotSchedule) {
        context.hotspotDataStore.edit { p ->
            p[Keys.enabled] = schedule.enabled
            p[Keys.hour] = schedule.hour
            p[Keys.minute] = schedule.minute
            p[Keys.days] = schedule.days.sortedBy { it.value }.joinToString(",") { it.value.toString() }
        }
    }
}
