package com.bharathsimha.androidutilityapp

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.bharathsimha.androidutilityapp.hotspot.HotspotSchedule
import com.bharathsimha.androidutilityapp.hotspot.HotspotScheduleStore
import com.bharathsimha.androidutilityapp.hotspot.HotspotScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        val store = HotspotScheduleStore(this)
        setContent {
            val loaded = remember { mutableStateOf<HotspotSchedule?>(null) }
            LaunchedEffect(Unit) { loaded.value = store.schedule.first() }
            MaterialTheme {
                loaded.value?.let { schedule ->
                    HotspotScreen(schedule, { newSchedule ->
                        lifecycleScope.launch {
                            store.save(newSchedule)
                            HotspotScheduler(this@MainActivity).scheduleNext(newSchedule)
                            loaded.value = newSchedule
                        }
                    }) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }
                } ?: Text("Loading…", modifier = Modifier.padding(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotspotScreen(
    initial: HotspotSchedule,
    onSave: (HotspotSchedule) -> Unit,
    onExactAlarmSettings: () -> Unit
) {
    var enabled by remember(initial) { mutableStateOf(initial.enabled) }
    var hour by remember(initial) { mutableStateOf(initial.hour) }
    var minute by remember(initial) { mutableStateOf(initial.minute) }
    var days by remember(initial) { mutableStateOf(initial.days) }

    Scaffold(topBar = { TopAppBar(title = { Text("Android Utilities") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Hotspot Auto-Off", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text("Schedule a reminder to turn off your hotspot.")
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column { Text("Auto-off", style = MaterialTheme.typography.titleMedium); Text(if (enabled) "Enabled" else "Disabled") }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            }
            item {
                Text("Turn off at", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { hour = (hour + 23) % 24 }) { Text("−") }
                    Text(LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("hh:mm a")), style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
                    OutlinedButton(onClick = { minute = (minute + 55) % 60 }) { Text("− min") }
                    OutlinedButton(onClick = { minute = (minute + 5) % 60 }) { Text("+ min") }
                }
            }
            item { Text("Repeat", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = days.size == 7, onClick = { days = DayOfWeek.entries.toSet() }, label = { Text("Every day") })
                    FilterChip(selected = days == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), onClick = { days = DayOfWeek.entries.filter { it.value <= 5 }.toSet() }, label = { Text("Weekdays") })
                    FilterChip(selected = days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), onClick = { days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }, label = { Text("Weekends") })
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(selected = day in days, onClick = { days = if (day in days) days - day else days + day }, label = { Text(day.name.take(1)) })
                    }
                }
            }
            item {
                Button(onClick = { onSave(HotspotSchedule(enabled, hour, minute, days)) }, modifier = Modifier.fillMaxWidth()) { Text("Save schedule") }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val alarmManager = androidx.compose.ui.platform.LocalContext.current.getSystemService(AlarmManager::class.java)
                    if (!alarmManager.canScheduleExactAlarms()) {
                        OutlinedButton(onClick = onExactAlarmSettings, modifier = Modifier.fillMaxWidth()) { Text("Allow exact alarms") }
                    }
                }
            }
        }
    }
}
