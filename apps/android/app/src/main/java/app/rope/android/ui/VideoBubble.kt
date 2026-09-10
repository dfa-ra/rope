package app.rope.android.ui

import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rope.android.RopeShapes
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageTime
import app.rope.android.data.PhotoLayout
import app.rope.android.data.VideoPipRules
import app.rope.android.media.VideoCodec

/**
 * Telegram-like in-thread video: poster + duration + play in the bubble.
 * Playback is hoisted so a floating mini-player can keep the clip when the
 * user leaves the thread. Not CallVideoRenderer.
 */
@Composable
fun VideoMessageBubble(
    m: ChatMessage,
    onEnsure: (ChatMessage) -> Unit,
    overlayMeta: Boolean = false,
    playing: Boolean = false,
    onToggle: (ChatMessage) -> Unit = {},
    onCompleted: () -> Unit = {},
) {
    LaunchedEffect(m.id, m.localPath) {
        onEnsure(m)
    }
    val extra = runCatching { MediaPayload.parse(m.extra) }.getOrNull()
    val duration = extra?.durationMs ?: 0L
    val path = m.localPath
    val poster = remember(path) { path?.let { VideoCodec.poster(it) } }
    val box = if (poster != null) {
        PhotoLayout.box(poster.width, poster.height)
    } else {
        PhotoLayout.Box(PhotoLayout.MAX_WIDTH_DP, PhotoLayout.MAX_WIDTH_DP * 9f / 16f)
    }
    val meta = MessageTime.meta(m.status, m.outgoing, m.timestampMs, edited = m.edited)
    Box(
        modifier = Modifier
            .width(box.widthDp.dp)
            .height(box.heightDp.dp)
            .clip(RoundedCornerShape(RopeShapes.media))
            .background(Color.Black),
    ) {
        if (playing && !path.isNullOrBlank()) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setOnCompletionListener { onCompleted() }
                    }
                },
                update = { view ->
                    if (view.tag != path) {
                        view.tag = path
                        view.setVideoPath(path)
                        view.start()
                    } else if (!view.isPlaying) {
                        view.start()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else if (poster != null) {
            Image(
                bitmap = poster.asImageBitmap(),
                contentDescription = "Видео",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!playing) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = !path.isNullOrBlank()) { onToggle(m) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Смотреть",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { onToggle(m) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Pause, contentDescription = "Пауза", tint = Color.White)
            }
        }
        Text(
            if (duration > 0L) MediaPayload.formatDuration(duration) else "видео",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (overlayMeta && meta.isNotBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (path.isNullOrBlank()) {
            Text(
                "Видео · загружается…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
fun VideoViewerSurface(path: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(path)
                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    start()
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun VideoPipCard(
    title: String,
    path: String?,
    playing: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onCompleted: () -> Unit = onStop,
    modifier: Modifier = Modifier,
) {
    val poster = remember(path) { path?.let { VideoCodec.poster(it) } }
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.width(200.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(Color.Black)
                    .clickable(onClick = onOpen),
            ) {
                if (playing && !path.isNullOrBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                setOnCompletionListener { onCompleted() }
                            }
                        },
                        update = { view ->
                            if (view.tag != path) {
                                view.tag = path
                                view.setVideoPath(path)
                                view.start()
                            } else if (!view.isPlaying) {
                                view.start()
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (poster != null) {
                    Image(
                        bitmap = poster.asImageBitmap(),
                        contentDescription = VideoPipRules.FALLBACK_TITLE,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (playing) VideoPipRules.PAUSE else VideoPipRules.PLAY,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        title.ifBlank { VideoPipRules.FALLBACK_TITLE },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        VideoPipRules.subtitle(playing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Outlined.Close, contentDescription = VideoPipRules.CLOSE)
                }
            }
        }
    }
}
