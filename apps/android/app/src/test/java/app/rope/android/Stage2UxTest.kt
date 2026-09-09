package app.rope.android

import app.rope.android.data.AdminSnapshot
import app.rope.android.data.CallMedia
import app.rope.android.data.CallSignal
import app.rope.android.data.IceServerSpec
import app.rope.android.data.IceServers
import app.rope.android.data.ChatActions
import app.rope.android.data.ChatControl
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatListHit
import app.rope.android.data.ChatListRules
import app.rope.android.data.QueryHighlight
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.ChatRouting
import app.rope.android.data.Conversation
import app.rope.android.data.MessageSearch
import app.rope.android.data.TypingRules
import app.rope.android.data.ComposerRules
import app.rope.android.data.VoiceGesture
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
import org.junit.Assert.assertNull
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
        assertFalse(ComposerRules.showSendButton("привет", true, recordingLocked = false))
        assertTrue(ComposerRules.showSendButton("", true, recordingLocked = true))
        assertTrue(ComposerRules.showSendButton("привет", true, recordingLocked = true))
        assertFalse(ComposerRules.showMicButton("", true, recordingLocked = true))
        assertTrue(ComposerRules.showMicButton("", true, recordingLocked = false))
        assertEquals(80f, ComposerRules.VOICE_LOCK_SLIDE_UP)
        assertEquals(80f, ComposerRules.VOICE_CANCEL_SLIDE_LEFT)
        assertEquals(VoiceGesture.LOCK, ComposerRules.voiceGesture(0f, -80f))
        assertEquals(VoiceGesture.CANCEL, ComposerRules.voiceGesture(-80f, 0f))
        assertEquals(VoiceGesture.HOLD, ComposerRules.voiceGesture(-20f, -20f))
        assertEquals(VoiceGesture.LOCK, ComposerRules.voiceGesture(-40f, -100f))
        assertEquals(VoiceGesture.CANCEL, ComposerRules.voiceGesture(-100f, -40f))
        assertTrue(ComposerRules.shouldLockVoice(-80f))
        assertTrue(ComposerRules.shouldCancelVoice(-80f))
        assertFalse(ComposerRules.shouldLockVoice(-20f))
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
        assertFalse(ChatRouting.showLeftoverThread("saved:"))
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
            .put("ice_enabled", true)
            .put("turn_running", false)
            .put("turn_error", "порт 443 занят другим сервисом, TURNS должен быть на 5349")
            .put("turn_port", 3478)
            .put("turns_port", 443)
        val snap = AdminSnapshot.from(obj)
        assertTrue(snap.cards.size >= 6)
        val turn = snap.cards.first { it.label == "TURN" }
        assertEquals("не слушает", turn.value)
        assertTrue(turn.hint.contains("443"))
        assertEquals("работает", AdminSnapshot.turnValue(JSONObject().put("turn_running", true).put("ice_enabled", true)))
        assertEquals(
            "allocate нет",
            AdminSnapshot.turnValue(
                JSONObject().put("turn_running", true).put("ice_enabled", true).put("turn_allocate_ok", false),
            ),
        )
        assertEquals("настроен", AdminSnapshot.turnValue(JSONObject().put("ice_enabled", true)))
        assertEquals("нет", AdminSnapshot.turnValue(JSONObject()))
        assertTrue(AdminSnapshot.turnHint(JSONObject()).contains("3478"))
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
        assertTrue(RoleRules.canOpenStatus("guest"))
        assertFalse(RoleRules.canUpgradeCore("guest"))
        assertFalse(RoleRules.canUpgradeCore("member"))
        assertFalse(RoleRules.canShowInviteQr("guest"))
        assertFalse(RoleRules.canShowAdminCards("guest"))
        assertFalse(RoleRules.isOwner(null))
        assertTrue(RoleRules.canUpgradeCore("owner"))
        assertTrue(RoleRules.canShowInviteQr("owner"))
        assertTrue(RoleRules.canShowAdminCards("owner"))
        assertTrue(RoleRules.isOwner("OWNER"))
    }

    @Test
    fun textBodyKeepsPlainAndPacksReply() {
        assertEquals("привет", TextBody.encode("привет", null, "", ""))
        val packed = TextBody.encode("ответ", "mid-1", "цитата", "Анна")
        val body = TextBody.decode(packed)
        assertEquals("ответ", body.text)
        assertEquals("mid-1", body.replyTo)
        assertEquals("цитата", body.replyPreview)
        assertEquals("Анна", body.replyName)
        assertNull(body.forwardedFrom)
        val plain = TextBody.decode("просто текст")
        assertEquals("просто текст", plain.text)
        assertEquals(null, plain.replyTo)
        val notReplyJson = TextBody.decode("""{"hello":"world"}""")
        assertEquals("""{"hello":"world"}""", notReplyJson.text)
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
        assertTrue(ChatActions.canCopy(incoming))
        assertTrue(ChatActions.canPin(incoming))
        assertFalse(ChatActions.canCopy(deleted))
        assertFalse(ChatActions.canPin(deleted))
        assertFalse(ChatActions.canOpen(incoming))
        assertTrue(ChatActions.canOpen(photo.copy(outgoing = false)))
        val video = mine.copy(kind = MessageKind.VIDEO)
        assertTrue(ChatActions.canOpen(video.copy(outgoing = false)))
        assertFalse(ChatActions.canOpen(deleted.copy(kind = MessageKind.IMAGE)))
        assertFalse(ChatActions.canOpen(deleted.copy(kind = MessageKind.VIDEO)))
    }

    @Test
    fun chatListSearchPinMuteAndTyping() {
        val a = Conversation("1", "Анна", "привет", false, true, null, pinned = true, unread = 2)
        val b = Conversation("2", "Боб", "ок", false, false, null, muted = true)
        assertTrue(ChatListRules.matches(a, "анн"))
        assertFalse(ChatListRules.matches(b, "анн"))
        assertTrue(ChatListRules.compare(a, b) < 0)
        val prefs = ChatPrefs.parse(ChatPrefs(pinned = true, muted = true, unread = 3, draft = "черн", pinnedMessageId = "m1", muteUntilMs = 9).toJson())
        assertTrue(prefs.pinned)
        assertTrue(prefs.muted)
        assertEquals(3, prefs.unread)
        assertEquals("черн", prefs.draft)
        assertEquals("m1", prefs.pinnedMessageId)
        assertEquals(9L, prefs.muteUntilMs)
        val msg = ChatMessage("m", "p", false, "Секретный текст", MessageStatus.DELIVERED_TO_DEVICE, 1L)
        assertTrue(MessageSearch.matches(msg, "секрет"))
        assertFalse(MessageSearch.matches(msg, "фото"))
        assertTrue(TypingRules.shouldSend(0, 3_000, "hi"))
        assertFalse(TypingRules.shouldSend(2_500, 3_000, "hi"))
        assertTrue(TypingRules.isActive(4_000, 3_000))
        val typing = ChatControl.parse("""{"kind":"typing"}""")!!
        assertEquals(ChatControl.TYPING, typing.kind)
        val pin = ChatControl.parse(ChatControl(ChatControl.PIN, "m9", op = "set").toJson())!!
        assertEquals(ChatControl.PIN, pin.kind)
        assertEquals("m9", pin.targetId)
        assertEquals("в сети", MessageTime.lastSeenLabel("", true))
        val seen = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 5)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val rfc = java.time.Instant.ofEpochMilli(seen.timeInMillis).toString()
        assertEquals("был(а) 14:05", MessageTime.lastSeenLabel(rfc, false, seen.timeInMillis))
    }

    @Test
    fun chatListSearchRanksTitleOverPreviewAndIgnoresPins() {
        fun msg(text: String, t: Long) =
            ChatMessage("m$t", "p", false, text, MessageStatus.DELIVERED_TO_DEVICE, t)
        val prefix = Conversation("1", "Анна", "zzz", false, true, msg("zzz", 1L), pinned = true)
        val title = Conversation("2", "Марианна", "zzz", false, false, msg("zzz", 50L))
        val preview = Conversation("3", "Боб", "секретный план", false, false, msg("секретный план", 100L))
        val presence = Conversation("4", "Кира", "в сети", false, true, null)
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(prefix, "  Анн  "))
        assertEquals(ChatListHit.TITLE, ChatListRules.hit(title, "анн"))
        assertEquals(ChatListHit.PREVIEW, ChatListRules.hit(preview, "секрет"))
        assertEquals(ChatListHit.NONE, ChatListRules.hit(presence, "сети"))
        assertFalse(ChatListRules.matches(presence, "сети"))
        assertTrue(ChatListRules.matches(prefix, "анн"))
        val ranked = ChatListRules.rows(listOf(preview, title, prefix, presence), "анн")
        assertEquals(listOf("1", "2"), ranked.map { it.id })
        val idle = ChatListRules.rows(listOf(preview, prefix), "")
        assertEquals(listOf("1", "3"), idle.map { it.id })
        assertTrue(ChatListRules.showPinDivider(idle, ""))
        assertFalse(ChatListRules.showPinDivider(idle, "анн"))
        assertEquals(emptyList<Conversation>(), ChatListRules.pinnedBlock(idle, "боб"))
        assertEquals(listOf("1"), ChatListRules.pinnedBlock(idle, "").map { it.id })
        assertEquals(0 until 3, QueryHighlight.firstRange("Анна", "анн"))
        assertNull(QueryHighlight.firstRange("Анна", "   "))
        assertEquals("анн", ChatListRules.normalize("  Анн  "))
        assertTrue(ChatListRules.searching("ан"))
        assertFalse(ChatListRules.searching(" \t "))
    }

    @Test
    fun chatListSearchWordPrefixRanksSecondTitleWord() {
        fun msg(text: String, t: Long) =
            ChatMessage("m$t", "p", false, text, MessageStatus.DELIVERED_TO_DEVICE, t)
        val word = Conversation("w", "Мария Анна", "zzz", false, false, msg("zzz", 10L))
        val substring = Conversation("s", "Марианна", "zzz", false, false, msg("zzz", 20L))
        val first = Conversation("f", "Анна", "zzz", false, true, msg("zzz", 1L), pinned = true)
        val hyphen = Conversation("h", "Анна-Мария", "zzz", false, false, msg("zzz", 5L))
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(word, "анн"))
        assertEquals(ChatListHit.TITLE, ChatListRules.hit(substring, "анн"))
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(first, "анн"))
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(hyphen, "мар"))
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(hyphen, "анн"))
        assertEquals(ChatListHit.TITLE, ChatListRules.hit(Conversation("i", "Иван", "zzz", false, false, msg("zzz", 2L)), "ан"))
        val ranked = ChatListRules.rows(listOf(substring, word, first), "анн")
        assertEquals(listOf("w", "f", "s"), ranked.map { it.id })
        assertEquals(6 until 9, QueryHighlight.firstRange("Мария Анна", "анн"))
        assertEquals(5 until 7, QueryHighlight.firstRange("Иван Анна", "ан"))
        assertEquals(5 until 8, QueryHighlight.firstRange("Анна-Мария", "мар"))
        assertEquals(6, ChatListRules.wordPrefixIndex("Мария Анна", "анн"))
        assertEquals(-1, ChatListRules.wordPrefixIndex("Марианна", "анн"))
    }

    @Test
    fun chatListSearchHighlightStaysInBoundsWhenDottedIExpandsOnLowercase() {
        fun msg(text: String, t: Long) =
            ChatMessage("m$t", "p", false, text, MessageStatus.DELIVERED_TO_DEVICE, t)
        val istanbul = "İstanbul Anna"
        val triple = "İİİ Anna"
        assertTrue("İ".lowercase().length > 1)
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(Conversation("i", istanbul, "zzz", false, false, msg("zzz", 1L)), "ann"))
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(Conversation("t", triple, "zzz", false, false, msg("zzz", 2L)), "ann"))
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(Conversation("w", "Мария Анна", "zzz", false, false, msg("zzz", 10L)), "анн"))
        assertEquals(ChatListHit.TITLE, ChatListRules.hit(Conversation("s", "Марианна", "zzz", false, false, msg("zzz", 20L)), "анн"))
        val istanbulRange = QueryHighlight.firstRange(istanbul, "ann")!!
        assertTrue(istanbulRange.first >= 0)
        assertTrue(istanbulRange.last < istanbul.length)
        assertEquals("Ann", istanbul.substring(istanbulRange.first, istanbulRange.last + 1))
        val tripleRange = QueryHighlight.firstRange(triple, "ann")!!
        assertTrue(tripleRange.first >= 0)
        assertTrue(tripleRange.last < triple.length)
        assertEquals("Ann", triple.substring(tripleRange.first, tripleRange.last + 1))
        assertEquals(istanbul.indexOf('A'), ChatListRules.wordPrefixIndex(istanbul, "ann"))
        assertEquals(triple.indexOf('A'), ChatListRules.wordPrefixIndex(triple, "ann"))
    }

    @Test
    fun webrtcSignalsStayInCallPayload() {
        val offer = CallSignal.parse(CallSignal(CallSignal.OFFER, sdp = "v=0").toJson())!!
        assertEquals(CallSignal.OFFER, offer.kind)
        assertEquals("v=0", offer.sdp)
        val ice = CallSignal.parse(CallSignal(CallSignal.ICE, candidate = "typ host", sdpMid = "0", sdpMLineIndex = 0).toJson())!!
        assertEquals(CallSignal.ICE, ice.kind)
        assertEquals("typ host", ice.candidate)
        assertEquals(null, CallSignal.parse("""{"kind":"ring"}"""))
        assertEquals(null, CallSignal.parse("""{"kind":"offer","sdp":null}"""))
        assertTrue(CallMedia.STUN_URLS.any { it.startsWith("stun:") })
        assertEquals("WebRTC · DTLS-SRTP", CallMedia.label("CONNECTED"))
        assertEquals("WebRTC · через сервер", CallMedia.label("CONNECTED", viaRelay = true))
        assertTrue(CallMedia.label("FAILED").contains("TURN"))
        assertEquals("WebRTC", CallMedia.PROTOCOL)
        assertTrue(CallMedia.isRelayCandidate("candidate:1 1 udp 1 1.2.3.4 3478 typ relay raddr 10.0.0.2"))
        assertFalse(CallMedia.isRelayCandidate("candidate:1 1 udp 1 1.2.3.4 3478 typ host"))
    }

    @Test
    fun iceServersParseCredentialsAndNullIds() {
        val raw = """
            [
              {"urls":["stun:vps.example:3478"]},
              {
                "urls":["turns:vps.example:443?transport=tcp","turn:vps.example:3478"],
                "username":"1700000000:rope",
                "credential":"abc"
              }
            ]
        """.trimIndent()
        val parsed = IceServers.parse(raw)
        assertEquals(2, parsed.size)
        assertEquals(listOf("stun:vps.example:3478"), parsed[0].urls)
        assertEquals(null, parsed[0].username)
        assertEquals("1700000000:rope", parsed[1].username)
        assertEquals("abc", parsed[1].credential)
        assertTrue(parsed[1].hasTurn)

        val androidNull = IceServers.parse(
            """[{"urls":["turn:vps:3478"],"username":"null","credential":"null"}]""",
        )
        assertEquals(1, androidNull.size)
        assertEquals(null, androidNull[0].username)
        assertEquals(null, androidNull[0].credential)
        assertEquals(null, JsonIds.optional("null"))

        val fromInfo = IceServers.fromInfo(
            JSONObject("""{"server_id":"x","ice_servers":[{"urls":"stun:vps:3478"}]}"""),
        )
        assertEquals(listOf("stun:vps:3478"), fromInfo[0].urls)

        assertEquals(emptyList<IceServerSpec>(), IceServers.parse(null))
        assertEquals(emptyList<IceServerSpec>(), IceServers.parse("null"))
        assertEquals(emptyList<IceServerSpec>(), IceServers.parse(""))
    }

    @Test
    fun iceServersDropGoogleWhenVpsPresent() {
        val vps = IceServers.parse("""[{"urls":["stun:203.0.113.9:3478","turn:203.0.113.9:3478"],"username":"u","credential":"c"}]""")
        val resolved = IceServers.resolve(vps)
        assertEquals(vps[0].urls, resolved[0].urls)
        assertFalse(IceServers.usesPublicStunFallback(resolved))
        assertTrue(resolved.none { spec -> spec.urls.any { it.contains("google") || it.contains("cloudflare") } })

        val fallback = IceServers.resolve(emptyList())
        assertTrue(IceServers.usesPublicStunFallback(fallback))
        assertEquals(CallMedia.STUN_URLS, fallback.flatMap { it.urls })
    }
}
