package app.rope.android.data

/**
 * Telegram-style photo box: natural aspect, max ~240×320 dp, min edge ~120 dp, never distort.
 */
object PhotoLayout {
    const val MAX_WIDTH_DP = 240f
    const val MAX_HEIGHT_DP = 320f
    const val MIN_EDGE_DP = 120f
    const val MOSAIC_WIDTH_DP = 240f
    const val MOSAIC_HEIGHT_DP = 240f
    const val MOSAIC_GAP_DP = 2f

    data class Box(val widthDp: Float, val heightDp: Float)

    data class Tile(val xDp: Float, val yDp: Float, val widthDp: Float, val heightDp: Float)

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

    /** Telegram-like 2-up mosaic for 2–10 album members. Count 1 is a full tile. */
    fun mosaic(count: Int): List<Tile> {
        val n = count.coerceAtLeast(1)
        if (n == 1) return listOf(Tile(0f, 0f, MOSAIC_WIDTH_DP, MOSAIC_HEIGHT_DP))
        val gap = MOSAIC_GAP_DP
        val colW = (MOSAIC_WIDTH_DP - gap) / 2f
        if (n == 2) {
            return listOf(
                Tile(0f, 0f, colW, MOSAIC_HEIGHT_DP),
                Tile(colW + gap, 0f, colW, MOSAIC_HEIGHT_DP),
            )
        }
        if (n == 3) {
            val halfH = (MOSAIC_HEIGHT_DP - gap) / 2f
            return listOf(
                Tile(0f, 0f, colW, MOSAIC_HEIGHT_DP),
                Tile(colW + gap, 0f, colW, halfH),
                Tile(colW + gap, halfH + gap, colW, halfH),
            )
        }
        val rows = (n + 1) / 2
        val rowH = (MOSAIC_HEIGHT_DP - gap * (rows - 1)) / rows
        return (0 until n).map { i ->
            val row = i / 2
            val col = i % 2
            val lastOdd = n % 2 == 1 && i == n - 1
            val w = if (lastOdd) MOSAIC_WIDTH_DP else colW
            Tile(col * (colW + gap), row * (rowH + gap), w, rowH)
        }
    }
}
