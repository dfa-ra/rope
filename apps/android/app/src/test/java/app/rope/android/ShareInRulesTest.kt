package app.rope.android

import app.rope.android.data.AlbumRules
import app.rope.android.data.LocalStore
import app.rope.android.data.ShareInRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareInRulesTest {
    @Test
    fun inheritStoreAndRejectControlChars() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Выберите чат", ShareInRules.PICK)
        assertEquals("Отправить в Rope", ShareInRules.TITLE)
        assertFalse(ShareInRules.TITLE.contains("FCM", ignoreCase = true))
        assertNull(ShareInRules.sanitizeUri("content://ok\nbad"))
        assertNull(ShareInRules.sanitizeUri("content://ok\rbad"))
        assertNull(ShareInRules.sanitizeUri("content://ok\u0000bad"))
        assertNull(ShareInRules.sanitizeUri("https://evil.example/x.png"))
        assertNull(ShareInRules.sanitizeUri("javascript:alert(1)"))
        assertNull(ShareInRules.sanitizeText("hi\u0000there"))
        assertEquals("content://media/1", ShareInRules.sanitizeUri("  content://media/1  "))
        assertEquals("две\nстроки", ShareInRules.sanitizeText("  две\nстроки  "))
    }

    @Test
    fun onlySendActionsAndUnconsumedIntents() {
        assertTrue(ShareInRules.isSend(ShareInRules.ACTION_SEND))
        assertTrue(ShareInRules.isSend(ShareInRules.ACTION_SEND_MULTIPLE))
        assertFalse(ShareInRules.isSend("android.intent.action.MAIN"))
        assertFalse(ShareInRules.isSend(null))
        assertTrue(ShareInRules.alreadyConsumed(true))
        assertFalse(ShareInRules.alreadyConsumed(false))
        assertNull(
            ShareInRules.consume(
                ShareInRules.ACTION_SEND,
                "content://media/1",
                emptyList(),
                null,
                consumed = true,
            ),
        )
    }

    @Test
    fun streamsAndCaptionBecomeInbound() {
        val shot = ShareInRules.consume(
            ShareInRules.ACTION_SEND,
            "content://shots/1",
            emptyList(),
            "подпись",
        )
        assertEquals(listOf("content://shots/1"), shot!!.uris)
        assertEquals("подпись", shot.text)
        val many = ShareInRules.consume(
            ShareInRules.ACTION_SEND_MULTIPLE,
            null,
            (0 until AlbumRules.MAX_PHOTOS + 2).map { "content://img/$it" },
            null,
        )
        assertEquals(AlbumRules.MAX_PHOTOS, many!!.uris.size)
        val note = ShareInRules.consume(ShareInRules.ACTION_SEND, null, emptyList(), "привет")
        assertTrue(note!!.uris.isEmpty())
        assertEquals("привет", note.text)
        assertTrue(ShareInRules.active(emptyList(), "x"))
        assertFalse(ShareInRules.active(emptyList(), "  "))
        assertNull(ShareInRules.consume("android.intent.action.VIEW", "content://x", emptyList(), null))
        assertNull(ShareInRules.consume(ShareInRules.ACTION_SEND, null, emptyList(), "  "))
    }
}
