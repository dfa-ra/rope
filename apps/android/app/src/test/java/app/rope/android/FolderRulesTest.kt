package app.rope.android

import app.rope.android.data.AlbumRules
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatListEmptyRules
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.CustomFolder
import app.rope.android.data.FolderRules
import app.rope.android.data.FolderSnap
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.UnreadBadgeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderRulesTest {
    private val saved = conv(SavedMessagesRules.ID, title = SavedMessagesRules.TITLE)
    private val anna = conv("aaa111", unread = 2, title = "Анна")
    private val muted = conv("bbb222", unread = 3, muted = true, title = "Борис")
    private val group = conv(ChatIds.group("g-uuid"), title = "Команда", group = true)
    private val read = conv("ccc333", unread = 0, title = "Кира")

    @Test
    fun allContainsSaved() {
        val base = listOf(saved, anna, group)
        val rows = FolderRules.apply(base, FolderRules.ALL_ID)
        assertTrue(rows.any { SavedMessagesRules.isSaved(it.id) })
        assertEquals(3, rows.size)
        assertTrue(FolderRules.member(SavedMessagesRules.ID, FolderRules.ALL_ID, FolderRules.defaultSnap(), unread = 0))
    }

    @Test
    fun unreadOnlyUnreadGtZero() {
        val rows = FolderRules.apply(listOf(saved, anna, read, group), FolderRules.UNREAD_ID)
        assertEquals(listOf(anna.id), rows.map { it.id })
        assertFalse(rows.any { it.unread <= 0 })
    }

    @Test
    fun unreadIncludesMuted() {
        val rows = FolderRules.apply(listOf(anna, muted, read), FolderRules.UNREAD_ID)
        assertEquals(setOf(anna.id, muted.id), rows.map { it.id }.toSet())
        assertEquals(
            UnreadBadgeKind.ACCENT,
            FolderRules.chipBadgeKind(listOf(anna, muted), FolderRules.UNREAD_ID, FolderRules.defaultSnap()),
        )
        assertEquals(
            UnreadBadgeKind.MUTED,
            FolderRules.chipBadgeKind(listOf(muted), FolderRules.UNREAD_ID, FolderRules.defaultSnap()),
        )
        assertEquals(5, FolderRules.chipUnread(listOf(anna, muted), FolderRules.UNREAD_ID, FolderRules.defaultSnap()))
    }

    @Test
    fun emptyIncludeIsEmpty() {
        val snap = FolderSnap(custom = listOf(CustomFolder("work", "Работа", 0)))
        val rows = FolderRules.apply(listOf(anna, group, saved), "work", snap)
        assertTrue(rows.isEmpty())
        assertFalse(FolderRules.member(anna.id, "work", snap, anna.unread))
    }

    @Test
    fun excludeWins() {
        val snap = FolderSnap(
            custom = listOf(
                CustomFolder(
                    "work",
                    "Работа",
                    0,
                    include = listOf(anna.id, group.id, saved.id),
                    exclude = listOf(saved.id, anna.id),
                ),
            ),
        )
        val rows = FolderRules.apply(listOf(saved, anna, group), "work", snap)
        assertEquals(listOf(group.id), rows.map { it.id })
        assertFalse(FolderRules.member(anna.id, "work", snap, 2))
        assertTrue(FolderRules.member(group.id, "work", snap, 0))
    }

    @Test
    fun applyAfterRowsKeepsPinDivider() {
        val pinned = anna.copy(pinned = true)
        val convos = listOf(pinned, read.copy(pinned = false))
        val snap = FolderSnap(
            custom = listOf(CustomFolder("f", "Друзья", 0, include = listOf(pinned.id, read.id))),
        )
        val base = ChatListRules.rows(convos, "", ChatListMode.ALL)
        val rows = FolderRules.apply(base, "f", snap, ChatListMode.ALL)
        assertTrue(ChatListRules.showPinDivider(rows, ""))
        assertEquals(listOf(pinned.id), ChatListRules.pinnedBlock(rows, "").map { it.id })
        assertEquals(listOf(read.id), ChatListRules.unpinnedBlock(rows, "").map { it.id })
    }

    @Test
    fun searchMissInsideFolder() {
        val snap = FolderSnap(custom = listOf(CustomFolder("f", "Друзья", 0, include = listOf(anna.id))))
        val base = ChatListRules.rows(listOf(anna, muted), "кира", ChatListMode.ALL)
        val rows = FolderRules.apply(base, "f", snap)
        assertTrue(rows.isEmpty())
        val copy = ChatListEmptyRules.copy(ChatListMode.ALL, "кира", forwarding = false, role = "owner", folderId = "f")
        assertEquals(ChatListEmptyRules.SEARCH_TITLE, copy.title)
        assertEquals("Ничего не найдено", copy.title)
    }

    @Test
    fun capTen() {
        var snap = FolderSnap()
        repeat(FolderRules.MAX_CUSTOM) { i ->
            val (next, err) = FolderRules.addCustom(snap, "Папка $i", id = "id$i")
            assertEquals(null, err)
            snap = next
        }
        assertEquals(10, snap.custom.size)
        val (overflow, err) = FolderRules.addCustom(snap, "Ещё")
        assertEquals(FolderRules.TOO_MANY, err)
        assertEquals(10, overflow.custom.size)
        assertEquals(AlbumRules.MAX_PHOTOS, 10)
    }

    @Test
    fun corruptKvFallsBackToAll() {
        assertEquals(FolderRules.ALL_ID, FolderRules.parse(null).selected)
        assertEquals(FolderRules.ALL_ID, FolderRules.parse("").selected)
        assertEquals(FolderRules.ALL_ID, FolderRules.parse("{not json").selected)
        assertEquals(FolderRules.ALL_ID, FolderRules.parse("[]").selected)
        val badSelected = FolderRules.parse("""{"v":1,"selected":"missing","custom":[]}""")
        assertEquals(FolderRules.ALL_ID, badSelected.selected)
        val round = FolderRules.parse(FolderRules.encode(FolderSnap(selected = FolderRules.UNREAD_ID)))
        assertEquals(FolderRules.UNREAD_ID, round.selected)
    }

    @Test
    fun groupsModeIgnoresSelected() {
        val snap = FolderSnap(custom = listOf(CustomFolder("work", "Работа", 0, include = listOf(anna.id))))
        val base = listOf(anna, group)
        assertEquals(base, FolderRules.apply(base, "work", snap, ChatListMode.GROUPS))
        assertEquals(base, FolderRules.apply(base, FolderRules.UNREAD_ID, snap, ChatListMode.CALLS))
        assertEquals(listOf(anna), FolderRules.apply(base, "work", snap, ChatListMode.ALL))
    }

    @Test
    fun pinIsGlobalNotPerFolder() {
        assertFalse(ChatPrefs().toJson().contains("folder"))
        assertFalse(ChatPrefs(pinned = true).toJson().contains("folder"))
        val pinnedInWork = anna.copy(pinned = true)
        val other = read.copy(pinned = false)
        val snap = FolderSnap(
            custom = listOf(CustomFolder("work", "Работа", 0, include = listOf(pinnedInWork.id, other.id))),
        )
        val rows = FolderRules.apply(listOf(other, pinnedInWork), "work", snap)
        val sorted = rows.sortedWith { a, b -> ChatListRules.compare(a, b) }
        assertEquals(pinnedInWork.id, sorted.first().id)
        assertTrue(sorted.first().pinned)
        assertEquals(FolderRules.chips(FolderRules.defaultSnap()).map { it.id }, listOf("all", "unread", "edit"))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("chat_folders", FolderRules.KV_KEY)
        assertEquals(1, FolderRules.VERSION)
        assertFalse(FolderRules.KV_KEY == "chat_prefs")
    }

    private fun conv(
        id: String,
        unread: Int = 0,
        pinned: Boolean = false,
        muted: Boolean = false,
        title: String = id,
        group: Boolean = false,
        last: ChatMessage? = null,
    ) = Conversation(
        id = id,
        title = title,
        subtitle = "",
        isGroup = group,
        online = false,
        last = last ?: ChatMessage(
            "m-$id",
            id,
            true,
            "hi",
            MessageStatus.DELIVERED_TO_DEVICE,
            1L,
            kind = if (group) MessageKind.GROUP_TEXT else MessageKind.TEXT,
        ),
        pinned = pinned,
        muted = muted,
        unread = unread,
    )
}
