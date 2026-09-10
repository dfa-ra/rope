package app.rope.android

import app.rope.android.data.AlbumRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatThreadItem
import app.rope.android.data.DateSeparatorRules
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class AlbumRulesTest {
    @Test
    fun singletonHasNoAlbumId() {
        val slots = AlbumRules.slots(1, albumId = "ignored")
        assertEquals(1, slots.size)
        assertNull(slots[0].albumId)
        assertEquals(0, slots[0].index)
        assertEquals(1, slots[0].count)
        assertFalse(slots[0].grouped)
        assertEquals("Фото", AlbumRules.preview(1))
    }

    @Test
    fun twoToTenShareAlbumIdAndIndexOrder() {
        val slots = AlbumRules.slots(3, albumId = "alb-1")
        assertEquals(listOf(0, 1, 2), slots.map { it.index })
        assertTrue(slots.all { it.albumId == "alb-1" && it.count == 3 && it.grouped })
        assertEquals("Альбом · 3 фото", AlbumRules.preview(3))
        val generated = AlbumRules.slots(2)
        assertEquals(generated[0].albumId, generated[1].albumId)
        assertNotNull(generated[0].albumId)
    }

    @Test
    fun capsAtTen() {
        assertEquals(10, AlbumRules.cap(15))
        assertEquals(10, AlbumRules.slots(15, "x").size)
        assertEquals(10, AlbumRules.slots(15, "x").first().count)
        assertEquals(0, AlbumRules.slots(0).size)
        assertEquals(AlbumRules.MAX_PHOTOS, 10)
    }

    @Test
    fun mosaicMembersFollowAlbumIndexNotTimestamp() {
        val lateFirst = photo("a", extra = albumJson("alb", 0, 3), ts = 300)
        val earlySecond = photo("b", extra = albumJson("alb", 1, 3), ts = 100)
        val midThird = photo("c", extra = albumJson("alb", 2, 3), ts = 200)
        val other = photo("d", extra = albumJson("other", 0, 2), ts = 150)
        val ordered = AlbumRules.members(listOf(lateFirst, earlySecond, midThird, other), "alb")
        assertEquals(listOf("a", "b", "c"), ordered.map { it.id })
    }

    @Test
    fun singletonAlbumLooksLikeNormalPhoto() {
        val single = photo("p1", extra = imageJson())
        val withNullAlbum = photo("p2", extra = albumJson(null, 0, 1))
        val items = listOf(
            ChatThreadItem.Day("2026-09-09", "Сегодня"),
            ChatThreadItem.Bubble(single),
            ChatThreadItem.Bubble(withNullAlbum),
        )
        val collapsed = AlbumRules.collapse(items)
        assertEquals(3, collapsed.size)
        assertTrue(collapsed[1] is ChatThreadItem.Bubble)
        assertTrue(collapsed[2] is ChatThreadItem.Bubble)
        assertNull(AlbumRules.albumId(single))
        assertEquals(listOf(single), AlbumRules.siblings(listOf(single, withNullAlbum), single))
    }

    @Test
    fun collapseConsecutiveAlbumMembersToMosaic() {
        val a = photo("a", extra = albumJson("alb", 1, 3), ts = 2)
        val b = photo("b", extra = albumJson("alb", 0, 3), ts = 1)
        val c = photo("c", extra = albumJson("alb", 2, 3), ts = 3)
        val text = ChatMessage("t", "p", false, "hi", MessageStatus.DELIVERED_TO_DEVICE, 4)
        val collapsed = AlbumRules.collapse(
            listOf(
                ChatThreadItem.Bubble(a),
                ChatThreadItem.Bubble(b),
                ChatThreadItem.Bubble(c),
                ChatThreadItem.Bubble(text),
            ),
        )
        assertEquals(2, collapsed.size)
        val album = collapsed[0] as ChatThreadItem.Album
        assertEquals(listOf("b", "a", "c"), album.members.map { it.id })
        assertTrue(collapsed[1] is ChatThreadItem.Bubble)
        assertEquals(0, AlbumRules.indexOfMessage(collapsed, "b"))
        assertEquals(0, AlbumRules.indexOfMessage(collapsed, "c"))
        assertEquals(1, AlbumRules.indexOfMessage(collapsed, "t"))
    }

    @Test
    fun threadDateChipsStillSeparateThenAlbumCollapses() {
        val utc = TimeZone.getTimeZone("UTC")
        val now = 1_778_000_000_000L
        val a = photo("a", extra = albumJson("alb", 0, 2), ts = now - 86_400_000)
        val b = photo("b", extra = albumJson("alb", 1, 2), ts = now - 86_400_000 + 1)
        val items = AlbumRules.collapse(DateSeparatorRules.items(listOf(a, b), now, utc))
        assertTrue(items[0] is ChatThreadItem.Day)
        val album = items[1] as ChatThreadItem.Album
        assertEquals(listOf("a", "b"), album.members.map { it.id })
    }

    @Test
    fun notifyIdCoalescesAlbumMembers() {
        assertEquals(AlbumRules.notifyId("Фото", "alb-9"), AlbumRules.notifyId("Альбом · 3 фото", "alb-9"))
        assertNotEquals(AlbumRules.notifyId("Фото", null), AlbumRules.notifyId("hi", null))
        assertEquals("Фото".hashCode(), AlbumRules.notifyId("Фото", null))
    }

    @Test
    fun albumIdRejectsControlBeforeTrim() {
        assertEquals("alb", AlbumRules.albumKey(" alb "))
        assertNull(AlbumRules.albumKey("\nalb"))
        assertNull(AlbumRules.albumKey("alb\n"))
        assertNull(AlbumRules.albumKey("alb\r"))
        assertNull(AlbumRules.albumKey("alb\u0000x"))
        assertNull(AlbumRules.albumKey(""))
        assertNull(AlbumRules.albumKey("   "))
        val poisoned = photo("p", extra = JSONObject(albumJson("alb", 0, 2)).put("album_id", "\nalb").toString())
        assertNull(AlbumRules.albumId(poisoned))
        val padded = photo("q", extra = JSONObject(albumJson("alb", 0, 2)).put("album_id", " alb ").toString())
        assertEquals("alb", AlbumRules.albumId(padded))
        assertTrue(AlbumRules.members(listOf(poisoned, padded), "\nalb").isEmpty())
        assertEquals(listOf("q"), AlbumRules.members(listOf(poisoned, padded), " alb ").map { it.id })
        val crlfSlots = AlbumRules.slots(2, "\nalb")
        assertNotEquals("\nalb", crlfSlots[0].albumId)
        assertEquals(crlfSlots[0].albumId, crlfSlots[1].albumId)
        val spaced = AlbumRules.slots(2, " alb-1 ")
        assertTrue(spaced.all { it.albumId == "alb-1" })
        assertEquals("Фото".hashCode(), AlbumRules.notifyId("Фото", "alb\n9"))
        assertEquals(AlbumRules.notifyId("x", " alb "), AlbumRules.notifyId("x", "alb"))
    }

    @Test
    fun mediaPayloadOmitsAlbumFieldsForSingleton() {
        val single = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1)
        assertFalse(single.toJson().contains("album_id"))
        val grouped = single.copy(albumId = "alb", albumIndex = 1, albumCount = 4)
        val parsed = MediaPayload.parse(grouped.toJson())
        assertEquals("alb", parsed.albumId)
        assertEquals(1, parsed.albumIndex)
        assertEquals(4, parsed.albumCount)
        assertEquals("Альбом · 4 фото", parsed.preview())
        assertEquals("Фото", single.preview())
    }

    @Test
    fun videoMembersCollapseIntoTheSameMosaic() {
        val photo = photo("a", extra = albumJson("alb", 0, 2), ts = 1)
        val clip = ChatMessage(
            id = "v",
            peerDeviceId = "p",
            outgoing = false,
            text = "Видео",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 2,
            kind = MessageKind.VIDEO,
            extra = MediaPayload(
                kind = "video",
                objectId = "o-v",
                sha256 = "ab",
                keyB64 = "KEY",
                mime = "video/mp4",
                name = "c.mp4",
                size = 1,
                durationMs = 3400,
                albumId = "alb",
                albumIndex = 1,
                albumCount = 2,
            ).toJson(),
        )
        val collapsed = AlbumRules.collapse(
            listOf(ChatThreadItem.Bubble(photo), ChatThreadItem.Bubble(clip)),
        )
        val album = collapsed.single() as ChatThreadItem.Album
        assertEquals(listOf("a", "v"), album.members.map { it.id })
        assertEquals("alb", AlbumRules.albumId(clip))
        assertEquals("Альбом · 2", AlbumRules.preview(2, videoCount = 1))
    }

    private fun photo(id: String, extra: String, ts: Long = 1L) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = "Фото",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = MessageKind.IMAGE,
        extra = extra,
    )

    private fun imageJson() = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1).toJson()

    private fun albumJson(albumId: String?, index: Int, count: Int) = MediaPayload(
        kind = "image",
        objectId = "o-$index",
        sha256 = "ab",
        keyB64 = "KEY",
        mime = "image/jpeg",
        name = "p.jpg",
        size = 1,
        albumId = albumId,
        albumIndex = index,
        albumCount = count,
    ).toJson()
}
