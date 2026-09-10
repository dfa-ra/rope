package app.rope.android.data

/**
 * Telegram-like pinch-zoom in the full-screen photo viewer.
 * Scale 1..4, pan only while zoomed. Horizontal pager and tap-to-close stay
 * at rest (scale 1). Videos stay pager + tap-to-close. No CallVideoRenderer.
 * LocalStore stays v6.
 */
object PhotoZoomRules {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 4f
    const val DOUBLE_TAP_SCALE = 2.5f
    const val TAP_SLOP_DP = 12f
    const val ZOOMED_EPS = 0.02f

    fun canZoom(kind: MessageKind): Boolean = kind == MessageKind.IMAGE

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    fun isZoomed(scale: Float): Boolean = scale > MIN_SCALE + ZOOMED_EPS

    fun doubleTapScale(current: Float): Float =
        if (isZoomed(current)) MIN_SCALE else DOUBLE_TAP_SCALE

    fun canPageSwipe(scale: Float): Boolean = !isZoomed(scale)

    fun tapCloses(scale: Float): Boolean = !isZoomed(scale)

    fun shouldConsume(zoomed: Boolean, pointerCount: Int, zoom: Float = 1f): Boolean {
        if (zoomed) return true
        if (pointerCount >= 2) return true
        return zoom < 0.99f || zoom > 1.01f
    }

    fun isTap(movePx: Float, maxPointers: Int, slopPx: Float): Boolean =
        maxPointers < 2 && movePx < slopPx

    fun clampOffset(
        x: Float,
        y: Float,
        scale: Float,
        viewportW: Float,
        viewportH: Float,
    ): Pair<Float, Float> {
        if (!isZoomed(scale) || viewportW <= 0f || viewportH <= 0f) return 0f to 0f
        val maxX = (viewportW * (scale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (viewportH * (scale - 1f) / 2f).coerceAtLeast(0f)
        return x.coerceIn(-maxX, maxX) to y.coerceIn(-maxY, maxY)
    }
}
