package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.GroupChatUx
import app.rope.android.data.MessageStatus
import app.rope.android.data.RoleRules
import app.rope.android.data.RopeGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupChatUxTest {
    private fun msg(
        id: String,
        outgoing: Boolean,
        text: String,
        senderId: String = "",
        senderName: String = "",
        ts: Long = 1_000L,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "g:crew",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        senderId = senderId,
        senderName = senderName,
    )

    @Test
    fun groupListPreviewIsNameColonText() {
        val incoming = msg("1", false, "привет", senderId = "d2", senderName = "Аня")
        assertEquals("Аня: привет", GroupChatUx.listPreview(incoming, "me"))
        val mine = msg("2", true, "ок", senderId = "me", senderName = "Боря")
        assertEquals("Вы: ок", GroupChatUx.listPreview(mine, "me"))
        val byId = msg("3", false, "фото", senderId = "me", senderName = "Боря")
        assertEquals("Вы: фото", GroupChatUx.listPreview(byId, "me"))
        assertEquals("3 участников", GroupChatUx.groupSubtitle(null, 3, "me"))
        assertEquals("Аня: привет", GroupChatUx.groupSubtitle(incoming, 3, "me"))
    }

    @Test
    fun senderLabelHiddenOnOwnAndShownOnFirstIncoming() {
        assertFalse(GroupChatUx.showSenderName(isGroup = true, outgoing = true, firstInCluster = true))
        assertFalse(GroupChatUx.showSenderName(isGroup = true, outgoing = false, firstInCluster = false))
        assertFalse(GroupChatUx.showSenderName(isGroup = false, outgoing = false, firstInCluster = true))
        assertTrue(GroupChatUx.showSenderName(isGroup = true, outgoing = false, firstInCluster = true))
    }

    @Test
    fun typingCopyForNPeople() {
        assertEquals("", GroupChatUx.typingLine(emptyList()))
        assertEquals("печатает…", GroupChatUx.typingLine(listOf("Аня"), isGroup = false))
        assertEquals("Аня печатает…", GroupChatUx.typingLine(listOf("Аня")))
        assertEquals("Аня и Боря печатают…", GroupChatUx.typingLine(listOf("Аня", "Боря")))
        assertEquals("Аня, Боря и Вика печатают…", GroupChatUx.typingLine(listOf("Аня", "Боря", "Вика")))
        assertEquals("Аня, Боря и ещё 2 печатают…", GroupChatUx.typingLine(listOf("Аня", "Боря", "Вика", "Глеб")))
    }

    @Test
    fun consecutiveSameSenderClusters() {
        val a1 = msg("a1", false, "раз", "d2", "Аня", 1_000)
        val a2 = msg("a2", false, "два", "d2", "Аня", 2_000)
        val b1 = msg("b1", false, "три", "d3", "Боря", 3_000)
        val mine = msg("m1", true, "четыре", "me", "Я", 4_000)
        val list = listOf(a1, a2, b1, mine)
        assertTrue(GroupChatUx.firstInCluster(list, 0))
        assertFalse(GroupChatUx.firstInCluster(list, 1))
        assertTrue(GroupChatUx.lastInCluster(list, 1))
        assertTrue(GroupChatUx.firstInCluster(list, 2))
        assertTrue(GroupChatUx.firstInCluster(list, 3))
        assertFalse(GroupChatUx.sameCluster(a1, b1))
        assertTrue(GroupChatUx.sameCluster(a1, a2))
    }

    @Test
    fun mentionSpansHighlightAtName() {
        val raw = "Эй, @Аня смотри @Боря"
        val spans = GroupChatUx.mentionSpans(raw, listOf("Аня", "Боря", "Вы"))
        assertEquals(2, spans.size)
        assertEquals("@Аня", raw.substring(spans[0]))
        assertEquals("@Боря", raw.substring(spans[1]))
        assertTrue(GroupChatUx.mentionSpans("без упоминаний", listOf("Аня")).isEmpty())
    }

    @Test
    fun mentionQueryStartsAfterAtOrWhitespace() {
        val bare = GroupChatUx.mentionQuery("@", 1)
        assertEquals(0, bare?.atIndex)
        assertEquals("", bare?.query)
        val mid = GroupChatUx.mentionQuery("Эй @Ан", 6)
        assertEquals(3, mid?.atIndex)
        assertEquals("Ан", mid?.query)
        assertEquals("Аня", GroupChatUx.mentionQuery("Эй @Аня", 7)?.query)
        assertEquals(null, GroupChatUx.mentionQuery("foo@bar", 7))
        assertEquals(null, GroupChatUx.mentionQuery("Эй @Аня ещё", 11))
        assertEquals(null, GroupChatUx.mentionQuery("@Аня", 0))
    }

    @Test
    fun mentionSuggestionsPreferPrefixThenContains() {
        val names = listOf("Аня", "Боря", "Анна", "Иван")
        assertEquals(listOf("Аня", "Анна"), GroupChatUx.mentionSuggestions("ан", names))
        assertEquals(listOf("Боря"), GroupChatUx.mentionSuggestions("бор", names))
        assertEquals(listOf("Иван"), GroupChatUx.mentionSuggestions("ван", names))
        assertEquals(names.take(GroupChatUx.MENTION_LIMIT), GroupChatUx.mentionSuggestions("", names))
        assertTrue(GroupChatUx.mentionSuggestions("xyz", names).isEmpty())
        val many = (1..12).map { "N$it" }
        assertEquals(GroupChatUx.MENTION_LIMIT, GroupChatUx.mentionSuggestions("", many).size)
    }

    @Test
    fun applyMentionReplacesQueryAndAddsSpace() {
        val (text, cursor) = GroupChatUx.applyMention("Эй @Ан", 6, "Аня")
        assertEquals("Эй @Аня ", text)
        assertEquals(text.length, cursor)
        val (again, _) = GroupChatUx.applyMention("просто текст", 5, "Аня")
        assertEquals("просто текст", again)
        val (lead, leadCur) = GroupChatUx.applyMention("@", 1, "Боря")
        assertEquals("@Боря ", lead)
        assertEquals(6, leadCur)
    }

    @Test
    fun mentionCandidatesUseGroupMembersNotWholeDirectory() {
        val names = mapOf("d1" to "Аня", "d2" to "Боря", "d9" to "Чужак")
        val hits = GroupChatUx.mentionCandidates(
            memberIds = listOf("d1", "me", "d2"),
            namesById = names,
            myId = "me",
            myName = "Вика",
        )
        assertEquals(listOf("Аня", "Вика", "Боря"), hits)
        assertFalse(hits.contains("Чужак"))
        val selfBlank = GroupChatUx.mentionCandidates(listOf("me"), emptyMap(), "me", "")
        assertEquals(listOf("Вы"), selfBlank)
    }

    @Test
    fun rolesAndManageFollowOrganizer() {
        val g = RopeGroup("g1", "Команда", 1, listOf("org", "mem"), createdBy = "org")
        assertEquals("организатор", GroupChatUx.memberRoleLabel("org", g))
        assertEquals("участник", GroupChatUx.memberRoleLabel("mem", g))
        assertEquals("Вы", GroupChatUx.memberDisplayName("mem", "mem", mapOf("org" to "Оля", "mem" to "Я")))
        assertTrue(RoleRules.canManageGroupMembers(true, "org", "org", "guest"))
        assertFalse(RoleRules.canManageGroupMembers(true, "mem", "org", "guest"))
        assertTrue(RoleRules.canManageGroupMembers(true, "mem", "org", "owner"))
        assertFalse(RoleRules.canManageGroupMembers(false, "org", "org", "owner"))
        assertTrue(RoleRules.canLeaveGroup(true))
        assertFalse(RoleRules.canLeaveGroup(false))
    }

    @Test
    fun senderColorsAreStableAndDistinctEnough() {
        val a = GroupChatUx.senderColorArgb("dev-a", "Аня")
        val b = GroupChatUx.senderColorArgb("dev-b", "Боря")
        assertEquals(a, GroupChatUx.senderColorArgb("dev-a", "Аня"))
        assertTrue(GroupChatUx.SENDER_COLORS.contains(a))
        assertTrue(GroupChatUx.SENDER_COLORS.contains(b))
        assertNotEquals(0, a)
    }

    @Test
    fun replyQuoteShowsWho() {
        assertEquals("Аня", GroupChatUx.replyQuoteName("Аня", outgoing = false))
        assertEquals("Вы", GroupChatUx.replyQuoteName("", outgoing = true))
        assertEquals("Ответ", GroupChatUx.replyQuoteName("", outgoing = false))
    }
}
