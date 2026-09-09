package app.rope.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageTime
import app.rope.android.data.PeerProfileRules
import app.rope.android.media.ImageCodec

@Composable
fun PeerProfilePane(
    state: UiState,
    onBack: () -> Unit,
    onOpenImage: (ChatMessage) -> Unit = {},
    onEnsureMedia: (ChatMessage) -> Unit = {},
    onSetTtl: (Int) -> Unit = {},
) {
    val peer = state.peer
    val title = PeerProfileRules.title(peer?.displayName)
    val online = peer?.online == true
    val subtitle = MessageTime.lastSeenLabel(peer?.lastSeen.orEmpty(), online)
    val photos = PeerProfileRules.photos(state.messages)
    var showTtl by remember { mutableStateOf(false) }
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(PeerProfileRules.GRID_COLUMNS),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { contentDescription = PeerProfileRules.SECTION },
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(span = { GridItemSpan(PeerProfileRules.GRID_COLUMNS) }) {
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
            }
            item(span = { GridItemSpan(PeerProfileRules.GRID_COLUMNS) }) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    DisappearSettingsRow(
                        ttlSec = state.ttlSec,
                        canSet = true,
                        onOpen = { showTtl = true },
                    )
                }
            }
            item(span = { GridItemSpan(PeerProfileRules.GRID_COLUMNS) }) {
                Text(
                    PeerProfileRules.sectionLabel(photos.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (photos.isEmpty()) {
                item(span = { GridItemSpan(PeerProfileRules.GRID_COLUMNS) }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(PeerProfileRules.emptyTitle(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            PeerProfileRules.emptyBody(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(photos, key = { it.id }) { m ->
                    SharedPhotoTile(m, onEnsureMedia) { onOpenImage(m) }
                }
            }
        }
        if (showTtl) {
            DisappearSheet(
                ttlSec = state.ttlSec,
                canSet = true,
                onSelect = {
                    onSetTtl(it)
                    showTtl = false
                },
                onDismiss = { showTtl = false },
            )
        }
    }
}

@Composable
private fun SharedPhotoTile(
    m: ChatMessage,
    onEnsure: (ChatMessage) -> Unit,
    onClick: () -> Unit,
) {
    LaunchedEffect(m.id, m.localPath) { onEnsure(m) }
    val bmp = m.localPath?.let { runCatching { ImageCodec.decodePreview(it) }.getOrNull() }
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Фото" },
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Фото",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
