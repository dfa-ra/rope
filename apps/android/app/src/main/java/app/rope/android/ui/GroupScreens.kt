package app.rope.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rope.android.RopeShapes
import app.rope.android.UiState
import app.rope.android.data.GroupChatUx
import app.rope.android.data.GroupNameRules
import app.rope.android.data.GroupPhotoRules
import app.rope.android.data.RoleRules

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
                        RoleRules.groupNoMembersHint(state.profile?.role),
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
                enabled = GroupNameRules.parse(state.groupNameDraft) != null,
            )
        }
    }
}

@Composable
fun GroupInfoPane(
    state: UiState,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onLeave: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onBack: () -> Unit,
) {
    val g = state.group
    if (g == null) {
        RopeEmptyState(
            title = "Нет группы",
            body = "Системная «назад» вернёт в чат.",
        )
        return
    }
    val names = state.devices.associate { it.deviceId to it.displayName }
    val me = state.profile?.deviceId
    val organizer = GroupChatUx.organizerId(g)
    val isMember = me != null && me in g.members
    val canManage = RoleRules.canManageGroupMembers(isMember, me, organizer, state.profile?.role)
    val canRename = RoleRules.canRenameGroup(isMember, me, organizer, state.profile?.role)
    val canPhoto = GroupPhotoRules.canSet(isMember, me, organizer, state.profile?.role)
    val photoPath = GroupPhotoRules.lookup(state.groupPhotos, g.groupId)
    val canLeave = RoleRules.canLeaveGroup(isMember)
    var confirmLeave by remember { mutableStateOf(false) }
    var renameDraft by remember(g.groupId, g.name) { mutableStateOf(g.name) }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .then(
                                        if (canPhoto) Modifier.clickable(onClick = onPickPhoto) else Modifier,
                                    )
                                    .semantics { contentDescription = GroupPhotoRules.TITLE },
                            ) {
                                InitialsAvatar(
                                    title = g.name,
                                    group = true,
                                    online = g.members.any { it in state.onlineIds && it != me },
                                    size = 72.dp,
                                    photoPath = photoPath,
                                    showPresence = false,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(g.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "${g.members.size} участников",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (canPhoto) {
                            TextButton(
                                onClick = onPickPhoto,
                                modifier = Modifier.semantics { contentDescription = GroupPhotoRules.TITLE },
                            ) { Text(GroupPhotoRules.TITLE) }
                            Text(
                                GroupPhotoRules.HINT,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (canRename) {
                            OutlinedTextField(
                                renameDraft,
                                { renameDraft = it },
                                label = { Text("Название") },
                                singleLine = true,
                                shape = RoundedCornerShape(RopeShapes.field),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                            TextButton(
                                onClick = { onRename(renameDraft) },
                                enabled = GroupNameRules.parse(renameDraft) != null &&
                                    GroupNameRules.parse(renameDraft) != g.name,
                            ) { Text("Сохранить название") }
                        }
                    }
                }
            }
            item { Text("Участники", style = MaterialTheme.typography.titleSmall) }
            items(g.members, key = { it }) { id ->
                val label = GroupChatUx.memberDisplayName(id, me, names)
                val role = GroupChatUx.memberRoleLabel(id, g)
                val tint = Color(GroupChatUx.senderColorArgb(id, label))
                val online = id in state.onlineIds || (id == me && !state.offline)
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InitialsAvatar(
                            title = label,
                            group = false,
                            online = online,
                            size = 40.dp,
                            tint = tint,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                role,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (canManage && id != me) {
                            TextButton(onClick = { onRemove(id) }) { Text("Убрать") }
                        }
                    }
                }
            }
            if (canManage) {
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
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canLeave) {
                if (confirmLeave) {
                    GlowButton("Точно выйти из группы", onLeave, Modifier.fillMaxWidth())
                    TextButton(onClick = { confirmLeave = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Отмена")
                    }
                } else {
                    QuietButton("Выйти из группы", { confirmLeave = true }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
