package app.rope.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.rope.android.RopeShapes
import app.rope.android.data.ScheduleRules
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendOptionsSheet(
    onSilent: () -> Unit,
    onSchedule: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = RopeShapes.card, topEnd = RopeShapes.card),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onSilent, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.NotificationsOff, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(ScheduleRules.SILENT, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onSchedule, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(ScheduleRules.DEFER, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTimeSheet(
    nowMs: Long = System.currentTimeMillis(),
    onConfirm: (fireAtMs: Long, silent: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var silent by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDay by remember { mutableStateOf<Long?>(null) }
    val hour = ScheduleRules.hourFromNow(nowMs)
    val tomorrow = ScheduleRules.tomorrowNine(nowMs)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = RopeShapes.card, topEnd = RopeShapes.card),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(ScheduleRules.DEFER, style = MaterialTheme.typography.titleMedium)
            if (ScheduleRules.inWindow(nowMs, hour)) {
                TextButton(
                    onClick = { onConfirm(hour, silent) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Через 1 час · ${ScheduleRules.formatWhen(hour)}", modifier = Modifier.weight(1f))
                }
            }
            if (ScheduleRules.inWindow(nowMs, tomorrow)) {
                TextButton(
                    onClick = { onConfirm(tomorrow, silent) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Завтра, 9:00", modifier = Modifier.weight(1f))
                }
            }
            TextButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Свой срок", modifier = Modifier.weight(1f))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { silent = !silent }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = silent, onCheckedChange = { silent = it })
                Spacer(Modifier.width(8.dp))
                Text(ScheduleRules.SILENT)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (showDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = nowMs + ScheduleRules.MIN_DELAY_MS)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDay = dateState.selectedDateMillis
                        showDate = false
                        showTime = true
                    },
                ) { Text("Далее") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs + ScheduleRules.MIN_DELAY_MS }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        DatePickerDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val day = pickedDay ?: nowMs
                        val fire = Calendar.getInstance().apply {
                            timeInMillis = day
                            set(Calendar.HOUR_OF_DAY, timeState.hour)
                            set(Calendar.MINUTE, timeState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        showTime = false
                        if (ScheduleRules.inWindow(System.currentTimeMillis(), fire)) {
                            onConfirm(fire, silent)
                        }
                    },
                ) { Text("Отложить") }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("Отмена") }
            },
        ) {
            TimePicker(state = timeState, modifier = Modifier.padding(16.dp))
        }
    }
}
