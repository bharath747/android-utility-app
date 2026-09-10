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
import androidx.compose.foundation.lazy.items
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
import com.bharathsimha.androidutilityapp.hotspot.HotspotAutoOffController
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
            HotspotScheduler(this@MainActivity).scheduleAll(
                HotspotScheduleStore(this@MainActivity).schedules.first()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotspotScreen() {
    val context = LocalContext.current
    val store = remember { HotspotScheduleStore(context) }
    val controller = remember { HotspotAutoOffController(context) }
    val scope = rememberCoroutineScope()
    var schedules by remember { mutableStateOf<List<HotspotSchedule>>(emptyList()) }
    var editing by remember { mutableStateOf<HotspotSchedule?>(null) }
    var enabled by remember { mutableStateOf(true) }
    var hour by remember { mutableStateOf(23) }
    var minute by remember { mutableStateOf(30) }
    var days by remember { mutableStateOf(DayOfWeek.entries.toSet()) }
    var appManaged by remember { mutableStateOf(controller.isAppManaged()) }
    val snackbar = remember { SnackbarHostState() }
    val directControlAvailable = Build.VERSION.SDK_INT >= 36

    LaunchedEffect(Unit) {
        store.schedules.collect { schedules = it }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Android Utilities") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Hotspot Auto-Off", style = MaterialTheme.typography.headlineMedium)
                Text("Create one or more automatic hotspot turn-off schedules.")
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hotspot control", style = MaterialTheme.typography.titleMedium)
                        if (directControlAvailable) {
                            Text(if (appManaged) "✓ Hotspot is managed by this app" else "Hotspot is not managed by this app")
                            Text("Start the hotspot from this app when you want scheduled shutdown to control that session. Hotspots started outside the app may require the notification fallback.")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = !appManaged,
                                    onClick = {
                                        controller.tryStart { started ->
                                            scope.launch {
                                                appManaged = controller.isAppManaged()
                                                snackbar.showSnackbar(
                                                    if (started) "Hotspot started by Android Utilities"
                                                    else "Android did not allow the app to start the hotspot"
                                                )
                                            }
                                        }
                                    }
                                ) { Text("Start hotspot") }
                                OutlinedButton(
                                    enabled = appManaged,
                                    onClick = {
                                        controller.tryTurnOff { stopped ->
                                            scope.launch {
                                                appManaged = controller.isAppManaged()
                                                snackbar.showSnackbar(
                                                    if (stopped) "Hotspot turned off"
                                                    else "The app could not stop the hotspot"
                                                )
                                            }
                                        }
                                    }
                                ) { Text("Stop hotspot") }
                            }
                        } else {
                            Text("Automatic app-owned hotspot control requires Android 16 (API 36) or newer.")
                            Text("Schedules remain available and will show a notification at the scheduled time when direct control is unavailable.")
                        }
                        if (Build.VERSION.SDK_INT >= 36 && !Settings.System.canWrite(context)) {
                            Text("Some devices may require the system settings access below before allowing tethering control.")
                            OutlinedButton(onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }) { Text("Allow system settings access") }
                        }
                    }
                }
            }
            item {
                Text(
                    if (schedules.isEmpty()) "No saved schedules yet." else "Saved schedules",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(schedules, key = { it.id }) { schedule ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    LocalTime.of(schedule.hour, schedule.minute)
                                        .format(DateTimeFormatter.ofPattern("hh:mm a")),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(daySummary(schedule.days))
                                Text(if (schedule.enabled) "Enabled" else "Disabled")
                            }
                            Switch(checked = schedule.enabled, onCheckedChange = { checked ->
                                scope.launch {
                                    val updated = schedule.copy(enabled = checked)
                                    store.save(updated)
                                    if (checked) HotspotScheduler(context).scheduleNext(updated)
                                    else HotspotScheduler(context).cancel(updated.id)
                                    snackbar.showSnackbar(if (checked) "Schedule enabled" else "Schedule disabled")
                                }
                            })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                editing = schedule
                                enabled = schedule.enabled
                                hour = schedule.hour
                                minute = schedule.minute
                                days = schedule.days
                            }) { Text("Edit") }
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
                    FilterChip(selected = days.size == 7, onClick = { days = DayOfWeek.entries.toSet() }, label = { Text("Every day") })
                    FilterChip(selected = days == DayOfWeek.entries.filter { it.value <= 5 }.toSet(), onClick = { days = DayOfWeek.entries.filter { it.value <= 5 }.toSet() }, label = { Text("Weekdays") })
                    FilterChip(selected = days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), onClick = { days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }, label = { Text("Weekends") })
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(selected = day in days, onClick = { days = if (day in days) days - day else days + day }, label = { Text(day.name.take(1)) })
                    }
                }
            }
            item {
                Button(
                    enabled = days.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            val schedule = editing?.copy(enabled = enabled, hour = hour, minute = minute, days = days)
                                ?: HotspotSchedule(enabled = enabled, hour = hour, minute = minute, days = days)
                            store.save(schedule)
                            if (schedule.enabled) HotspotScheduler(context).scheduleNext(schedule)
                            else HotspotScheduler(context).cancel(schedule.id)
                            editing = null
                            snackbar.showSnackbar(if (schedule.enabled) "Schedule saved" else "Schedule saved disabled")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editing == null) "Save schedule" else "Update schedule") }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                    if (!alarmManager.canScheduleExactAlarms()) {
                        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Allow exact alarms")
                        }
                    }
                }
            }
        }
    }
}

private fun daySummary(days: Set<DayOfWeek>): String = when {
    days.size == 7 -> "Every day"
    days == DayOfWeek.entries.filter { it.value <= 5 }.toSet() -> "Weekdays"
    days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
    days.isEmpty() -> "No days selected"
    else -> days.sortedBy { it.value }.joinToString(", ") { it.name.take(3) }
}
