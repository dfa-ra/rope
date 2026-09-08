package app.rope.android.data

/**
 * Telegram-style photo box: natural aspect, max ~240×320 dp, min edge ~120 dp, never distort.
 */
object PhotoLayout {
    const val MAX_WIDTH_DP = 240f
    const val MAX_HEIGHT_DP = 320f
    const val MIN_EDGE_DP = 120f

    data class Box(val widthDp: Float, val heightDp: Float)

    fun box(width: Int, height: Int): Box {
        val srcW = width.coerceAtLeast(1).toFloat()
        val srcH = height.coerceAtLeast(1).toFloat()
        val fit = minOf(MAX_WIDTH_DP / srcW, MAX_HEIGHT_DP / srcH)
        var w = srcW * fit
        var h = srcH * fit
        val minEdge = minOf(w, h)
        if (minEdge < MIN_EDGE_DP) {
            val up = MIN_EDGE_DP / minEdge
            w *= up
            h *= up
            val refit = minOf(MAX_WIDTH_DP / w, MAX_HEIGHT_DP / h, 1f)
            if (refit < 1f) {
                w *= refit
                h *= refit
            }
        }
        return Box(w, h)
    }
}
