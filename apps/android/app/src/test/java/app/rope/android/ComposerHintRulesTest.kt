package app.rope.android

import app.rope.android.data.ComposerHintKind
import app.rope.android.data.ComposerHintRules
import app.rope.android.data.GroupChatUx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerHintRulesTest {
    @Test
    fun replyTitleIsNameNotOtvetDot() {
        val copy = ComposerHintRules.reply("Аня", "привет")
        assertEquals(ComposerHintKind.REPLY, copy.kind)
        assertEquals("Аня", copy.title)
        assertFalse(copy.title.contains("Ответ ·"))
        assertFalse(copy.title.startsWith("Ответ"))
        assertEquals("привет", copy.body)
        assertEquals("Отменить ответ", copy.dismissContentDescription)
    }

    @Test
    fun replyUsesQuoteNameYou() {
        val name = GroupChatUx.replyQuoteName("", outgoing = true)
        val copy = ComposerHintRules.reply(name, "фото")
        assertEquals("Вы", copy.title)
        assertEquals("фото", copy.body)
    }

    @Test
    fun blankNameFallsBackToOtvetNotCompound() {
        val copy = ComposerHintRules.reply("  ", "текст")
        assertEquals("Ответ", copy.title)
        assertFalse(copy.title.contains("·"))
    }

    @Test
    fun editTitleIsEditingNotPreview() {
        val copy = ComposerHintRules.edit("старое сообщение")
        assertEquals(ComposerHintKind.EDIT, copy.kind)
        assertEquals("Редактирование", copy.title)
        assertEquals("старое сообщение", copy.body)
        assertEquals("Отменить редактирование", copy.dismissContentDescription)
        assertFalse(copy.dismissContentDescription.contains("Отмена"))
    }

    @Test
    fun blankPreviewIsMessage() {
        assertEquals("Сообщение", ComposerHintRules.reply("Аня", "  \n\t ").body)
        assertEquals("Сообщение", ComposerHintRules.edit("").body)
    }

    @Test
    fun multilinePreviewIsOneLine() {
        val copy = ComposerHintRules.reply("Аня", "первая\nвторая\r\nтретья")
        assertEquals("первая вторая третья", copy.body)
        assertFalse(copy.body.contains('\n'))
    }

    @Test
    fun crlfNameFallsBackToOtvetBeforeTrim() {
        val lf = ComposerHintRules.reply("Аня\nAdmin", "привет")
        assertEquals(ComposerHintRules.REPLY_FALLBACK_TITLE, lf.title)
        assertFalse(lf.title.contains('\n'))
        assertEquals("Ответ", ComposerHintRules.reply("Аня\r", "привет").title)
        assertEquals("Ответ", ComposerHintRules.reply("Аня\u0000x", "привет").title)
        assertEquals("Ответ", ComposerHintRules.reply("Аня\n", "привет").title)
        assertEquals("Аня", ComposerHintRules.reply("  Аня  ", "привет").title)
    }

    @Test
    fun replyChromeShowsSpanNotOnlyFullBody() {
        val copy = ComposerHintRules.reply("Аня", "длинное исходное сообщение", "фрагмент")
        assertEquals("фрагмент", copy.body)
        assertEquals("Аня", copy.title)
    }

    @Test
    fun longPreviewClipsWithEllipsis() {
        val q = "я".repeat(90)
        val body = ComposerHintRules.clipBody(q)
        assertTrue(body.endsWith("…"))
        assertTrue(body.length <= ComposerHintRules.BODY_MAX)
        assertEquals(ComposerHintRules.BODY_MAX, body.length)
    }
}
