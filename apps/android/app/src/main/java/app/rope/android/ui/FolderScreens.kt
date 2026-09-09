package app.rope.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.Conversation
import app.rope.android.data.CustomFolder
import app.rope.android.data.FolderMembership
import app.rope.android.data.FolderRules
import app.rope.android.data.FolderSnap

@Composable
fun FolderEditPane(
    state: UiState,
    onShowUnread: (Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onCycleChat: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    val snap = state.folders
    var newName by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Встроенные", style = MaterialTheme.typography.titleMedium)
        SectionCard {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(FolderRules.UNREAD_TITLE, style = MaterialTheme.typography.titleSmall)
                    Text("Показывать вкладку", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = FolderRules.UNREAD_ID !in snap.hiddenBuiltins,
                    onCheckedChange = onShowUnread,
                )
            }
        }
        Text("Ваши папки", style = MaterialTheme.typography.titleMedium)
        snap.custom.sortedBy { it.sort }.forEach { folder ->
            SectionCard {
                FolderCard(
                    folder = folder,
                    conversations = state.conversations,
                    expanded = editingId == folder.id,
                    renameDraft = if (editingId == folder.id) renameDraft else folder.name,
                    onExpand = {
                        editingId = if (editingId == folder.id) null else folder.id
                        renameDraft = folder.name
                    },
                    onRenameDraft = { renameDraft = it },
                    onSaveName = { onRename(folder.id, renameDraft) },
                    onDelete = { onDelete(folder.id); if (editingId == folder.id) editingId = null },
                    onMoveUp = { onMove(folder.id, -1) },
                    onMoveDown = { onMove(folder.id, 1) },
                    onCycleChat = { onCycleChat(folder.id, it) },
                )
            }
        }
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it.take(FolderRules.NAME_MAX) },
            label = { Text("Новая папка") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                onCreate(newName)
                newName = ""
            },
            enabled = FolderRules.normalizeName(newName) != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Создать")
        }
        Text(
            FolderRules.DEVICE_ONLY,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onBack) { Text("Назад") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FolderCard(
    folder: CustomFolder,
    conversations: List<Conversation>,
    expanded: Boolean,
    renameDraft: String,
    onExpand: () -> Unit,
    onRenameDraft: (String) -> Unit,
    onSaveName: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCycleChat: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                folder.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).clickable(onClick = onExpand),
            )
            TextButton(onClick = onMoveUp) { Text("↑") }
            TextButton(onClick = onMoveDown) { Text("↓") }
            TextButton(onClick = onDelete) { Text(FolderRules.DELETE) }
        }
        if (expanded) {
            OutlinedTextField(
                value = renameDraft,
                onValueChange = { onRenameDraft(it.take(FolderRules.NAME_MAX)) },
                label = { Text(FolderRules.RENAME) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onSaveName) { Text("Сохранить имя") }
            Text("Чаты", style = MaterialTheme.typography.labelMedium)
            val known = conversations.associateBy { it.id }
            val extraIds = (folder.include + folder.exclude).filter { it !in known }
            val rows = conversations + extraIds.map { id ->
                Conversation(id, "чат недоступен", "", false, false, null)
            }
            rows.forEach { c ->
                val member = FolderRules.membership(folder, c.id)
                val label = when (member) {
                    FolderMembership.INCLUDE -> "Включено"
                    FolderMembership.EXCLUDE -> "Исключено"
                    FolderMembership.SKIP -> "Пропуск"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCycleChat(c.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.title.ifBlank { c.id }, modifier = Modifier.weight(1f))
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun FolderPickDialog(
    conversation: Conversation,
    folders: FolderSnap,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember(conversation.id, folders) {
        mutableStateOf(FolderRules.foldersContaining(folders, conversation.id))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(FolderRules.ADD_TO_FOLDER) },
        text = {
            Column {
                if (folders.custom.isEmpty()) {
                    Text("Сначала создайте папку через ✎.")
                } else {
                    folders.custom.sortedBy { it.sort }.forEach { folder ->
                        val checked = folder.id in picked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    picked = if (checked) picked - folder.id else picked + folder.id
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    picked = if (it) picked + folder.id else picked - folder.id
                                },
                            )
                            Text(folder.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }) { Text("Готово") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
