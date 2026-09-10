package app.rope.android.data

/**
 * Pinch/double-tap zoom in the photo viewer. Not in-thread video seek,
 * not CallVideoRenderer.
 */
object PhotoZoomRules {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 4f
    const val DOUBLE_TAP_SCALE = 2.5f

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    fun isZoomed(scale: Float): Boolean = scale > MIN_SCALE + 0.02f

    fun doubleTapScale(current: Float): Float =
        if (isZoomed(current)) MIN_SCALE else DOUBLE_TAP_SCALE

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

    fun canPageSwipe(scale: Float): Boolean = !isZoomed(scale)
}
