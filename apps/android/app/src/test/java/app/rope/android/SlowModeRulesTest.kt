package app.rope.android

import app.rope.android.data.ChatPrefs
import app.rope.android.data.LocalStore
import app.rope.android.data.SlowModeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlowModeRulesTest {
    @Test
    fun chipsLocalOnlyAndVersion() {
        assertEquals("Медленный режим", SlowModeRules.TITLE)
        assertEquals(listOf(0, 10, 30, 60), SlowModeRules.CHIPS.map { it.seconds })
        assertEquals(listOf("Выкл", "10с", "30с", "1м"), SlowModeRules.CHIPS.map { it.label })
        SlowModeRules.CHIPS.forEach { chip ->
            assertFalse(chip.label.contains('\n'))
            assertFalse(chip.label.contains('\r'))
        }
        assertEquals(0, SlowModeRules.normalize(7))
        assertEquals(10, SlowModeRules.normalize(10))
        assertEquals(30, SlowModeRules.parseStored(30))
        assertEquals(0, SlowModeRules.parseStored(-1))
        assertTrue(SlowModeRules.canShow(true))
        assertFalse(SlowModeRules.canShow(false))
        assertTrue(SlowModeRules.HINT.contains("телефоне"))
        assertTrue(SlowModeRules.HINT.contains("автоудаление"))
        assertFalse(SlowModeRules.TITLE.contains("FCM", ignoreCase = true))
        assertFalse(SlowModeRules.HINT.contains("FCM", ignoreCase = true))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun waitAndNoticeAreLocalCooldown() {
        assertEquals(0L, SlowModeRules.waitMs(0, 1_000L, 1_100L))
        assertEquals(0L, SlowModeRules.waitMs(10, 0L, 5_000L))
        assertEquals(4_000L, SlowModeRules.waitMs(10, 1_000L, 7_000L))
        assertEquals(0L, SlowModeRules.waitMs(10, 1_000L, 11_000L))
        assertEquals(1_000L, SlowModeRules.waitMs(60, 1_000L, 60_000L))
        assertEquals("Подождите ещё 1 с", SlowModeRules.blockedNotice(1))
        assertEquals("Подождите ещё 1 с", SlowModeRules.blockedNotice(1000))
        assertEquals("Подождите ещё 4 с", SlowModeRules.blockedNotice(3001))
    }

    @Test
    fun prefsRoundTripWithoutSchemaBump() {
        val empty = ChatPrefs.parse(null)
        assertEquals(0, empty.slowModeSec)
        val parsed = ChatPrefs.parse("""{"pinned":false,"slow_mode_sec":30}""")
        assertEquals(30, parsed.slowModeSec)
        val json = parsed.copy(slowModeSec = SlowModeRules.normalize(60)).toJson()
        assertTrue(json.contains("\"slow_mode_sec\":60"))
        assertEquals(60, ChatPrefs.parse(json).slowModeSec)
        assertEquals(0, ChatPrefs.parse("""{"slow_mode_sec":7}""").slowModeSec)
    }
}
