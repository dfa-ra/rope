package app.rope.android.ui

import android.media.MediaPlayer
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rope.android.RopeShapes
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageTime
import app.rope.android.data.PhotoLayout
import app.rope.android.data.VideoMuteRules
import app.rope.android.media.VideoCodec

/**
 * Telegram-like in-thread video: poster + duration + play in the bubble.
 * Tap/long-press on the bubble still opens the 0.3.2 menu; the play control
 * starts the in-thread player. Mute chip is independent of call video.
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
    var muted by remember(m.id) { mutableStateOf(false) }
    val player = remember(m.id) { arrayOfNulls<MediaPlayer>(1) }
    val box = if (poster != null) {
        PhotoLayout.box(poster.width, poster.height)
    } else {
        PhotoLayout.Box(PhotoLayout.MAX_WIDTH_DP, PhotoLayout.MAX_WIDTH_DP * 9f / 16f)
    }
    val meta = MessageTime.meta(m.status, m.outgoing, m.timestampMs, edited = m.edited)
    LaunchedEffect(muted, playing) {
        player[0]?.let { applyVideoMute(it, muted) }
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
                            applyVideoMute(mp, muted)
                        }
                        setOnCompletionListener {
                            playing = false
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
            DisposableEffect(m.id) {
                onDispose {
                    playing = false
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
        if (!path.isNullOrBlank()) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { muted = VideoMuteRules.toggle(muted) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                    contentDescription = VideoMuteRules.contentDescription(muted),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
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
    var muted by remember(path) { mutableStateOf(false) }
    val player = remember(path) { arrayOfNulls<MediaPlayer>(1) }
    LaunchedEffect(muted) {
        player[0]?.let { applyVideoMute(it, muted) }
    }
    Box(modifier) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(path)
                    setOnPreparedListener { mp ->
                        player[0] = mp
                        mp.isLooping = false
                        applyVideoMute(mp, muted)
                        start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { muted = VideoMuteRules.toggle(muted) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                contentDescription = VideoMuteRules.contentDescription(muted),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun applyVideoMute(player: MediaPlayer, muted: Boolean) {
    try {
        val v = VideoMuteRules.volume(muted)
        player.setVolume(v, v)
    } catch (_: Exception) {
    }
}
