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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.rope.android.NavRules
import app.rope.android.UiState
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.RoleRules

@Composable
fun PeoplePane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onInvite: () -> Unit,
) {
    val people = NavRules.peopleOf(state.devices, state.profile?.deviceId)
    Column(Modifier.fillMaxSize()) {
        FadeIn(40) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Люди", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Контакты с вашего сервера. Тап — открыть личный чат.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                itemsIndexed(people, key = { _, d -> d.deviceId }) { index, d ->
                    FadeIn(80 + SplashTiming.staggerDelayMs(index)) {
                        PersonRow(d) { onOpen(conversationOf(d)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonRow(d: DirectoryDevice, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(d.displayName.ifBlank { "?" }, group = false, online = d.online)
        Column(Modifier.weight(1f)) {
            Text(d.displayName.ifBlank { d.deviceId.take(8) }, style = MaterialTheme.typography.titleMedium)
            Text(
                if (d.online) "в сети" else "не в сети",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
