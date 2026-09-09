package app.rope.android

import app.rope.android.data.ChatListEmptyRules
import app.rope.android.data.ChatListMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListEmptyRulesTest {
    @Test
    fun searchMissIsFoundNotIdle() {
        val copy = ChatListEmptyRules.copy(ChatListMode.ALL, "  Анн  ", forwarding = false, role = "owner")
        assertEquals("Ничего не найдено", copy.title)
        assertEquals("Нет чатов по запросу «анн».", copy.body)
        assertNull(copy.actionLabel)
        assertFalse(copy.showFab)
        assertFalse(ChatListEmptyRules.showFab(ChatListMode.ALL, "анн", forwarding = false))
    }

    @Test
    fun searchMissBeatsForwardEmpty() {
        val copy = ChatListEmptyRules.copy(ChatListMode.ALL, "боб", forwarding = true, role = "owner")
        assertEquals("Ничего не найдено", copy.title)
        assertEquals("Нет чатов по запросу «боб».", copy.body)
        assertFalse(copy.body.contains("переслать"))
        assertNull(copy.actionLabel)
        assertFalse(copy.showFab)
    }

    @Test
    fun searchMissIsModeAware() {
        val groups = ChatListEmptyRules.copy(ChatListMode.GROUPS, "ком", forwarding = false, role = "owner")
        val calls = ChatListEmptyRules.copy(ChatListMode.CALLS, "ком", forwarding = false, role = "guest")
        assertEquals("Ничего не найдено", groups.title)
        assertEquals("Нет групп по запросу «ком».", groups.body)
        assertNull(groups.actionLabel)
        assertFalse(groups.showFab)
        assertEquals("Нет звонков по запросу «ком».", calls.body)
        assertFalse(ChatListEmptyRules.showFab(ChatListMode.CALLS, "ком", forwarding = false))
    }

    @Test
    fun whitespaceQueryIsIdleNotSearch() {
        val copy = ChatListEmptyRules.copy(ChatListMode.ALL, " \t ", forwarding = false, role = "guest")
        assertEquals("Пока никого нет", copy.title)
        assertEquals("Пока пусто", copy.body)
        assertTrue(copy.showFab)
        assertNull(copy.actionLabel)
        assertTrue(ChatListEmptyRules.showFab(ChatListMode.ALL, " \t ", forwarding = false))
    }

    @Test
    fun idleGroupsKeepsNewGroupCta() {
        val copy = ChatListEmptyRules.copy(ChatListMode.GROUPS, "", forwarding = false, role = "owner")
        assertEquals("Групп пока нет", copy.title)
        assertEquals("Пока пусто", copy.body)
        assertEquals("Новая группа", copy.actionLabel)
        assertTrue(copy.showFab)
    }

    @Test
    fun idleForwardHidesFabAndCta() {
        val owner = ChatListEmptyRules.copy(ChatListMode.ALL, "", forwarding = true, role = "owner")
        assertTrue(owner.body.contains("переслать"))
        assertNull(owner.actionLabel)
        assertFalse(owner.showFab)
        val guest = ChatListEmptyRules.copy(ChatListMode.ALL, "", forwarding = true, role = "guest")
        assertTrue(guest.body.contains("переслать"))
        assertFalse(guest.body.contains("Пригласите"))
    }

    @Test
    fun idleFolderCopy() {
        val unread = ChatListEmptyRules.copy(ChatListMode.ALL, "", forwarding = false, role = "owner", folderId = "unread")
        assertEquals("Нет непрочитанных", unread.title)
        assertEquals("Все прочитано.", unread.body)
        val custom = ChatListEmptyRules.copy(ChatListMode.ALL, "", forwarding = false, role = "owner", folderId = "work")
        assertEquals("Нет чатов в папке", custom.title)
        val groups = ChatListEmptyRules.copy(ChatListMode.GROUPS, "", forwarding = false, role = "owner", folderId = "unread")
        assertEquals("Групп пока нет", groups.title)
    }

    @Test
    fun longQueryFallsBackWithoutLectures() {
        val q = "я".repeat(70)
        val body = ChatListEmptyRules.searchBody(ChatListMode.ALL, q)
        assertEquals("Попробуйте изменить запрос.", body)
        assertTrue(body.length <= ChatListEmptyRules.COPY_MAX)
        assertFalse(body.contains('\n'))
    }
}
