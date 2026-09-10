package app.rope.android

import app.rope.android.data.EmailTapRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailTapRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun mailtoUriAcceptsSimpleAndRejectsCtl() {
        assertEquals("mailto:ivan@example.com", EmailTapRules.mailtoUri("ivan@example.com"))
        assertEquals("mailto:a.b+tag@sub.example.co", EmailTapRules.mailtoUri("a.b+tag@sub.example.co"))
        assertNull(EmailTapRules.mailtoUri("not-an-email"))
        assertNull(EmailTapRules.mailtoUri("ivan@localhost"))
        assertNull(EmailTapRules.mailtoUri("ivan@example.com\ncc:evil"))
        assertNull(EmailTapRules.mailtoUri("ivan@example.com\u0000x"))
        assertNull(EmailTapRules.mailtoUri(".ivan@example.com"))
        assertNull(EmailTapRules.mailtoUri("ivan@ex..com"))
    }

    @Test
    fun spansSkipOccupiedAndControl() {
        val text = "пиши ivan@example.com или https://example.com/a"
        val mails = EmailTapRules.spans(text)
        assertEquals(1, mails.size)
        assertEquals("mailto:ivan@example.com", mails.single().mailto)
        assertTrue(mails.single().start >= 0)
        val blocked = EmailTapRules.spans(text, listOf(mails.single().start until mails.single().endExclusive))
        assertTrue(blocked.isEmpty())
        assertTrue(EmailTapRules.spans("нет почты").isEmpty())
        assertTrue(EmailTapRules.spans("ivan@exam\rple.com").isEmpty())
        val inUrl = "см https://example.com/u@x.com конец"
        val urlStart = inUrl.indexOf("https://")
        val urlEnd = inUrl.indexOf(" конец")
        assertTrue(EmailTapRules.spans(inUrl, listOf(urlStart until urlEnd)).isEmpty())
    }
}
