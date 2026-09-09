package app.rope.android

import app.rope.android.data.ChatControl
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatListPreviewKind
import app.rope.android.data.ChatListPreviewRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.ExpireRules
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.RoleRules
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.TextBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ExpireRulesTest {
    @Test
    fun expiresAtIsTimestampPlusTtlSeconds() {
        val ts = 1_700_000_000_000L
        assertEquals(ts + 86_400_000L, ExpireRules.expiresAtMs(ts, ExpireRules.HOURS_24))
        assertEquals(ts + 604_800_000L, ExpireRules.expiresAtMs(ts, ExpireRules.DAYS_7))
        assertEquals(ts + 2_678_400_000L, ExpireRules.expiresAtMs(ts, ExpireRules.DAYS_31))
        assertNull(ExpireRules.expiresAtMs(ts, 0))
        assertNull(ExpireRules.expiresAtMs(ts, -1))
        assertEquals(2_678_400, 31 * 86_400)
    }

    @Test
    fun dueIsInclusiveAtBoundary() {
        val exp = 1_000L
        assertFalse(ExpireRules.due(null, 5_000L))
        assertFalse(ExpireRules.due(0L, 5_000L))
        assertFalse(ExpireRules.due(exp, 999L))
        assertTrue(ExpireRules.due(exp, 1_000L))
        assertTrue(ExpireRules.due(exp, 1_001L))
        assertFalse(ExpireRules.due(null, Long.MAX_VALUE))
    }

    @Test
    fun missingExpFallsBackToTimestampPlusTtl() {
        val ts = 10_000L
        assertEquals(ts + 86_400_000L, ExpireRules.resolveExp(null, ExpireRules.HOURS_24, ts))
        assertEquals(99L, ExpireRules.resolveExp(99L, ExpireRules.HOURS_24, ts))
        assertNull(ExpireRules.resolveExp(null, 0, ts))
    }

    @Test
    fun savedNeverSendsControl() {
        assertFalse(ExpireRules.shouldSendControl(SavedMessagesRules.ID))
        assertFalse(ExpireRules.shouldSendControl("saved:"))
        assertTrue(SavedMessagesRules.skipNetwork(SavedMessagesRules.ID))
        assertTrue(ExpireRules.shouldSendControl("peer-device"))
        assertTrue(ExpireRules.shouldSendControl(ChatIds.group("abc")))
        assertTrue(ExpireRules.canSetTimer(isGroup = false, canManageGroup = false, saved = true))
    }

    @Test
    fun guestCannotSetGroupTimer() {
        assertFalse(
            ExpireRules.canSetTimer(isGroup = true, canManageGroup = false, saved = false),
        )
        assertTrue(
            ExpireRules.canSetTimer(isGroup = true, canManageGroup = true, saved = false),
        )
        assertTrue(
            ExpireRules.canSetTimer(isGroup = false, canManageGroup = false, saved = false),
        )
        assertFalse(
            ExpireRules.senderMaySetTtl(
                isGroup = true,
                senderId = "guest",
                organizerId = "org",
                senderServerRole = "guest",
            ),
        )
        assertTrue(
            ExpireRules.senderMaySetTtl(
                isGroup = true,
                senderId = "org",
                organizerId = "org",
                senderServerRole = "guest",
            ),
        )
        assertTrue(
            ExpireRules.senderMaySetTtl(
                isGroup = true,
                senderId = "guest",
                organizerId = "org",
                senderServerRole = "owner",
            ),
        )
        assertFalse(RoleRules.canManageGroupMembers(true, "mem", "org", "guest"))
        assertFalse(ExpireRules.showSettingsRow(isGroup = true, canSet = false, ttlSec = 0))
        assertTrue(ExpireRules.showSettingsRow(isGroup = true, canSet = false, ttlSec = ExpireRules.HOURS_24))
        assertEquals("Сообщения исчезают через 24 часа", ExpireRules.guestSubtitle(ExpireRules.HOURS_24))
    }

    @Test
    fun unlinkDeletesOnlyUnderMediaRoot() {
        val root = createTempDirectory("rope-media").toFile()
        val inside = File(root, "obj.jpg")
        inside.writeText("plain")
        assertTrue(inside.exists())
        assertTrue(ExpireRules.unlinkIfUnderMedia(inside.absolutePath, root))
        assertFalse(inside.exists())

        val outside = File(createTempDirectory("rope-other").toFile(), "secret.jpg")
        outside.writeText("keep")
        assertFalse(ExpireRules.unlinkIfUnderMedia(outside.absolutePath, root))
        assertTrue(outside.exists())
        assertFalse(ExpireRules.unlinkIfUnderMedia(null, root))
        assertFalse(ExpireRules.unlinkIfUnderMedia("", root))
        assertTrue(ExpireRules.unlinkIfUnderMedia(File(root, "already-gone.bin").absolutePath, root))
    }

    @Test
    fun messageWithoutExpNeverSwept() {
        assertFalse(ExpireRules.shouldWipe(expMs = null, nowMs = 9_000L, tombstone = false))
        assertFalse(ExpireRules.due(null, 9_000L))
        val forever = ChatMessage(
            id = "keep",
            peerDeviceId = "peer",
            outgoing = true,
            text = "навсегда",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
        )
        assertNull(forever.expiresAtMs)
        assertFalse(ExpireRules.due(forever.expiresAtMs, Long.MAX_VALUE))
    }

    @Test
    fun receiveExpireWipesEvenIfExpInFuture() {
        val exp = 9_999_999L
        assertFalse(ExpireRules.shouldWipe(exp, nowMs = 1L, tombstone = false))
        assertTrue(ExpireRules.shouldWipe(exp, nowMs = 1L, tombstone = true))
        assertTrue(ExpireRules.shouldWipe(null, nowMs = 1L, tombstone = true))
    }

    @Test
    fun chatControlParseAcceptsTtlAndExpire() {
        val ttl = ChatControl.parse(
            ChatControl(ChatControl.TTL, "peer", op = "set", text = "86400").toJson(),
        )!!
        assertEquals(ChatControl.TTL, ttl.kind)
        assertEquals("peer", ttl.targetId)
        assertEquals("set", ttl.op)
        assertEquals("86400", ttl.text)
        val expire = ChatControl.parse(
            ChatControl(ChatControl.EXPIRE, "mid-1", op = "set").toJson(),
        )!!
        assertEquals(ChatControl.EXPIRE, expire.kind)
        assertEquals("mid-1", expire.targetId)
        assertEquals("ttl", ChatControl.TTL)
        assertEquals("expire", ChatControl.EXPIRE)
        assertNotNull(ChatControl.parse("""{"kind":"ttl","target":"g:1","op":"clear","text":"0"}"""))
        assertNotNull(ChatControl.parse("""{"kind":"expire","target":"m1"}"""))
    }

    @Test
    fun chatListPreviewIsNotDeletedStubAfterAutoExpire() {
        val remaining = ChatMessage(
            id = "older",
            peerDeviceId = "peer",
            outgoing = false,
            text = "вчера",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
        )
        val copy = ChatListPreviewRules.copy(remaining, draft = "", isGroup = false, myDeviceId = "me")
        assertEquals("вчера", copy.text)
        assertEquals(ChatListPreviewKind.LAST, copy.kind)
        assertNotEquals(ExpireRules.DELETED_STUB, copy.text)
        val empty = ChatListPreviewRules.copy(null, draft = "", isGroup = false, online = true)
        assertEquals(ChatListPreviewKind.PRESENCE, empty.kind)
        assertNotEquals(ExpireRules.DELETED_STUB, empty.text)
        val stub = ChatMessage(
            id = "gone",
            peerDeviceId = "peer",
            outgoing = false,
            text = "",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 2L,
            deleted = true,
        )
        assertEquals(ExpireRules.DELETED_STUB, stub.preview())
        assertNotEquals(stub.preview(), remaining.preview())
    }

    @Test
    fun innerJsonPacksTtlExpAndGroupMid() {
        val exp = 1_789_000_000_000L
        val packed = TextBody.encode("секрет", null, "", "", ttlSec = 86400, expMs = exp)
        val body = TextBody.decode(packed)
        assertEquals("секрет", body.text)
        assertEquals(86400, body.ttlSec)
        assertEquals(exp, body.expiresAtMs)
        val group = GroupTextPayload.parse(
            GroupTextPayload("gid", "hi", 1, ttlSec = 86400, expiresAtMs = exp, mid = "mid-9").toJson(),
        )
        assertEquals(86400, group.ttlSec)
        assertEquals(exp, group.expiresAtMs)
        assertEquals("mid-9", group.mid)
        val media = MediaPayload.parse(
            MediaPayload(
                kind = "image",
                objectId = "o",
                sha256 = "ab",
                keyB64 = "k",
                mime = "image/jpeg",
                name = "a.jpg",
                size = 1,
                ttlSec = 86400,
                expiresAtMs = exp,
                mid = "mid-m",
            ).toJson(),
        )
        assertEquals(86400, media.ttlSec)
        assertEquals(exp, media.expiresAtMs)
        assertEquals("mid-m", media.mid)
        val prefs = ChatPrefs.parse("{}")
        assertEquals(0, prefs.ttlSec)
        val withTtl = ChatPrefs.parse(ChatPrefs(ttlSec = 86400, ttlSetAtMs = 12L).toJson())
        assertEquals(86400, withTtl.ttlSec)
        assertEquals(12L, withTtl.ttlSetAtMs)
        val o = JSONObject().also { ExpireRules.put(it, ExpireRules.stamp(86400, 0L, "mid-x").copy(expMs = exp)) }
        assertEquals(86400, o.getInt("ttl"))
        assertEquals(exp, o.getLong("exp"))
        assertEquals("mid-x", o.getString("mid"))
        assertEquals("group:abc", ExpireRules.ttlTarget(ChatIds.group("abc")))
        assertEquals(ChatIds.group("abc"), ExpireRules.chatIdFromTtlTarget("group:abc", "peer"))
    }

    @Test
    fun remainingCopyAndDurations() {
        val now = 1_000_000L
        assertEquals("исчезнет через 3 ч", ExpireRules.remainingCopy(now + 3 * 3_600_000L, now))
        assertEquals("исчезнет через 2 д", ExpireRules.remainingCopy(now + 2 * 86_400_000L, now))
        assertEquals("исчезнет через 5 мин", ExpireRules.remainingCopy(now + 5 * 60_000L, now))
        assertEquals("истекло", ExpireRules.remainingCopy(now - 1, now))
        assertEquals("Выкл", ExpireRules.rowSubtitle(0))
        assertEquals(4, ExpireRules.OPTIONS.size)
        assertEquals(MessageKind.IMAGE, MessageKind.IMAGE)
    }
}
