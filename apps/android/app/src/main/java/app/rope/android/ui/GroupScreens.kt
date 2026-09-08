package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.rope.android.RopeShapes
import app.rope.android.UiState

@Composable
fun NewGroupPane(
    state: UiState,
    onName: (String) -> Unit,
    onToggle: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FadeIn(40) { Text("Новая группа", style = MaterialTheme.typography.titleLarge) }
            FadeIn(100) {
                Text(
                    "Сервер знает только состав. Текст шифруется каждому участнику отдельно.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                state.groupNameDraft,
                onName,
                label = { Text("Название") },
                singleLine = true,
                shape = RoundedCornerShape(RopeShapes.field),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Участники", style = MaterialTheme.typography.titleSmall)
            if (state.devices.isEmpty()) {
                SectionCard {
                    Text(
                        "Пока некого добавить. Пригласите человека QR-кодом.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                state.devices.forEachIndexed { index, d ->
                    FadeIn(140 + index * 40) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressScale { onToggle(d.deviceId) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = d.deviceId in state.pickedMembers, onCheckedChange = { onToggle(d.deviceId) })
                            Column {
                                Text(d.displayName)
                                Text(if (d.online) "в сети" else "не в сети", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton(
                "Создать",
                onCreate,
                Modifier.fillMaxWidth(),
                enabled = state.groupNameDraft.isNotBlank(),
            )
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Назад") }
        }
    }
}

@Composable
fun GroupInfoPane(
    state: UiState,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
) {
    val g = state.group
    if (g == null) {
        RopeEmptyState(
            title = "Нет группы",
            body = "Вернитесь в чат и откройте группу ещё раз.",
            actionLabel = "Назад в чат",
            onAction = onBack,
        )
        return
    }
    val names = state.devices.associate { it.deviceId to it.displayName }
    val me = state.profile?.deviceId
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FadeIn(40) {
                    SectionCard {
                        Text(g.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "эпоха ${g.epoch} · ${g.members.size} участников",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(g.members, key = { it }) { id ->
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (id == me) "Вы" else names[id] ?: id.take(8))
                        if (id != me) {
                            TextButton(onClick = { onRemove(id) }) { Text("Убрать") }
                        }
                    }
                }
            }
            item { Text("Добавить", style = MaterialTheme.typography.titleSmall) }
            val extras = state.devices.filter { it.deviceId !in g.members }
            if (extras.isEmpty()) {
                item {
                    Text(
                        "Все знакомые уже в группе.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(extras, key = { it.deviceId }) { d ->
                    SectionCard(onClick = { onAdd(d.deviceId) }) {
                        Text("+ ${d.displayName}", style = MaterialTheme.typography.titleMedium)
                        Text(if (d.online) "в сети" else "не в сети", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        QuietButton("Назад в чат", onBack, Modifier.padding(16.dp).fillMaxWidth())
    }
}
