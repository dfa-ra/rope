package app.rope.android

import app.rope.android.data.ChatPrefs
import app.rope.android.data.MuteChoice
import app.rope.android.data.MuteRules
import app.rope.android.data.effectivelyMuted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuteRulesTest {
    @Test
    fun timedUntilIsNowPlusDurationAndForeverIsZero() {
        val now = 1_700_000_000_000L
        assertEquals(now + MuteRules.HOUR_MS, MuteRules.untilMs(MuteChoice.HOUR, now))
        assertEquals(now + MuteRules.EIGHT_HOURS_MS, MuteRules.untilMs(MuteChoice.EIGHT_HOURS, now))
        assertEquals(now + MuteRules.TWO_DAYS_MS, MuteRules.untilMs(MuteChoice.TWO_DAYS, now))
        assertEquals(0L, MuteRules.untilMs(MuteChoice.FOREVER, now))
        assertEquals(4, MuteRules.CHOICES.size)
    }

    @Test
    fun activeForeverUntilExpiredAndUnmuted() {
        val now = 1_000L
        assertTrue(MuteRules.active(muted = true, muteUntilMs = 0L, nowMs = now))
        assertTrue(MuteRules.active(muted = true, muteUntilMs = now + 1, nowMs = now))
        assertFalse(MuteRules.active(muted = true, muteUntilMs = now, nowMs = now))
        assertFalse(MuteRules.active(muted = true, muteUntilMs = now - 1, nowMs = now))
        assertFalse(MuteRules.active(muted = false, muteUntilMs = now + 9_999, nowMs = now))
        assertFalse(MuteRules.active(muted = false, muteUntilMs = 0L, nowMs = now))
    }

    @Test
    fun labelsAreRussianTelegramCopy() {
        assertEquals("1 час", MuteRules.label(MuteChoice.HOUR))
        assertEquals("8 часов", MuteRules.label(MuteChoice.EIGHT_HOURS))
        assertEquals("2 дня", MuteRules.label(MuteChoice.TWO_DAYS))
        assertEquals("Навсегда", MuteRules.label(MuteChoice.FOREVER))
        assertEquals("Без звука", MuteRules.muteLabel())
        assertEquals("Включить звук", MuteRules.unmuteLabel())
    }

    @Test
    fun chatPrefsRoundTripMuteUntilAndEffective() {
        val now = 5_000L
        val timed = ChatPrefs(muted = true, muteUntilMs = now + 10)
        val parsed = ChatPrefs.parse(timed.toJson())
        assertTrue(parsed.muted)
        assertEquals(now + 10, parsed.muteUntilMs)
        assertTrue(parsed.effectivelyMuted(now))
        assertFalse(parsed.effectivelyMuted(now + 10))
        val forever = ChatPrefs.parse(ChatPrefs(muted = true, muteUntilMs = 0).toJson())
        assertTrue(forever.effectivelyMuted(now))
        val off = ChatPrefs.parse(ChatPrefs(muted = false, muteUntilMs = now + 99).toJson())
        assertFalse(off.effectivelyMuted(now))
        val legacy = ChatPrefs.parse("""{"pinned":false,"muted":true,"unread":0,"last_read_ms":0,"draft":""}""")
        assertTrue(legacy.muted)
        assertEquals(0L, legacy.muteUntilMs)
        assertTrue(legacy.effectivelyMuted(now))
    }
}
