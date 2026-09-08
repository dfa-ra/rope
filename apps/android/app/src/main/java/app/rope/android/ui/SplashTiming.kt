package app.rope.android.ui

import app.rope.android.Screen

/** Pure timing / visibility rules for the paint-fill splash and in-app motion. */
object SplashTiming {
    const val MIN_HOLD_MS = 1_400L
    const val MAX_HOLD_MS = 8_000L
    const val PAINT_MS = 1_600L
    const val LONG_LOAD_AFTER_MS = 450L
    const val REDUCE_MOTION_MAX_MS = 2_400L
    const val STAGGER_STEP_MS = 36
    const val STAGGER_CAP_MS = 280

    fun isReduceMotion(animatorDurationScale: Float, transitionAnimationScale: Float = 1f): Boolean =
        animatorDurationScale <= 0f || transitionAnimationScale <= 0f

    fun minHoldMs(reduceMotion: Boolean): Long = if (reduceMotion) 0L else MIN_HOLD_MS

    fun maxHoldMs(reduceMotion: Boolean): Long = if (reduceMotion) REDUCE_MOTION_MAX_MS else MAX_HOLD_MS

    /** Cold-start overlay: until session is ready and the paint has had time, but never forever. */
    fun shouldHoldColdStart(
        elapsedMs: Long,
        sessionReady: Boolean,
        seenThisProcess: Boolean,
        reduceMotion: Boolean = false,
    ): Boolean {
        if (seenThisProcess) return false
        val max = maxHoldMs(reduceMotion)
        if (elapsedMs >= max) return false
        if (!sessionReady) return true
        return elapsedMs < minHoldMs(reduceMotion)
    }

    fun shouldShowLongLoad(
        busyElapsedMs: Long,
        busy: Boolean,
        hasError: Boolean,
        callActive: Boolean,
        reduceMotion: Boolean = false,
    ): Boolean {
        if (!busy || hasError || callActive || reduceMotion) return false
        return busyElapsedMs >= LONG_LOAD_AFTER_MS
    }

    fun busyCaption(screen: Screen, hasProfile: Boolean): String = when {
        screen == Screen.Provision -> "Устанавливаем сервер…"
        screen == Screen.Join -> "Входим…"
        screen == Screen.Status -> "Обновление…"
        !hasProfile -> "Подключаемся…"
        else -> "Подождите…"
    }

    fun staggerDelayMs(index: Int, stepMs: Int = STAGGER_STEP_MS, capMs: Int = STAGGER_CAP_MS): Int =
        (index.coerceAtLeast(0) * stepMs).coerceAtMost(capMs)

    fun paintProgress(elapsedMs: Long, durationMs: Long = PAINT_MS, reduceMotion: Boolean = false): Float {
        if (reduceMotion || durationMs <= 0L) return 1f
        val t = (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t)
    }
}

/** Process-scoped gate so in-session navigation does not replay the cold splash. */
object SplashSession {
    @Volatile
    var seenThisProcess: Boolean = false
}
