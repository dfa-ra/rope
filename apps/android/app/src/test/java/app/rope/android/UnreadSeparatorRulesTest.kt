package app.rope.android

import app.rope.android.data.AlbumRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatThreadItem
import app.rope.android.data.DateSeparatorRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.UnreadFab
import app.rope.android.data.UnreadSeparatorRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class UnreadSeparatorRulesTest {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now = 1_000_000L

    @Test
    fun noChipWhenUnreadIsZero() {
        val msgs = listOf(incoming("a", 10), incoming("b", 20), outgoing("c", 30))
        assertNull(UnreadSeparatorRules.firstUnreadId(msgs, unreadCount = 0, lastReadMs = 0))
        val items = DateSeparatorRules.items(msgs, now, utc)
        assertEquals(items, UnreadSeparatorRules.insert(items, null))
        assertEquals(items, UnreadSeparatorRules.insert(items, "a", searching = true))
    }

    @Test
    fun firstUnreadIsEarliestIncomingAfterWatermark() {
        val read = incoming("r", 10)
        val first = incoming("u1", 20)
        val second = incoming("u2", 30)
        val mine = outgoing("me", 25)
        val msgs = listOf(read, first, mine, second)
        assertEquals("u1", UnreadSeparatorRules.firstUnreadId(msgs, unreadCount = 2, lastReadMs = 15))
        assertEquals("u1", UnreadSeparatorRules.firstUnreadId(msgs, unreadCount = 99, lastReadMs = 15))
        assertEquals("first", UnreadSeparatorRules.firstUnreadId(
            listOf(incoming("old", 1), incoming("first", 2), incoming("second", 3)),
            unreadCount = 2,
            lastReadMs = 0L,
        ))
    }

    @Test
    fun fallsBackToLastNIncomingWhenWatermarkIsNow() {
        val a = incoming("a", 10)
        val b = incoming("b", 20)
        val c = incoming("c", 30)
        val mine = outgoing("me", 40)
        val msgs = listOf(a, b, c, mine)
        assertEquals("b", UnreadSeparatorRules.firstUnreadId(msgs, unreadCount = 2, lastReadMs = 99_999))
        assertEquals("c", UnreadSeparatorRules.firstUnreadId(msgs, unreadCount = 1, lastReadMs = 99_999))
        assertNull(UnreadSeparatorRules.firstUnreadId(msgs.filter { it.outgoing }, unreadCount = 2, lastReadMs = 0))
    }

    @Test
    fun insertPlacesChipBeforeFirstUnreadAndSkipsDeleted() {
        val read = incoming("r", 10)
        val gone = incoming("gone", 15, deleted = true)
        val unread = incoming("u", 20)
        val later = incoming("v", 30)
        val msgs = listOf(read, gone, unread, later)
        val id = UnreadSeparatorRules.firstUnreadId(msgs, unreadCount = 2, lastReadMs = 12)
        assertEquals("u", id)
        val items = UnreadSeparatorRules.insert(DateSeparatorRules.items(msgs, now, utc), id)
        val unreadAt = items.indexOfFirst { it is ChatThreadItem.Unread }
        assertTrue(unreadAt > 0)
        assertEquals("u", (items[unreadAt + 1] as ChatThreadItem.Bubble).msg.id)
        assertEquals("Непрочитанные сообщения", UnreadSeparatorRules.LABEL)
        assertEquals(items, UnreadSeparatorRules.insert(items, id))
    }

    @Test
    fun insertSitsBeforeAlbumWhenFirstUnreadIsInAlbum() {
        val extra = """{"kind":"image","object_id":"o","sha256":"s","key_b64":"k","mime":"image/jpeg","name":"p.jpg","size":1,"album_id":"alb","album_index":0,"album_count":2}"""
        val extra1 = """{"kind":"image","object_id":"o1","sha256":"s","key_b64":"k","mime":"image/jpeg","name":"p.jpg","size":1,"album_id":"alb","album_index":1,"album_count":2}"""
        val text = incoming("t", 10)
        val a = incoming("a", 20, kind = MessageKind.IMAGE, extra = extra)
        val b = incoming("b", 21, kind = MessageKind.IMAGE, extra = extra1)
        val collapsed = AlbumRules.collapse(DateSeparatorRules.items(listOf(text, a, b), now, utc))
        val inserted = UnreadSeparatorRules.insert(collapsed, "a")
        val unreadAt = inserted.indexOfFirst { it is ChatThreadItem.Unread }
        assertTrue(inserted[unreadAt + 1] is ChatThreadItem.Album)
        assertEquals(unreadAt, UnreadSeparatorRules.scrollIndex(inserted, "a", "a"))
        assertEquals(unreadAt, UnreadSeparatorRules.scrollIndex(inserted, UnreadSeparatorRules.KEY, "a"))
    }

    @Test
    fun fabPointsUpToUnreadThenDownToLatest() {
        assertEquals(UnreadFab.UP, UnreadSeparatorRules.fab(atBottom = true, unreadVisible = false, hasUnread = true, unreadAbove = true))
        assertEquals(UnreadFab.UP, UnreadSeparatorRules.fab(atBottom = false, unreadVisible = false, hasUnread = true, unreadAbove = true))
        assertEquals(UnreadFab.DOWN, UnreadSeparatorRules.fab(atBottom = false, unreadVisible = true, hasUnread = true, unreadAbove = false))
        assertEquals(UnreadFab.DOWN, UnreadSeparatorRules.fab(atBottom = false, unreadVisible = false, hasUnread = false, unreadAbove = false))
        assertEquals(UnreadFab.DOWN, UnreadSeparatorRules.fab(atBottom = false, unreadVisible = false, hasUnread = true, unreadAbove = false))
        assertNull(UnreadSeparatorRules.fab(atBottom = true, unreadVisible = true, hasUnread = true, unreadAbove = false))
        assertNull(UnreadSeparatorRules.fab(atBottom = true, unreadVisible = false, hasUnread = true, unreadAbove = false))
    }

    private fun incoming(
        id: String,
        ts: Long,
        deleted: Boolean = false,
        kind: MessageKind = MessageKind.TEXT,
        extra: String = "",
    ) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = id,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = kind,
        extra = extra,
        deleted = deleted,
        senderId = "peer",
    )

    private fun outgoing(id: String, ts: Long) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = true,
        text = id,
        status = MessageStatus.SENT_TO_SERVER,
        timestampMs = ts,
        senderId = "me",
    )
}
