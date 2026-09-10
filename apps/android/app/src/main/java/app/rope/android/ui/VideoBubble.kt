package app.rope.android.ui

import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rope.android.RopeShapes
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageTime
import app.rope.android.data.PhotoLayout
import app.rope.android.data.VideoSeekRules
import app.rope.android.media.VideoCodec
import kotlinx.coroutines.delay

/**
 * Telegram-like in-thread video: poster + duration + play in the bubble.
 * Tap/long-press on the bubble still opens the 0.3.2 menu; the play control
 * starts the in-thread player.
 */
@Composable
fun VideoMessageBubble(
    m: ChatMessage,
    onEnsure: (ChatMessage) -> Unit,
    overlayMeta: Boolean = false,
) {
    LaunchedEffect(m.id, m.localPath) {
        onEnsure(m)
    }
    val extra = runCatching { MediaPayload.parse(m.extra) }.getOrNull()
    val duration = extra?.durationMs ?: 0L
    val path = m.localPath
    val poster = remember(path) { path?.let { VideoCodec.poster(it) } }
    var playing by remember(m.id) { mutableStateOf(false) }
    var player by remember { mutableStateOf<VideoView?>(null) }
    var positionMs by remember(m.id) { mutableLongStateOf(0L) }
    var durationMs by remember(m.id) { mutableLongStateOf(duration) }
    var scrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(playing, player) {
        while (playing) {
            val view = player
            if (view != null && !scrubbing) {
                positionMs = view.currentPosition.toLong()
                if (view.duration > 0) durationMs = view.duration.toLong()
            }
            delay(200)
        }
    }
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
                        setOnCompletionListener { playing = false }
                    }
                },
                update = { view ->
                    player = view
                    if (view.tag != path) {
                        view.tag = path
                        view.setVideoPath(path)
                        view.start()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            DisposableEffect(m.id) {
                onDispose { playing = false }
            }
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
                    .clickable(enabled = !path.isNullOrBlank()) { playing = true },
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
                    .clickable { playing = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Pause, contentDescription = "Пауза", tint = Color.White)
            }
            if (VideoSeekRules.canScrub(true, durationMs)) {
                VideoScrubBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    onSeek = { ms ->
                        positionMs = ms
                        player?.seekTo(ms.toInt())
                    },
                    onScrubbing = { scrubbing = it },
                )
            }
        }
        if (!playing) {
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
        }
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
private fun VideoScrubBar(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit,
    onScrubbing: (Boolean) -> Unit,
) {
    val fraction = VideoSeekRules.progress(positionMs, durationMs)
    Box(
        modifier
            .height(18.dp)
            .pointerInput(durationMs) {
                fun seek(x: Float) {
                    onSeek(VideoSeekRules.fromPointerX(x, size.width.toFloat(), durationMs))
                }
                detectTapGestures { offset -> seek(offset.x) }
            }
            .pointerInput(durationMs) {
                fun seek(x: Float) {
                    onSeek(VideoSeekRules.fromPointerX(x, size.width.toFloat(), durationMs))
                }
                detectHorizontalDragGestures(
                    onDragStart = {
                        onScrubbing(true)
                        seek(it.x)
                    },
                    onDragEnd = { onScrubbing(false) },
                    onDragCancel = { onScrubbing(false) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        seek(change.position.x)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.35f)),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White),
        )
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
