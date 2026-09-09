package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageTime
import app.rope.android.data.PeerProfileRules

@Composable
fun PeerProfilePane(
    state: UiState,
    onBack: () -> Unit,
    onOpenItem: (ChatMessage) -> Unit = {},
    onEnsureMedia: (ChatMessage) -> Unit = {},
) {
    val peer = state.peer
    val title = PeerProfileRules.title(peer?.displayName)
    val online = peer?.online == true
    val subtitle = MessageTime.lastSeenLabel(peer?.lastSeen.orEmpty(), online)
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
                "Профиль",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InitialsAvatar(title, group = false, online = online, size = 88.dp)
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SharedMediaHub(
            messages = state.messages,
            modifier = Modifier.weight(1f),
            onOpen = onOpenItem,
            onEnsureMedia = onEnsureMedia,
        )
    }
}
