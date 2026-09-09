package com.bharathsimha.androidutilityapp.hotspot

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.hotspotDataStore by preferencesDataStore("hotspot_settings")

class HotspotScheduleStore(private val context: Context) {
    private val schedulesKey = stringPreferencesKey("schedules")

    val schedules: Flow<List<HotspotSchedule>> = context.hotspotDataStore.data.map { prefs ->
        decode(prefs[schedulesKey].orEmpty())
    }

    suspend fun save(schedule: HotspotSchedule) {
        context.hotspotDataStore.edit { prefs ->
            val current = decode(prefs[schedulesKey].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.id == schedule.id }
            if (index >= 0) current[index] = schedule else current.add(schedule)
            prefs[schedulesKey] = encode(current)
        }
    }

    suspend fun delete(id: Long) {
        context.hotspotDataStore.edit { prefs ->
            prefs[schedulesKey] = encode(decode(prefs[schedulesKey].orEmpty()).filterNot { it.id == id })
        }
    }

    private fun encode(items: List<HotspotSchedule>): String = items.joinToString("|") { s ->
        "${s.id};${if (s.enabled) 1 else 0};${s.hour};${s.minute};${s.days.sortedBy { it.value }.joinToString(",") { it.value.toString() }}"
    }

    private fun decode(value: String): List<HotspotSchedule> = value.split("|").mapNotNull { row ->
        if (row.isBlank()) return@mapNotNull null
        val p = row.split(";")
        if (p.size != 5) return@mapNotNull null
        runCatching {
            HotspotSchedule(
                id = p[0].toLong(), enabled = p[1] == "1", hour = p[2].toInt(), minute = p[3].toInt(),
                days = p[4].split(",").mapNotNull { it.toIntOrNull() }.mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }.toSet()
            )
        }.getOrNull()
    }
}
