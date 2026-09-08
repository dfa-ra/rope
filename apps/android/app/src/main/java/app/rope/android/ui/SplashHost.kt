package app.rope.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import app.rope.android.UiState

data class SplashOverlay(
    val visible: Boolean,
    val caption: String?,
    val loop: Boolean,
    val compact: Boolean,
)

@Composable
fun rememberSplashOverlay(state: UiState): SplashOverlay {
    val reduce = rememberReduceMotion()
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var busyElapsedMs by remember { mutableLongStateOf(0L) }
    var seen by remember { mutableStateOf(SplashSession.seenThisProcess) }
    var sessionReady by remember { mutableStateOf(state.sessionReady) }
    var busy by remember { mutableStateOf(state.busy) }
    sessionReady = state.sessionReady
    busy = state.busy

    LaunchedEffect(reduce) {
        if (seen) return@LaunchedEffect
        val startNs = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            elapsedMs = ((now - startNs) / 1_000_000L).coerceAtLeast(0L)
            val hold = SplashTiming.shouldHoldColdStart(
                elapsedMs = elapsedMs,
                sessionReady = sessionReady,
                seenThisProcess = seen,
                reduceMotion = reduce,
            )
            if (!hold) {
                SplashSession.seenThisProcess = true
                seen = true
                break
            }
        }
    }

    LaunchedEffect(busy) {
        if (!busy) {
            busyElapsedMs = 0L
            return@LaunchedEffect
        }
        val startNs = withFrameNanos { it }
        while (busy) {
            val now = withFrameNanos { it }
            busyElapsedMs = ((now - startNs) / 1_000_000L).coerceAtLeast(0L)
        }
    }

    val cold = SplashTiming.shouldHoldColdStart(
        elapsedMs = elapsedMs,
        sessionReady = sessionReady,
        seenThisProcess = seen,
        reduceMotion = reduce,
    )
    val longLoad = SplashTiming.shouldShowLongLoad(
        busyElapsedMs = busyElapsedMs,
        busy = busy,
        hasError = !state.error.isNullOrBlank(),
        callActive = state.call != null,
        reduceMotion = reduce,
    )
    val visible = cold || longLoad
    val caption = when {
        longLoad -> SplashTiming.busyCaption(state.screen, state.profile != null)
        else -> null
    }
    return SplashOverlay(
        visible = visible,
        caption = caption,
        loop = longLoad || (cold && elapsedMs > SplashTiming.PAINT_MS),
        compact = longLoad && !cold,
    )
}
