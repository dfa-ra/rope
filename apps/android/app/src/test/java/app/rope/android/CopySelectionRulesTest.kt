package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.CopySelectionRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopySelectionRulesTest {
    private fun msg(
        id: String,
        text: String,
        outgoing: Boolean = false,
        sender: String = "",
        deleted: Boolean = false,
        kind: MessageKind = MessageKind.TEXT,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        senderName = sender,
        deleted = deleted,
    )

    @Test
    fun inheritStoreAndSingleIsPlainText() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("Скопировано", CopySelectionRules.NOTICE)
        assertEquals("Вы", CopySelectionRules.YOU)
        val one = msg("1", "привет", outgoing = true)
        assertEquals("привет", CopySelectionRules.join(listOf(one), "Анна"))
        assertTrue(CopySelectionRules.canCopy(listOf(one)))
        assertFalse(CopySelectionRules.NOTICE.contains("FCM", ignoreCase = true))
    }

    @Test
    fun severalIncludeSenderNames() {
        val a = msg("1", "привет", sender = "Анна")
        val b = msg("2", "ок", outgoing = true)
        val joined = CopySelectionRules.join(listOf(a, b), "Анна")
        assertEquals("Анна:\nпривет\n\nВы:\nок", joined)
        assertEquals("Анна", CopySelectionRules.senderLabel(a, "Кира"))
        assertEquals("Вы", CopySelectionRules.senderLabel(b, "Кира"))
        assertEquals("Кира", CopySelectionRules.senderLabel(msg("3", "x"), "Кира"))
    }

    @Test
    fun skipsDeletedAndBlankAndTrims() {
        val keep = msg("1", "  текст  ")
        val gone = msg("2", "нет", deleted = true)
        val voice = msg("3", "", kind = MessageKind.VOICE)
        assertEquals("текст", CopySelectionRules.payload(keep))
        assertEquals(null, CopySelectionRules.payload(gone))
        assertEquals(null, CopySelectionRules.payload(voice))
        assertFalse(CopySelectionRules.canCopy(listOf(gone, voice)))
        assertTrue(CopySelectionRules.canCopy(listOf(keep, gone)))
        assertEquals("Анна:\nтекст", CopySelectionRules.join(listOf(keep, gone), "Анна"))
        assertEquals("", CopySelectionRules.join(emptyList(), "Анна"))
    }
}
