package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.ScheduleRules
import app.rope.android.data.ScheduledSend

@Composable
fun ScheduledPane(
    state: UiState,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    onReschedule: (String, Long) -> Unit,
) {
    val now = System.currentTimeMillis()
    var editing by remember { mutableStateOf<ScheduledSend?>(null) }
    var pickTime by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
            }
            Text(
                ScheduleRules.LIST_TITLE,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.scheduled.isEmpty()) {
            RopeEmptyState(
                title = "Нет отложенных",
                body = "Удерживайте Отправить и выберите «Отложить».",
            )
        } else {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.scheduled, key = { it.id }) { row ->
                    SectionCard(onClick = { editing = row }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    ScheduleRules.listTimeLabel(now, row.fireAtMs),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    ScheduleRules.preview(row.kind, row.text),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (row.silent) {
                                Icon(
                                    Icons.Outlined.NotificationsOff,
                                    contentDescription = ScheduleRules.SILENT,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDelete(row.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { row ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(ScheduleRules.preview(row.kind, row.text)) },
            text = {
                Column {
                    Text(ScheduleRules.listTimeLabel(now, row.fireAtMs))
                    TextButton(onClick = { pickTime = true }) { Text("Изменить время") }
                }
            },
            confirmButton = {
                TextButton(onClick = { onDelete(row.id); editing = null }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Закрыть") }
            },
        )
    }
    if (pickTime && editing != null) {
        val row = editing!!
        ScheduleTimeSheet(
            onConfirm = { fire, _ ->
                onReschedule(row.id, fire)
                pickTime = false
                editing = null
            },
            onDismiss = { pickTime = false },
        )
    }
}

@Composable
fun ScheduledRowCard(
    count: Int,
    onClick: () -> Unit,
) {
    SectionCard(onClick = onClick) {
        Text(ScheduleRules.countLabel(count), style = MaterialTheme.typography.titleMedium)
        Text(
            "Сообщения с этого устройства",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
