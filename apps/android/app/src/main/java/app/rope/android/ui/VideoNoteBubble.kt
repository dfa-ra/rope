package app.rope.android.ui

import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.VideoNoteRules
import app.rope.android.media.VideoCodec

/**
 * Telegram-like кружок: looping circular clip, tap to pause.
 */
@Composable
fun VideoNoteBubble(
    m: ChatMessage,
    onEnsure: (ChatMessage) -> Unit,
) {
    LaunchedEffect(m.id, m.localPath) { onEnsure(m) }
    val extra = runCatching { MediaPayload.parse(m.extra) }.getOrNull()
    val duration = extra?.durationMs ?: 0L
    val path = m.localPath
    val poster = remember(path) { path?.let { VideoCodec.poster(it) } }
    var playing by remember(m.id) { mutableStateOf(false) }
    val size = VideoNoteRules.DISPLAY_DP.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black)
            .clickable(enabled = !path.isNullOrBlank()) { playing = !playing },
        contentAlignment = Alignment.Center,
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
                            mp.isLooping = true
                            start()
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
                onDispose { playing = false }
            }
        } else if (poster != null) {
            Image(
                bitmap = poster.asImageBitmap(),
                contentDescription = "Видеосообщение",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!playing) {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = "Смотреть кружок",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        } else {
            Icon(
                Icons.Outlined.Pause,
                contentDescription = "Пауза",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
            )
        }
        Text(
            if (duration > 0L) MediaPayload.formatDuration(duration) else "кружок",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (path.isNullOrBlank()) {
            Text(
                "загружается…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
            )
        }
    }
}

@Composable
fun VideoNoteRecorderOverlay(
    recordMs: Long,
    onPreviewReady: (SurfaceHolder, Int) -> Unit,
    onPreviewGone: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
    ) {
        Box(
            Modifier
                .size(VideoNoteRules.DISPLAY_DP.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { ctx ->
                    val surface = SurfaceView(ctx)
                    val rotation = ctx.resources.configuration.orientation
                    val displayDeg = try {
                        @Suppress("DEPRECATION")
                        (ctx.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager)
                            .defaultDisplay.rotation.let { r ->
                                when (r) {
                                    android.view.Surface.ROTATION_90 -> 90
                                    android.view.Surface.ROTATION_180 -> 180
                                    android.view.Surface.ROTATION_270 -> 270
                                    else -> 0
                                }
                            }
                    } catch (_: Exception) {
                        if (rotation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 90 else 0
                    }
                    surface.holder.addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                onPreviewReady(holder, displayDeg)
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) {
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                onPreviewGone()
                            }
                        },
                    )
                    FrameLayout(ctx).apply {
                        addView(
                            surface,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Gravity.CENTER,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            MediaPayload.formatDuration(recordMs),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = (VideoNoteRules.DISPLAY_DP / 2 + 28).dp),
        )
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Отменить кружок", tint = Color.White)
        }
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Outlined.Send, contentDescription = "Отправить кружок", tint = Color.White)
        }
    }
}
