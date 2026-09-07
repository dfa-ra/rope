package app.rope.android

import app.rope.android.data.AdminSnapshot
import app.rope.android.data.ChatIds
import app.rope.android.data.ComposerRules
import app.rope.android.data.EnvelopeTypes
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage2UxTest {
    @Test
    fun voiceIsOneGesture() {
        assertEquals(1, ComposerRules.VOICE_TAPS_TO_SEND)
        assertTrue(ComposerRules.showMicButton("", false))
        assertFalse(ComposerRules.showSendButton("", false))
        assertTrue(ComposerRules.showSendButton("привет", false))
        assertFalse(ComposerRules.showSendButton("привет", true))
    }

    @Test
    fun photoAndCallAndGroupBudgets() {
        assertTrue(ComposerRules.PHOTO_TAPS_TO_SEND <= 3)
        assertEquals(1, ComposerRules.ANSWER_CALL_TAPS)
        assertTrue(ComposerRules.GROUP_CREATE_TAPS <= 3)
    }

    @Test
    fun mediaPayloadRoundtrip() {
        val p = MediaPayload("voice", "obj-1", "ab", "KEY", "audio/mp4", "v.m4a", 1200, 3400, "g1")
        val got = MediaPayload.parse(p.toJson())
        assertEquals("voice", got.kind)
        assertEquals("obj-1", got.objectId)
        assertEquals(MessageKind.VOICE, got.messageKind())
        assertTrue(got.preview().contains("Голосовое"))
        assertEquals("0:03", MediaPayload.formatDuration(3400))
    }

    @Test
    fun unknownEnvelopeTypeIsLoud() {
        assertEquals(MessageKind.UNKNOWN, EnvelopeTypes.kindOf(99u, ""))
        assertEquals(MessageKind.TEXT, EnvelopeTypes.kindOf(EnvelopeTypes.TEXT, ""))
        assertEquals(MessageKind.VOICE, EnvelopeTypes.kindOf(EnvelopeTypes.MEDIA, MediaPayload("voice", "o", "h", "k", "a", "n", 1).toJson()))
    }

    @Test
    fun groupTextAndIds() {
        val p = GroupTextPayload.parse(GroupTextPayload("gid", "hi", 2).toJson())
        assertEquals("gid", p.groupId)
        assertEquals("hi", p.text)
        assertEquals(2, p.epoch)
        assertTrue(ChatIds.isGroup(ChatIds.group("gid")))
        assertEquals("gid", ChatIds.rawGroupId(ChatIds.group("gid")))
        assertFalse(ChatIds.isGroup("device"))
    }

    @Test
    fun adminCardsCoverStage2() {
        val obj = JSONObject()
            .put("version", "0.2.0")
            .put("protocol_version", 1)
            .put("member_count", 2)
            .put("device_count", 2)
            .put("online_devices", 1)
            .put("mailbox_count", 0)
            .put("object_count", 3)
            .put("object_bytes", 2048)
            .put("group_count", 1)
            .put("max_object_bytes", 25 * 1024 * 1024)
            .put("listen", "0.0.0.0:8443")
        val snap = AdminSnapshot.from(obj)
        assertTrue(snap.cards.size >= 6)
        assertEquals("ожидает", ComposerRules.statusLabel(MessageStatus.CREATED, true))
        assertEquals("на сервере", ComposerRules.statusLabel(MessageStatus.SENT_TO_SERVER, true))
        assertEquals("доставлено", ComposerRules.statusLabel(MessageStatus.DELIVERED_TO_DEVICE, true))
    }
}
