package com.bharathsimha.androidutilityapp.hotspot

import java.time.DayOfWeek

data class HotspotSchedule(
    val id: Long = System.currentTimeMillis(),
    val enabled: Boolean = false,
    val hour: Int = 23,
    val minute: Int = 30,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet()
)
