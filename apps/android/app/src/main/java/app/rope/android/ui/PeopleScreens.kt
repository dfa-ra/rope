package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rope.android.NavRules
import app.rope.android.UiState
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.RevokeRules
import app.rope.android.data.RoleRules

@Composable
fun PeoplePane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onInvite: () -> Unit,
    onRevokeMember: (String) -> Unit = {},
) {
    val people = NavRules.peopleOf(state.devices, state.profile?.deviceId)
    val canRevoke = RevokeRules.canRevoke(state.profile?.role)
    var pendingMemberId by remember { mutableStateOf<String?>(null) }
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
                            showRevoke = RevokeRules.canRevokeTarget(
                                state.profile?.role,
                                state.profile?.memberId,
                                state.profile?.deviceId,
                                d,
                            ),
                            confirming = pendingMemberId == d.memberId,
                            onOpen = {
                                if (pendingMemberId != d.memberId) onOpen(conversationOf(d))
                            },
                            onAskRevoke = { pendingMemberId = d.memberId },
                            onConfirmRevoke = {
                                onRevokeMember(d.memberId)
                                pendingMemberId = null
                            },
                            onCancelRevoke = { pendingMemberId = null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonRow(
    d: DirectoryDevice,
    showRevoke: Boolean,
    confirming: Boolean,
    onOpen: () -> Unit,
    onAskRevoke: () -> Unit,
    onConfirmRevoke: () -> Unit,
    onCancelRevoke: () -> Unit,
) {
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
            InitialsAvatar(d.displayName.ifBlank { "?" }, group = false, online = d.online)
            Column(Modifier.weight(1f)) {
                Text(d.displayName.ifBlank { d.deviceId.take(8) }, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (confirming) RevokeRules.confirmPrompt(d.displayName.ifBlank { d.deviceId.take(8) })
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

internal fun conversationOf(d: DirectoryDevice): Conversation = Conversation(
    id = d.deviceId,
    title = d.displayName.ifBlank { d.deviceId.take(8) },
    subtitle = if (d.online) "в сети" else "не в сети",
    isGroup = false,
    online = d.online,
    last = null,
    peer = d,
)
