package com.bharathsimha.androidutilityapp.hotspot

import java.time.DayOfWeek

/** A recurring local-time schedule for the hotspot auto-off utility. */
data class HotspotSchedule(
    val enabled: Boolean = false,
    val hour: Int = 23,
    val minute: Int = 30,
    val days: Set<DayOfWeek> = DayOfWeek.values().toSet()
)
