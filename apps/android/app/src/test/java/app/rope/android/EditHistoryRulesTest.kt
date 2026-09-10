package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.EditHistoryRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageMeta
import app.rope.android.data.MessageStatus
import app.rope.android.data.MessageTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryRulesTest {
    @Test
    fun tapShowsOriginalAcrossReEdits() {
        val first = EditHistoryRules.capture(null, "привет")
        assertEquals("привет", first)
        val second = EditHistoryRules.capture(first, "привет!")
        assertEquals("привет", second)
        assertEquals("привет", EditHistoryRules.persist(edited = true, prior = first))
        assertNull(EditHistoryRules.persist(edited = false, prior = "привет"))
        assertTrue(EditHistoryRules.clickable(edited = true, prior = first))
        assertFalse(EditHistoryRules.clickable(edited = true, prior = "  "))
        assertFalse(EditHistoryRules.clickable(edited = true, prior = first, deleted = true))
        assertFalse(EditHistoryRules.showLabel(edited = true, deleted = true))
        assertEquals("привет", EditHistoryRules.dialogBody(first))
        assertEquals(EditHistoryRules.MISSING, EditHistoryRules.dialogBody("  "))
        assertEquals("Было", EditHistoryRules.TITLE)
        assertEquals("изм.", EditHistoryRules.LABEL)
        assertEquals("Закрыть", EditHistoryRules.CLOSE)
    }

    @Test
    fun metaRoundtripKeepsPriorWithoutSchemaBump() {
        val meta = MessageMeta.parse(
            MessageMeta(edited = true, priorText = "старое").toJson(),
        )
        assertTrue(meta.edited)
        assertEquals("старое", meta.priorText)
        assertFalse(MessageMeta.parse(MessageMeta(edited = true).toJson()).toJson().contains("prior_text"))
        val msg = ChatMessage(
            id = "m",
            peerDeviceId = "p",
            outgoing = true,
            text = "новое",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
            edited = true,
            priorText = "старое",
        )
        val packed = MessageMeta.of(msg)
        assertEquals("старое", packed.priorText)
        assertTrue(packed.edited)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun metaLineStillJoinsIzm() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 5)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        assertEquals(
            "14:05 · изм. · доставлено",
            MessageTime.meta(
                MessageStatus.DELIVERED_TO_DEVICE,
                true,
                cal.timeInMillis,
                edited = true,
                now = cal.timeInMillis,
            ),
        )
        assertEquals(EditHistoryRules.LABEL, "изм.")
    }
}
