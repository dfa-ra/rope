package app.rope.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import android.graphics.Bitmap
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaHubRules
import app.rope.android.data.MediaHubTab
import app.rope.android.data.MessageKind
import app.rope.android.media.ImageCodec
import app.rope.android.media.VideoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SharedMediaHub(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    onOpen: (ChatMessage) -> Unit,
    onEnsureMedia: (ChatMessage) -> Unit = {},
) {
    var tab by remember { mutableStateOf(MediaHubTab.MEDIA) }
    val rows = MediaHubRules.items(messages, tab)
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MediaHubTab.values().forEach { t ->
                val count = MediaHubRules.items(messages, t).size
                FilterChip(
                    selected = tab == t,
                    onClick = { tab = t },
                    label = { Text(MediaHubRules.tabCaption(t, count)) },
                )
            }
        }
        if (tab == MediaHubTab.MEDIA) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(MediaHubRules.GRID_COLUMNS),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { contentDescription = MediaHubRules.SECTION },
            ) {
                if (rows.isEmpty()) {
                    item(span = { GridItemSpan(MediaHubRules.GRID_COLUMNS) }) {
                        HubEmpty(tab)
                    }
                } else {
                    items(rows, key = { it.id }) { m ->
                        HubMediaTile(m, onEnsureMedia) { onOpen(m) }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { contentDescription = MediaHubRules.SECTION },
            ) {
                if (rows.isEmpty()) {
                    item { HubEmpty(tab) }
                } else {
                    items(rows, key = { it.id }) { m ->
                        HubListRow(tab, m) { onOpen(m) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubEmpty(tab: MediaHubTab) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(MediaHubRules.emptyTitle(tab), style = MaterialTheme.typography.titleMedium)
        Text(
            MediaHubRules.emptyBody(tab),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HubMediaTile(
    m: ChatMessage,
    onEnsure: (ChatMessage) -> Unit,
    onClick: () -> Unit,
) {
    var bmp by remember(m.id, m.localPath, m.kind) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(m.id, m.localPath, m.kind) {
        onEnsure(m)
        val path = m.localPath
        if (path.isNullOrBlank()) {
            bmp = null
            return@LaunchedEffect
        }
        bmp = withContext(Dispatchers.Default) {
            runCatching { hubTileBitmap(path, m.kind) }.getOrNull()
        }
    }
    val video = MediaHubRules.usesVideoPoster(m.kind)
    val poster = bmp
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (video) "Видео" else "Фото" },
        contentAlignment = Alignment.Center,
    ) {
        if (poster != null) {
            Image(
                bitmap = poster.asImageBitmap(),
                contentDescription = if (video) "Видео" else "Фото",
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
        if (video) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun hubTileBitmap(path: String, kind: MessageKind): Bitmap? {
    if (MediaHubRules.usesVideoPoster(kind)) {
        val frame = VideoCodec.poster(path) ?: return null
        return ImageCodec.scale(frame, MediaHubRules.TILE_EDGE)
    }
    return ImageCodec.decodePreview(path)
}

@Composable
private fun HubListRow(tab: MediaHubTab, m: ChatMessage, onClick: () -> Unit) {
    val icon = when (tab) {
        MediaHubTab.FILES -> Icons.AutoMirrored.Outlined.InsertDriveFile
        MediaHubTab.LINKS -> Icons.Outlined.Link
        else -> Icons.Outlined.Mic
    }
    val title = when (tab) {
        MediaHubTab.FILES -> MediaHubRules.fileLabel(m)
        MediaHubTab.LINKS -> MediaHubRules.linkLabel(m)
        else -> MediaHubRules.voiceLabel(m)
    }
    SectionCard(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
