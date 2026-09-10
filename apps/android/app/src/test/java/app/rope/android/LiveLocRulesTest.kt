package app.rope.android

import app.rope.android.data.LiveLocFix
import app.rope.android.data.LiveLocRules
import app.rope.android.data.LiveLocSession
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveLocRulesTest {
    @Test
    fun labelsAndVersion() {
        assertEquals("Трансляция", LiveLocRules.LABEL)
        assertEquals("Остановить", LiveLocRules.STOP)
        assertEquals("Нужен доступ к геопозиции.", LiveLocRules.NEED_PERM)
        assertEquals("Не удалось определить местоположение.", LiveLocRules.NEED_FIX)
        assertEquals(20_000L, LiveLocRules.PERIOD_MS)
        assertEquals(listOf("gps", "network", "passive"), LiveLocRules.PROVIDERS)
        assertEquals(listOf("15 минут", "1 час", "8 часов"), LiveLocRules.CHOICES.map { it.label })
        assertEquals(listOf(15L * 60_000L, 60L * 60_000L, 8L * 60L * 60_000L), LiveLocRules.CHOICES.map { it.ms })
        assertFalse(LiveLocRules.LABEL.contains('\n'))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun liveTextIsNotOneShotUrlOnly() {
        val choice = LiveLocRules.choice(15L * 60_000L)!!
        val text = LiveLocRules.text(55.751244, 37.618423, choice)
        assertNotNull(text)
        assertTrue(text!!.startsWith("Трансляция · 15 минут"))
        assertTrue(text.contains("https://www.openstreetmap.org/"))
        assertTrue(text.contains("mlat=55.751244"))
        assertTrue(LiveLocRules.isLive(text))
        assertFalse(LiveLocRules.isLive("https://www.openstreetmap.org/?mlat=1&mlon=2#map=16/1/2"))
        assertNull(LiveLocRules.text(91.0, 0.0, choice))
        assertNull(LiveLocRules.choice(1L))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun sessionActiveOnMatchingChat() {
        val session = LiveLocSession("peer-1", untilMs = 100L, choiceMs = 15L * 60_000L, messageId = "m1")
        assertTrue(LiveLocRules.active(100L, 99L))
        assertFalse(LiveLocRules.active(100L, 100L))
        assertTrue(LiveLocRules.here(session, "peer-1", 50L))
        assertFalse(LiveLocRules.here(session, "peer-2", 50L))
        assertFalse(LiveLocRules.here(session, "peer-1", 100L))
        assertFalse(LiveLocRules.here(null, "peer-1", 50L))
        assertNull(LiveLocRules.cleanChatId("bad\nid"))
        assertTrue(LiveLocRules.permissionOk(fine = true, coarse = false))
        assertFalse(LiveLocRules.permissionOk(fine = false, coarse = false))
        val picked = LiveLocRules.pick(
            listOf(
                LiveLocFix(10.0, 10.0, 1L),
                LiveLocFix(55.0, 37.0, 9L),
                LiveLocFix(99.0, 0.0, 99L),
            ),
        )
        assertEquals(55.0, picked!!.lat, 0.0)
        assertEquals(LiveLocRules.NEED_PERM, LiveLocRules.noticeDenied())
        assertEquals(LiveLocRules.NEED_FIX, LiveLocRules.noticeMissing())
    }
}
