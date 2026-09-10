package app.rope.android

import app.rope.android.data.LinkOpenRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkOpenRulesTest {
    @Test
    fun inheritStoreAndConfirmCopy() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("Открыть ссылку", LinkOpenRules.TITLE)
        assertEquals("Открыть", LinkOpenRules.CONFIRM)
        assertEquals("Отмена", LinkOpenRules.CANCEL)
    }

    @Test
    fun acceptHttpsRejectsCrLfNulAndHttp() {
        assertEquals(
            "https://example.com/a",
            LinkOpenRules.accept("  https://example.com/a  "),
        )
        assertNull(LinkOpenRules.accept("http://example.com"))
        assertNull(LinkOpenRules.accept("https://example.com\nbad"))
        assertNull(LinkOpenRules.accept("https://example.com\rbad"))
        assertNull(LinkOpenRules.accept("https://example.com\u0000bad"))
        assertNull(LinkOpenRules.accept("ftp://example.com"))
        assertNull(LinkOpenRules.accept(""))
    }

    @Test
    fun bodyShowsHost() {
        assertEquals("example.com", LinkOpenRules.body("https://example.com/path"))
        assertEquals("sub.example.com", LinkOpenRules.host("https://SUB.Example.com"))
        assertTrue(LinkOpenRules.body("https://example.com").isNotBlank())
    }
}
