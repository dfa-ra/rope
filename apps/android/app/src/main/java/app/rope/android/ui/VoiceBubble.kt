package app.rope.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaPayload
import app.rope.android.data.VoiceHoldRules
import app.rope.android.data.VoicePlayback
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Telegram-like voice bubble: play/pause, seekable waveform, 1x/1.5x/2x.
 */
@Composable
fun VoiceMessageBubble(
    message: ChatMessage,
    playing: Boolean,
    positionMs: Long,
    playerDurationMs: Long,
    speed: Float,
    onPlay: (ChatMessage) -> Unit,
    onSeek: (ChatMessage, Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onHold: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val extra = runCatching { MediaPayload.parse(message.extra) }.getOrNull()
    val total = VoicePlayback.resolvedDuration(playerDurationMs, extra?.durationMs ?: 0L)
    val pos = VoicePlayback.displayPosition(playing || positionMs > 0L, positionMs)
    val fraction = VoicePlayback.fraction(pos, total)
    val animated by animateFloatAsState(fraction, tween(80), label = "voiceFrac")
    val sending = VoicePlayback.isSending(message.outgoing, message.status)
    val downloading = extra != null && message.localPath == null
    var holding by remember(message.id) { mutableStateOf(false) }
    val shownSpeed = VoiceHoldRules.speed(holding, playing, speed)
    val bars = remember(message.id, extra?.waveform) {
        VoicePlayback.resolveBars(extra?.waveform.orEmpty(), message.id)
    }
    val fill = MaterialTheme.colorScheme.onSurface
    val track = fill.copy(alpha = 0.22f)
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .pointerInput(playing, message.id) {
                    awaitEachGesture {
                        awaitFirstDown()
                        var held = false
                        try {
                            val up = withTimeoutOrNull(VoiceHoldRules.HOLD_MS) { waitForUpOrCancellation() }
                            if (up == null && playing) {
                                held = true
                                holding = true
                                onHold(true)
                                waitForUpOrCancellation()
                            } else if (up != null) {
                                onPlay(message)
                            }
                        } finally {
                            if (held) {
                                holding = false
                                onHold(false)
                            }
                        }
                    }
                },
        ) {
            Icon(
                if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (playing) "Пауза" else "Голос",
            )
            if (sending || downloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            VoiceWaveform(
                bars = bars,
                fraction = animated,
                fill = fill,
                track = track,
                enabled = extra != null && !downloading && total > 0L,
                onSeekFraction = { frac -> onSeek(message, VoicePlayback.seekMs(frac, total)) },
            )
            Row(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
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
                if (extra != null && !downloading) {
                    Text(
                        if (VoiceHoldRules.chipVisible(holding, playing)) {
                            VoiceHoldRules.LABEL
                        } else {
                            VoicePlayback.speedLabel(shownSpeed)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCycleSpeed)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveform(
    bars: List<Float>,
    fraction: Float,
    fill: Color,
    track: Color,
    enabled: Boolean,
    onSeekFraction: (Float) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                fun frac(x: Float): Float {
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    return (x / w).coerceIn(0f, 1f)
                }
                detectTapGestures { offset -> onSeekFraction(frac(offset.x)) }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                fun frac(x: Float): Float {
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    return (x / w).coerceIn(0f, 1f)
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onSeekFraction(frac(offset.x)) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeekFraction(frac(change.position.x))
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val litUntil = VoicePlayback.litBarIndex(fraction, bars.size)
        bars.forEachIndexed { index, heightFrac ->
            val lit = index <= litUntil
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
