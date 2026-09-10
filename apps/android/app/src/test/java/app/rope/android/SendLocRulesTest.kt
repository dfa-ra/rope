package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SendLocFix
import app.rope.android.data.SendLocRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendLocRulesTest {
    @Test
    fun labelsAndVersion() {
        assertEquals("Геопозиция", SendLocRules.LABEL)
        assertEquals("Нужен доступ к геопозиции.", SendLocRules.NEED_PERM)
        assertEquals("Не удалось определить местоположение.", SendLocRules.NEED_FIX)
        assertFalse(SendLocRules.NEED_PERM.contains('\n'))
        assertFalse(SendLocRules.NEED_FIX.contains('\n'))
        assertEquals(listOf("gps", "network", "passive"), SendLocRules.PROVIDERS)
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun permissionAndBounds() {
        assertTrue(SendLocRules.permissionOk(fine = true, coarse = false))
        assertTrue(SendLocRules.permissionOk(fine = false, coarse = true))
        assertFalse(SendLocRules.permissionOk(fine = false, coarse = false))
        assertTrue(SendLocRules.valid(55.751244, 37.618423))
        assertTrue(SendLocRules.valid(-90.0, 180.0))
        assertFalse(SendLocRules.valid(90.1, 0.0))
        assertFalse(SendLocRules.valid(0.0, 181.0))
        assertFalse(SendLocRules.valid(Double.NaN, 0.0))
        assertFalse(SendLocRules.valid(0.0, Double.POSITIVE_INFINITY))
    }

    @Test
    fun osmTextHasNoCrlf() {
        val url = SendLocRules.text(55.751244, 37.618423)
        assertNotNull(url)
        assertTrue(url!!.startsWith("https://www.openstreetmap.org/"))
        assertTrue(url.contains("mlat=55.751244"))
        assertTrue(url.contains("mlon=37.618423"))
        assertFalse(url.contains('\n'))
        assertFalse(url.contains('\r'))
        assertFalse(url.contains('\u0000'))
        assertNull(SendLocRules.text(91.0, 0.0))
        assertEquals(SendLocRules.NEED_PERM, SendLocRules.noticeDenied())
        assertEquals(SendLocRules.NEED_FIX, SendLocRules.noticeMissing())
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun pickNewestValidFix() {
        val stale = SendLocFix(10.0, 10.0, timeMs = 1L)
        val fresh = SendLocFix(55.0, 37.0, timeMs = 9L)
        val bad = SendLocFix(99.0, 0.0, timeMs = 99L)
        val picked = SendLocRules.pick(listOf(stale, fresh, bad))
        assertEquals(55.0, picked!!.lat, 0.0)
        assertEquals(37.0, picked.lon, 0.0)
        assertNull(SendLocRules.pick(emptyList()))
        assertNull(SendLocRules.pick(listOf(bad)))
    }
}
