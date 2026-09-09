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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        setContent { MaterialTheme { HotspotScreen() } }
    }

    override fun onResume() {
        super.onResume()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            HotspotScheduler(this@MainActivity).scheduleAll(HotspotScheduleStore(this@MainActivity).schedules.first())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotspotScreen() {
    val context = LocalContext.current
    val store = remember { HotspotScheduleStore(context) }
    val scope = rememberCoroutineScope()
    var schedules by remember { mutableStateOf<List<HotspotSchedule>>(emptyList()) }
    var editing by remember { mutableStateOf<HotspotSchedule?>(null) }
    var enabled by remember { mutableStateOf(true) }
    var hour by remember { mutableStateOf(23) }
    var minute by remember { mutableStateOf(30) }
    var days by remember { mutableStateOf(DayOfWeek.entries.toSet()) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { store.schedules.collect { schedules = it } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Android Utilities") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Hotspot Auto-Off", style = MaterialTheme.typography.headlineMedium)
                Text("Create one or more automatic hotspot turn-off schedules.")
            }
            item { Text(if (schedules.isEmpty()) "No saved schedules yet." else "Saved schedules", style = MaterialTheme.typography.titleLarge) }
            items(schedules.size) { index ->
                val schedule = schedules[index]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(LocalTime.of(schedule.hour, schedule.minute).format(DateTimeFormatter.ofPattern("hh:mm a")), style = MaterialTheme.typography.titleLarge)
                                Text(daySummary(schedule.days))
                                Text(if (schedule.enabled) "Enabled" else "Disabled")
                            }
                            Switch(checked = schedule.enabled, onCheckedChange = { checked ->
                                scope.launch {
                                    val updated = schedule.copy(enabled = checked)
                                    store.save(updated)
                                    if (checked) HotspotScheduler(context).scheduleNext(updated) else HotspotScheduler(context).cancel(updated.id)
                                    snackbar.showSnackbar(if (checked) "Schedule enabled" else "Schedule disabled")
                                }
                            })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = schedule; enabled = schedule.enabled; hour = schedule.hour; minute = schedule.minute; days = schedule.days }) { Text("Edit") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    HotspotScheduler(context).cancel(schedule.id)
                                    store.delete(schedule.id)
                                    snackbar.showSnackbar("Schedule deleted")
                                }
                            }) { Text("Delete") }
                        }
                    }
                }
            }
            item {
                Text(if (editing == null) "New schedule" else "Edit schedule", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { hour = (hour + 23) % 24 }) { Text("−") }
                    Text(LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("hh:mm a")), style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
                    OutlinedButton(onClick = { minute = (minute + 55) % 60 }) { Text("−5m") }
                    OutlinedButton(onClick = { minute = (minute + 5) % 60 }) { Text("+5m") }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Text("Repeat", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(days.size == 7, { days = DayOfWeek.entries.toSet() }, label = { Text("Every day") })
                    FilterChip(days == DayOfWeek.entries.filter { it.value <= 5 }.toSet(), { days = DayOfWeek.entries.filter { it.value <= 5 }.toSet() }, label = { Text("Weekdays") })
                    FilterChip(days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), { days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }, label = { Text("Weekends") })
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(day in days, { days = if (day in days) days - day else days + day }, label = { Text(day.name.take(1)) })
                    }
                }
            }
            item {
                Button(enabled = days.isNotEmpty(), onClick = {
                    scope.launch {
                        val schedule = editing?.copy(enabled = enabled, hour = hour, minute = minute, days = days)
                            ?: HotspotSchedule(enabled = enabled, hour = hour, minute = minute, days = days)
                        store.save(schedule)
                        if (schedule.enabled) HotspotScheduler(context).scheduleNext(schedule) else HotspotScheduler(context).cancel(schedule.id)
                        editing = null
                        snackbar.showSnackbar(if (schedule.enabled) "Schedule saved" else "Schedule saved disabled")
                    }
                }, Modifier.fillMaxWidth()) { Text(if (editing == null) "Save schedule" else "Update schedule") }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                    if (!alarmManager.canScheduleExactAlarms()) OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }, Modifier.fillMaxWidth()) { Text("Allow exact alarms") }
                }
            }
        }
    }
}

private fun daySummary(days: Set<DayOfWeek>): String = when {
    days.size == 7 -> "Every day"
    days == DayOfWeek.entries.filter { it.value <= 5 }.toSet() -> "Weekdays"
    days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
    else -> days.sortedBy { it.value }.joinToString(", ") { it.name.take(3) }
}
