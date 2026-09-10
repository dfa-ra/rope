package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.GroupAboutRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupAboutRulesTest {
    @Test
    fun labelsAndVersion() {
        assertEquals("О группе", GroupAboutRules.TITLE)
        assertEquals("Только на этом устройстве. Не «О себе» и не фото группы.", GroupAboutRules.HINT)
        assertEquals("group_abouts", GroupAboutRules.KV)
        assertEquals(120, GroupAboutRules.MAX)
        assertFalse(GroupAboutRules.TITLE.contains('\n'))
        assertFalse(GroupAboutRules.HINT.contains('\n'))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun sanitizeStripsCrlfAndCaps() {
        assertEquals("", GroupAboutRules.sanitize(null))
        assertEquals("", GroupAboutRules.sanitize(""))
        assertEquals("привет", GroupAboutRules.sanitize("  привет  "))
        assertEquals("ab", GroupAboutRules.sanitize("a\nb\r"))
        assertEquals("ok", GroupAboutRules.sanitize("ok\u0000"))
        assertEquals("x".repeat(120), GroupAboutRules.sanitize("x".repeat(200)))
        assertEquals(118, GroupAboutRules.remaining("ab"))
    }

    @Test
    fun keyRejectsCrlfAndLooksThroughChatId() {
        assertEquals("g1", GroupAboutRules.key("g1"))
        assertEquals("g1", GroupAboutRules.key("  g1  "))
        assertEquals("g1", GroupAboutRules.key(ChatIds.group("g1")))
        assertNull(GroupAboutRules.key(null))
        assertNull(GroupAboutRules.key(""))
        assertNull(GroupAboutRules.key("g\n1"))
        assertNull(GroupAboutRules.key("g\r1"))
        assertNull(GroupAboutRules.key("g\u00001"))
    }

    @Test
    fun mapRoundTripAndLookup() {
        val encoded = GroupAboutRules.encode(mapOf("g1" to "  привет  ", "bad\nid" to "x", "g2" to ""))
        val parsed = GroupAboutRules.parseMap(encoded)
        assertEquals(mapOf("g1" to "привет"), parsed)
        assertEquals("привет", GroupAboutRules.lookup(parsed, ChatIds.group("g1")))
        assertEquals("", GroupAboutRules.lookup(parsed, "missing"))
        assertEquals(emptyMap<String, String>(), GroupAboutRules.parseMap(null))
        assertEquals(emptyMap<String, String>(), GroupAboutRules.parseMap("{"))
        assertTrue(GroupAboutRules.canSet(true, "me", "me", "member"))
        assertFalse(GroupAboutRules.canSet(true, "me", "other", "member"))
        assertEquals(6, LocalStore.VERSION)
    }
}
