package app.rope.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.rope.android.RopeShapes
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.VoicePlayback

/**
 * Telegram-like voice bubble: play/pause, `0:03 / 0:12`, bar + bars that fill as it plays.
 * Isolated so chat-screen merges stay small.
 */
@Composable
fun VoiceMessageBubble(
    message: ChatMessage,
    playing: Boolean,
    positionMs: Long,
    playerDurationMs: Long,
    onPlay: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = runCatching { MediaPayload.parse(message.extra) }.getOrNull()
    val total = VoicePlayback.resolvedDuration(playerDurationMs, extra?.durationMs ?: 0L)
    val pos = VoicePlayback.displayPosition(playing || positionMs > 0L, positionMs)
    val fraction = VoicePlayback.fraction(pos, total)
    val animated by animateFloatAsState(fraction, tween(80), label = "voiceFrac")
    val sending = VoicePlayback.isSending(message.outgoing, message.status)
    val downloading = extra != null && message.localPath == null
    val bars = remember(message.id) { VoicePlayback.waveform(message.id) }
    val fill = MaterialTheme.colorScheme.onSurface
    val track = fill.copy(alpha = 0.22f)
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = { onPlay(message) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (playing) "Пауза" else "Голос",
                )
            }
            if (sending || downloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            VoiceWaveform(bars, animated, fill, track)
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(RopeShapes.media))
                    .background(track),
            ) {
                if (animated > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(animated.coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .background(fill.copy(alpha = 0.85f)),
                    )
                }
            }
            Text(
                when {
                    downloading -> "скачивается…"
                    extra != null -> {
                        val clock = VoicePlayback.clock(pos, total)
                        if (sending) "$clock · отправка…" else clock
                    }
                    else -> message.text
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun VoiceWaveform(bars: List<Float>, fraction: Float, fill: Color, track: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        bars.forEachIndexed { index, heightFrac ->
            val lit = fraction > 0f && index <= ((bars.lastIndex) * fraction).toInt()
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFrac.coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (lit) fill else track),
            )
        }
    }
}
