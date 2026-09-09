package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rope.android.NavRules
import app.rope.android.UiState
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.NicknameRules
import app.rope.android.data.RevokeRules
import app.rope.android.data.RoleRules

@Composable
fun PeoplePane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onInvite: () -> Unit,
    onRevokeMember: (String) -> Unit = {},
    onSetNickname: (String, String) -> Unit = { _, _ -> },
) {
    val people = NavRules.peopleOf(state.devices, state.profile?.deviceId)
    val canRevoke = RevokeRules.canRevoke(state.profile?.role)
    var pendingMemberId by remember { mutableStateOf<String?>(null) }
    var nickEdit by remember { mutableStateOf<DirectoryDevice?>(null) }
    var nickDraft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        if (people.isEmpty()) {
            val role = state.profile?.role
            RopeEmptyState(
                title = "Пока никого нет",
                body = RoleRules.peopleEmptyHint(role),
                actionLabel = RoleRules.peopleInviteAction(role),
                onAction = if (RoleRules.canInvite(role)) onInvite else null,
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (canRevoke) {
                    item {
                        Text(
                            RevokeRules.peopleHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                itemsIndexed(people, key = { _, d -> d.deviceId }) { _, d ->
                    FadeIn(0) {
                        PersonRow(
                            d = d,
                            nick = state.nicks[d.deviceId],
                            showRevoke = RevokeRules.canRevokeTarget(
                                state.profile?.role,
                                state.profile?.memberId,
                                state.profile?.deviceId,
                                d,
                            ),
                            confirming = pendingMemberId == d.memberId,
                            onOpen = {
                                if (pendingMemberId != d.memberId) {
                                    onOpen(conversationOf(d, state.nicks[d.deviceId]))
                                }
                            },
                            onAskRevoke = { pendingMemberId = d.memberId },
                            onConfirmRevoke = {
                                onRevokeMember(d.memberId)
                                pendingMemberId = null
                            },
                            onCancelRevoke = { pendingMemberId = null },
                            onEditNick = {
                                nickEdit = d
                                nickDraft = state.nicks[d.deviceId].orEmpty()
                            },
                        )
                    }
                }
            }
        }
    }

    val editing = nickEdit
    if (editing != null) {
        AlertDialog(
            onDismissRequest = { nickEdit = null },
            title = { Text(NicknameRules.DIALOG_TITLE) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val orig = editing.displayName.trim()
                    if (orig.isNotEmpty()) {
                        Text("Сейчас: $orig", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = nickDraft,
                        onValueChange = { nickDraft = it.take(NicknameRules.MAX) },
                        singleLine = true,
                        label = { Text(NicknameRules.ACTION) },
                        supportingText = { Text("Пустое поле вернёт имя с сервера") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetNickname(editing.deviceId, nickDraft)
                        nickEdit = null
                    },
                ) {
                    Text(NicknameRules.SAVE)
                }
            },
            dismissButton = {
                Row {
                    if (NicknameRules.isCustom(state.nicks[editing.deviceId])) {
                        TextButton(
                            onClick = {
                                onSetNickname(editing.deviceId, "")
                                nickEdit = null
                            },
                        ) {
                            Text(NicknameRules.RESET)
                        }
                    }
                    TextButton(onClick = { nickEdit = null }) {
                        Text(NicknameRules.CANCEL)
                    }
                }
            },
        )
    }
}

@Composable
private fun PersonRow(
    d: DirectoryDevice,
    nick: String?,
    showRevoke: Boolean,
    confirming: Boolean,
    onOpen: () -> Unit,
    onAskRevoke: () -> Unit,
    onConfirmRevoke: () -> Unit,
    onCancelRevoke: () -> Unit,
    onEditNick: () -> Unit,
) {
    val shown = NicknameRules.display(nick, d.displayName, d.deviceId)
    val original = NicknameRules.originalLine(nick, d.displayName)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .pressScale(onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialsAvatar(shown.ifBlank { "?" }, group = false, online = d.online)
            Column(Modifier.weight(1f)) {
                Text(shown, style = MaterialTheme.typography.titleMedium)
                if (original != null && !confirming) {
                    Text(
                        original,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (confirming) RevokeRules.confirmPrompt(shown)
                    else if (d.online) "в сети" else "не в сети",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (confirming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (confirming) {
                    Text(
                        RevokeRules.confirmBody(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!confirming) {
            TextButton(
                onClick = onEditNick,
                modifier = Modifier.semantics { contentDescription = NicknameRules.ACTION },
            ) {
                Text(NicknameRules.ACTION)
            }
        }
        if (showRevoke) {
            if (confirming) {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = onConfirmRevoke,
                        modifier = Modifier.semantics { contentDescription = RevokeRules.confirmAction() },
                    ) {
                        Text(RevokeRules.confirmAction(), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onCancelRevoke) {
                        Text(RevokeRules.cancelAction())
                    }
                }
            } else {
                TextButton(
                    onClick = onAskRevoke,
                    modifier = Modifier.semantics { contentDescription = RevokeRules.actionLabel() },
                ) {
                    Text(RevokeRules.actionLabel(), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

internal fun conversationOf(d: DirectoryDevice, nick: String? = null): Conversation = Conversation(
    id = d.deviceId,
    title = NicknameRules.display(nick, d.displayName, d.deviceId),
    subtitle = if (d.online) "в сети" else "не в сети",
    isGroup = false,
    online = d.online,
    last = null,
    peer = d,
)
