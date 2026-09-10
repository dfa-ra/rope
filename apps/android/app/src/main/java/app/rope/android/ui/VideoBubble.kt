package app.rope.android.ui

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rope.android.RopeShapes
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageTime
import app.rope.android.data.PhotoLayout
import app.rope.android.data.VideoHoldRules
import app.rope.android.media.VideoCodec
import kotlinx.coroutines.withTimeoutOrNull

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
    var holding by remember(m.id) { mutableStateOf(false) }
    val player = remember(m.id) { arrayOfNulls<MediaPlayer>(1) }
    val speed = VideoHoldRules.speed(holding, playing)
    val box = if (poster != null) {
        PhotoLayout.box(poster.width, poster.height)
    } else {
        PhotoLayout.Box(PhotoLayout.MAX_WIDTH_DP, PhotoLayout.MAX_WIDTH_DP * 9f / 16f)
    }
    val meta = MessageTime.meta(m.status, m.outgoing, m.timestampMs, edited = m.edited)
    LaunchedEffect(speed, playing) {
        player[0]?.let { applyVideoHoldSpeed(it, speed) }
    }
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
                        setOnPreparedListener { mp ->
                            player[0] = mp
                            applyVideoHoldSpeed(mp, speed)
                        }
                        setOnCompletionListener {
                            playing = false
                            holding = false
                            player[0] = null
                        }
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
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(m.id) {
                        awaitVideoHold(onHolding = { holding = it })
                    },
            )
            DisposableEffect(m.id) {
                onDispose {
                    playing = false
                    holding = false
                    player[0] = null
                }
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
        VideoHoldChip(
            holding = holding,
            playing = playing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp),
        )
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
fun VideoViewerSurface(path: String, modifier: Modifier = Modifier, speed: Float = 1f) {
    val player = remember(path) { arrayOfNulls<MediaPlayer>(1) }
    LaunchedEffect(speed, path) {
        player[0]?.let { applyVideoHoldSpeed(it, speed) }
    }
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(path)
                setOnPreparedListener { mp ->
                    player[0] = mp
                    mp.isLooping = false
                    applyVideoHoldSpeed(mp, speed)
                    start()
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun VideoHoldChip(holding: Boolean, playing: Boolean = true, modifier: Modifier = Modifier) {
    if (!VideoHoldRules.chipVisible(holding, playing)) return
    Text(
        VideoHoldRules.LABEL,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

internal suspend fun PointerInputScope.awaitVideoHold(
    onHolding: (Boolean) -> Unit,
    onTap: (() -> Unit)? = null,
) {
    awaitEachGesture {
        awaitFirstDown()
        var held = false
        try {
            val up = withTimeoutOrNull(VideoHoldRules.HOLD_MS) { waitForUpOrCancellation() }
            if (up == null) {
                held = true
                onHolding(true)
                waitForUpOrCancellation()
            } else {
                onTap?.invoke()
            }
        } finally {
            if (held) onHolding(false)
        }
    }
}

internal fun applyVideoHoldSpeed(player: MediaPlayer, speed: Float) {
    try {
        val params = try {
            player.playbackParams
        } catch (_: Exception) {
            PlaybackParams()
        }
        player.playbackParams = params.setSpeed(speed)
    } catch (_: Exception) {
    }
}
