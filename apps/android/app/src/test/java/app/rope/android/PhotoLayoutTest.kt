package app.rope.android

import app.rope.android.data.PhotoLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoLayoutTest {
    @Test
    fun landscapeFitsMaxWidth() {
        val box = PhotoLayout.box(3200, 1800)
        assertEquals(PhotoLayout.MAX_WIDTH_DP, box.widthDp, 0.5f)
        assertTrue(box.heightDp <= PhotoLayout.MAX_HEIGHT_DP + 0.5f)
        assertEquals(3200f / 1800f, box.widthDp / box.heightDp, 0.02f)
    }

    @Test
    fun portraitFitsMaxHeight() {
        val box = PhotoLayout.box(1200, 2000)
        assertEquals(PhotoLayout.MAX_HEIGHT_DP, box.heightDp, 0.5f)
        assertTrue(box.widthDp <= PhotoLayout.MAX_WIDTH_DP + 0.5f)
        assertEquals(1200f / 2000f, box.widthDp / box.heightDp, 0.02f)
    }

    @Test
    fun squareStaysSquareInsideMax() {
        val box = PhotoLayout.box(1500, 1500)
        assertEquals(box.widthDp, box.heightDp, 0.01f)
        assertTrue(box.widthDp <= PhotoLayout.MAX_WIDTH_DP + 0.5f)
        assertTrue(box.widthDp >= PhotoLayout.MIN_EDGE_DP - 0.5f)
    }

    @Test
    fun tinyImageScalesUpWithoutDistort() {
        val box = PhotoLayout.box(40, 40)
        assertEquals(box.widthDp, box.heightDp, 0.01f)
        assertTrue(box.widthDp >= PhotoLayout.MIN_EDGE_DP - 0.5f)
        assertTrue(box.widthDp <= PhotoLayout.MAX_WIDTH_DP + 0.5f)
    }

    @Test
    fun neverDistortsExtremeAspect() {
        val wide = PhotoLayout.box(4000, 200)
        assertEquals(4000f / 200f, wide.widthDp / wide.heightDp, 0.02f)
        assertTrue(wide.widthDp <= PhotoLayout.MAX_WIDTH_DP + 0.5f)
        val tall = PhotoLayout.box(200, 4000)
        assertEquals(200f / 4000f, tall.widthDp / tall.heightDp, 0.02f)
        assertTrue(tall.heightDp <= PhotoLayout.MAX_HEIGHT_DP + 0.5f)
    }

    @Test
    fun mosaicTwoUpSideBySide() {
        val tiles = PhotoLayout.mosaic(2)
        assertEquals(2, tiles.size)
        assertEquals(0f, tiles[0].xDp, 0.01f)
        assertTrue(tiles[1].xDp > tiles[0].widthDp)
        assertEquals(PhotoLayout.MOSAIC_HEIGHT_DP, tiles[0].heightDp, 0.01f)
        assertEquals(PhotoLayout.MOSAIC_HEIGHT_DP, tiles[1].heightDp, 0.01f)
    }

    @Test
    fun mosaicThreeIsTwoColumn() {
        val tiles = PhotoLayout.mosaic(3)
        assertEquals(3, tiles.size)
        assertEquals(tiles[0].heightDp, PhotoLayout.MOSAIC_HEIGHT_DP, 0.01f)
        assertTrue(tiles[1].yDp < tiles[2].yDp)
        assertEquals(tiles[1].xDp, tiles[2].xDp, 0.01f)
    }
}
