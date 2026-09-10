package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.ReadMoreRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadMoreRulesTest {
    @Test
    fun inheritStoreAndTelegramPreviewWindow() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals(400, ReadMoreRules.PREVIEW_CHARS)
        assertEquals(8, ReadMoreRules.PREVIEW_LINES)
        assertEquals("Ещё", ReadMoreRules.LABEL)
        assertFalse(ReadMoreRules.needsCollapse("коротко"))
        assertFalse(ReadMoreRules.showLabel("коротко", expanded = false))
        assertEquals("коротко", ReadMoreRules.shown("коротко", expanded = false))
    }

    @Test
    fun longTextCollapsesUntilExpanded() {
        val long = "я".repeat(ReadMoreRules.PREVIEW_CHARS + 40)
        assertTrue(ReadMoreRules.needsCollapse(long))
        val collapsed = ReadMoreRules.collapsed(long)
        assertTrue(collapsed.endsWith("…"))
        assertTrue(collapsed.length < long.length)
        assertEquals(long, ReadMoreRules.shown(long, expanded = true))
        assertEquals(collapsed, ReadMoreRules.shown(long, expanded = false))
        assertTrue(ReadMoreRules.showLabel(long, expanded = false))
        assertFalse(ReadMoreRules.showLabel(long, expanded = true))
    }

    @Test
    fun manyLinesCollapseEvenIfShortChars() {
        val many = (1..12).joinToString("\n") { "строка $it" }
        assertTrue(ReadMoreRules.needsCollapse(many))
        val collapsed = ReadMoreRules.collapsed(many)
        assertTrue(collapsed.endsWith("…"))
        assertEquals(8, ReadMoreRules.lineCount(collapsed.trimEnd('…').trimEnd()))
        assertTrue(ReadMoreRules.lineCount(collapsed) <= ReadMoreRules.PREVIEW_LINES)
    }

    @Test
    fun wordBoundaryPrefersWhitespace() {
        val text = ("слово " + "х".repeat(20) + " ").repeat(20)
        assertTrue(text.length > ReadMoreRules.PREVIEW_CHARS)
        val collapsed = ReadMoreRules.collapsed(text)
        assertFalse(collapsed.contains("FCM", ignoreCase = true))
        assertTrue(collapsed.endsWith("…"))
        val body = collapsed.removeSuffix("…")
        assertTrue(body.last().isWhitespace() || body.last() == 'о' || body.last().isLetter())
    }
}
