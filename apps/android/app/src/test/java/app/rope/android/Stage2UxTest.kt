package app.rope.android

import app.rope.android.data.AdminSnapshot
import app.rope.android.data.ChatActions
import app.rope.android.data.ChatControl
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatRouting
import app.rope.android.data.ComposerRules
import app.rope.android.data.EnvelopeTypes
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.JsonIds
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageMeta
import app.rope.android.data.MessageStatus
import app.rope.android.data.MessageTime
import app.rope.android.data.Reaction
import app.rope.android.data.ReactionCodec
import app.rope.android.data.ReactionPayload
import app.rope.android.data.RoleRules
import app.rope.android.data.TextBody
import app.rope.android.media.ImageCodec
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
    fun androidNullGroupIdStaysInDirectChat() {
        assertEquals(null, JsonIds.optional("null"))
        assertEquals(null, JsonIds.optional(null))
        assertEquals(null, JsonIds.optional(""))
        val fromNullJson = MediaPayload.parse(
            """{"kind":"voice","object_id":"o","sha256":"ab","key_b64":"KEY","mime":"audio/mp4","name":"v.m4a","size":1,"duration_ms":1000,"group_id":null}""",
        )
        assertEquals(null, fromNullJson.groupId)
        val fromAndroidOptString = MediaPayload.parse(
            """{"kind":"image","object_id":"o","sha256":"ab","key_b64":"KEY","mime":"image/jpeg","name":"p.jpg","size":1,"group_id":"null"}""",
        )
        assertEquals(null, fromAndroidOptString.groupId)
        val dm = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1)
        assertFalse(dm.toJson().contains("group_id"))
        assertEquals("peer-1", ChatRouting.mediaChatId("null", "peer-1", setOf("real-group")))
        assertEquals("peer-1", ChatRouting.mediaChatId(null, "peer-1", emptySet()))
        assertEquals(ChatIds.group("real-group"), ChatRouting.mediaChatId("real-group", "peer-1", setOf("real-group")))
        assertFalse(ChatIds.isOpenableGroup("g:null"))
        assertFalse(ChatRouting.showLeftoverThread("g:null"))
        assertTrue(ChatRouting.showLeftoverThread("peer-1"))
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

    @Test
    fun photoFileNamesStaySafe() {
        assertEquals("jpg", ImageCodec.extensionFor("image/jpeg", "msf:39"))
        assertEquals("abc.jpg", ImageCodec.fileName("abc", "image/jpeg", "primary:DCIM/Camera/x"))
        assertFalse(ImageCodec.fileName("id", "image/png", "a/b/c.png").contains("/"))
        assertEquals(4, ImageCodec.sampleSize(4000, 3000, 1200))
    }

    @Test
    fun messageTimeAndReactions() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 5)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        assertEquals("14:05", MessageTime.label(cal.timeInMillis, cal.timeInMillis))
        assertEquals("14:05 · доставлено", MessageTime.meta(MessageStatus.DELIVERED_TO_DEVICE, true, cal.timeInMillis, now = cal.timeInMillis))
        assertEquals(
            "14:05 · изм. · доставлено",
            MessageTime.meta(MessageStatus.DELIVERED_TO_DEVICE, true, cal.timeInMillis, edited = true, now = cal.timeInMillis),
        )
        val raw = ReactionPayload("mid", "👍", ReactionPayload.SET).toJson()
        val parsed = ReactionPayload.parse(raw)!!
        assertEquals("mid", parsed.targetId)
        assertEquals("👍", parsed.emoji)
        val json = ReactionCodec.toJson(listOf(Reaction("❤️", "dev", "Ann")))
        val back = ReactionCodec.parse(json)
        assertEquals(1, back.size)
        assertEquals("❤️", back[0].emoji)
        assertEquals(null, ReactionPayload.parse("""{"kind":"other"}"""))
    }

    @Test
    fun guestsCanUpdateAppButNotCore() {
        assertTrue(RoleRules.canUpdateApp("guest"))
        assertTrue(RoleRules.canUpdateApp("member"))
        assertTrue(RoleRules.canUpdateApp("owner"))
        assertFalse(RoleRules.canUpgradeCore("guest"))
        assertFalse(RoleRules.canUpgradeCore("member"))
        assertFalse(RoleRules.isOwner(null))
        assertTrue(RoleRules.canUpgradeCore("owner"))
        assertTrue(RoleRules.isOwner("OWNER"))
    }

    @Test
    fun textBodyKeepsPlainAndPacksReply() {
        assertEquals("привет", TextBody.encode("привет", null, "", ""))
        val packed = TextBody.encode("ответ", "mid-1", "цитата", "Анна")
        val (text, replyId, pair) = TextBody.decode(packed)
        assertEquals("ответ", text)
        assertEquals("mid-1", replyId)
        assertEquals("цитата", pair.first)
        assertEquals("Анна", pair.second)
        val plain = TextBody.decode("просто текст")
        assertEquals("просто текст", plain.first)
        assertEquals(null, plain.second)
        val notReplyJson = TextBody.decode("""{"hello":"world"}""")
        assertEquals("""{"hello":"world"}""", notReplyJson.first)
        assertEquals(null, JsonIds.optional("null"))
    }

    @Test
    fun chatControlEditDeleteAndMeta() {
        val edit = ChatControl.parse(ChatControl(ChatControl.EDIT, "m1", text = "новое").toJson())!!
        assertEquals(ChatControl.EDIT, edit.kind)
        assertEquals("m1", edit.targetId)
        assertEquals("новое", edit.text)
        val del = ChatControl.parse(ChatControl(ChatControl.DELETE, "m2").toJson())!!
        assertEquals(ChatControl.DELETE, del.kind)
        assertEquals(null, ChatControl.parse("""{"kind":"other","target":"x"}"""))
        assertEquals(null, ChatControl.parse("""{"kind":"edit"}"""))
        val meta = MessageMeta.parse(MessageMeta("r1", "prev", "Имя", edited = true).toJson())
        assertEquals("r1", meta.replyToId)
        assertEquals("prev", meta.replyPreview)
        assertTrue(meta.edited)
        assertFalse(meta.deleted)
        val group = GroupTextPayload.parse(GroupTextPayload("g", "hi", 1, "r", "цитата", "Боб").toJson())
        assertEquals("r", group.replyTo)
        assertEquals("цитата", group.replyPreview)
        assertEquals("Боб", group.replyName)
    }

    @Test
    fun chatActionsFollowOwnership() {
        val incoming = ChatMessage("1", "p", false, "hi", MessageStatus.DELIVERED_TO_DEVICE, 1L)
        val mine = incoming.copy(id = "2", outgoing = true)
        val deleted = mine.copy(deleted = true)
        val photo = mine.copy(kind = MessageKind.IMAGE)
        assertTrue(ChatActions.canReply(incoming))
        assertTrue(ChatActions.canForward(incoming))
        assertFalse(ChatActions.canEdit(incoming))
        assertFalse(ChatActions.canDelete(incoming))
        assertTrue(ChatActions.canEdit(mine))
        assertTrue(ChatActions.canDelete(mine))
        assertFalse(ChatActions.canEdit(photo))
        assertFalse(ChatActions.canReply(deleted))
        assertFalse(ChatActions.canForward(deleted))
        assertFalse(ChatActions.canEdit(deleted))
        assertEquals("Сообщение удалено", deleted.preview())
    }
}
