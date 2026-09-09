package app.rope.android

import app.rope.android.data.DirectoryDevice
import app.rope.android.data.NicknameRules
import app.rope.android.ui.conversationOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NicknameRulesTest {
    @Test
    fun normalizeTrimsCapsAndClearsBlank() {
        assertEquals("Мама", NicknameRules.normalize("  Мама  "))
        assertEquals("Анна Борис", NicknameRules.normalize("Анна   Борис"))
        assertEquals("", NicknameRules.normalize("   "))
        assertEquals("", NicknameRules.normalize(null))
        val long = "x".repeat(NicknameRules.MAX + 8)
        assertEquals(NicknameRules.MAX, NicknameRules.normalize(long).length)
    }

    @Test
    fun displayPrefersNickThenDirectoryThenIdPrefix() {
        assertEquals("Мама", NicknameRules.display("Мама", "Анна", "dev-1"))
        assertEquals("Анна", NicknameRules.display("  ", "Анна", "dev-1"))
        assertEquals("dev-1xxx".take(8), NicknameRules.display(null, "", "dev-1xxxYYYY"))
        assertEquals("Чат", NicknameRules.display(null, "", ""))
        assertTrue(NicknameRules.isCustom("Мама"))
        assertFalse(NicknameRules.isCustom("  "))
        assertEquals("Анна", NicknameRules.originalLine("Мама", "Анна"))
        assertNull(NicknameRules.originalLine(null, "Анна"))
        assertNull(NicknameRules.originalLine("Анна", "Анна"))
    }

    @Test
    fun jsonRoundTripDropsBlankKeys() {
        val json = NicknameRules.toJson(mapOf(" d1 " to "  Мама ", "x" to "  ", "" to "Анна"))
        val parsed = NicknameRules.parse(json)
        assertEquals(mapOf("d1" to "Мама"), parsed)
        assertEquals(emptyMap<String, String>(), NicknameRules.parse(null))
        assertEquals(emptyMap<String, String>(), NicknameRules.parse("{"))
    }

    @Test
    fun conversationTitleUsesNick() {
        val d = DirectoryDevice("dev-1xxxYYYY", "m", "Анна", ByteArray(0), "", true)
        assertEquals("Мама", conversationOf(d, "Мама").title)
        assertEquals("Анна", conversationOf(d, null).title)
        assertEquals("dev-1xxx", conversationOf(d.copy(displayName = ""), null).title)
    }
}
