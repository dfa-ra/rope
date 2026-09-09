package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatControl
import app.rope.android.data.ChatMessage
import app.rope.android.data.EnvelopeTypes
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.PollReceipt
import app.rope.android.data.PollRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PollRulesTest {
    @Test
    fun questionAndTwoOptionsOk() {
        assertTrue(PollRules.validate("Куда на обед?", listOf("Пицца", "Суши")))
        assertTrue(PollRules.validate("  hi  ", listOf(" a ", "b")))
    }

    @Test
    fun blankQuestionRejected() {
        assertFalse(PollRules.validate("   ", listOf("a", "b")))
        assertFalse(PollRules.validate("", listOf("a", "b")))
    }

    @Test
    fun oneOptionRejected() {
        assertFalse(PollRules.validate("q", listOf("only")))
        assertFalse(PollRules.validate("q", listOf("a", "  ")))
    }

    @Test
    fun elevenOptionsRejected() {
        val opts = (1..11).map { "opt$it" }
        assertFalse(PollRules.validate("q", opts))
        assertTrue(PollRules.validate("q", (1..10).map { "opt$it" }))
    }

    @Test
    fun namedVotesHaveNoAnonymousFlag() {
        assertFalse(PollRules.ANONYMOUS)
    }

    @Test
    fun singleTapSameIndexClears() {
        assertEquals(listOf(0), PollRules.nextIx(false, emptyList(), 0))
        assertEquals(emptyList<Int>(), PollRules.nextIx(false, listOf(1), 1))
        assertEquals(listOf(1), PollRules.nextIx(false, listOf(0), 1))
    }

    @Test
    fun multiToggleKeepsFullIx() {
        assertEquals(listOf(0, 2), PollRules.nextIx(true, listOf(0), 2))
        assertEquals(listOf(0), PollRules.nextIx(true, listOf(0, 2), 2))
        assertEquals(listOf(1, 2), PollRules.nextIx(true, listOf(2), 1))
    }

    @Test
    fun lastWriteWinsPerVoter() {
        val receipts = listOf(
            vote("a", listOf(0), 1, "r1"),
            vote("a", listOf(1), 2, "r2"),
            vote("b", listOf(0), 3, "r3"),
        )
        val state = apply(receipts)
        assertEquals(listOf(1, 1), state.counts)
        assertEquals(2, state.totalVoters)
        assertFalse(state.closed)
    }

    @Test
    fun closeIgnoresLaterVotes() {
        val receipts = listOf(
            vote("a", listOf(0), 1, "r1"),
            PollReceipt("c1", "q1", "author", "A", ChatControl.POLL_CLOSE, ChatControl.OP_SET, emptyList(), 2),
            vote("a", listOf(1), 3, "r2"),
            vote("b", listOf(1), 4, "r3"),
        )
        val state = apply(receipts)
        assertTrue(state.closed)
        assertEquals(listOf(1, 0), state.counts)
        assertEquals(1, state.totalVoters)
    }

    @Test
    fun orphanVoteAppliesWhenCreateArrives() {
        val receipts = listOf(vote("a", listOf(1), 10, "early"))
        val state = apply(receipts, question = "later", options = listOf("x", "y"))
        assertEquals(listOf(0, 1), state.counts)
        assertEquals(1, state.totalVoters)
    }

    @Test
    fun guestCannotCloseOthersPoll() {
        assertFalse(PollRules.canClose("guest", "author", "org", isOwner = false, isMember = false))
        assertFalse(PollRules.canClose("guest", "author", "org", isOwner = false, isMember = true))
        val ignored = apply(
            listOf(
                PollReceipt("c1", "q1", "guest", "G", ChatControl.POLL_CLOSE, ChatControl.OP_SET, emptyList(), 1),
                vote("a", listOf(0), 2, "r1"),
            ),
        )
        assertFalse(ignored.closed)
        assertEquals(listOf(1, 0), ignored.counts)
    }

    @Test
    fun authorOrOrganizerOrOwnerCanClose() {
        assertTrue(PollRules.canClose("author", "author", "org", isOwner = false, isMember = true))
        assertTrue(PollRules.canClose("org", "author", "org", isOwner = false, isMember = true))
        assertTrue(PollRules.canClose("own", "author", "org", isOwner = true, isMember = true))
        val byOrg = apply(
            listOf(PollReceipt("c1", "q1", "org", "O", ChatControl.POLL_CLOSE, ChatControl.OP_SET, emptyList(), 1)),
        )
        assertTrue(byOrg.closed)
        val byOwner = apply(
            listOf(PollReceipt("c2", "q1", "own", "W", ChatControl.POLL_CLOSE, ChatControl.OP_SET, emptyList(), 1)),
            ownerIds = setOf("own"),
        )
        assertTrue(byOwner.closed)
    }

    @Test
    fun canForwardPollIsFalse() {
        val poll = ChatMessage(
            "q1",
            "g:g1",
            true,
            "Опрос: обед",
            MessageStatus.SENT_TO_SERVER,
            1L,
            kind = MessageKind.POLL,
        )
        val text = poll.copy(id = "t", kind = MessageKind.TEXT, text = "hi")
        assertFalse(ChatActions.canForward(poll))
        assertTrue(ChatActions.canForward(text))
        assertTrue(ChatActions.canReply(poll))
        assertFalse(ChatActions.canForward(text.copy(deleted = true)))
    }

    @Test
    fun chatControlParsesVoteAndPollClose() {
        val vote = ChatControl.parse(
            ChatControl(ChatControl.VOTE, "q1", op = ChatControl.OP_SET, indexes = listOf(1)).toJson(),
        )!!
        assertEquals(ChatControl.VOTE, vote.kind)
        assertEquals("q1", vote.targetId)
        assertEquals(listOf(1), vote.indexes)
        val close = ChatControl.parse(
            ChatControl(ChatControl.POLL_CLOSE, "q1", op = ChatControl.OP_SET).toJson(),
        )!!
        assertEquals(ChatControl.POLL_CLOSE, close.kind)
        assertEquals("q1", close.targetId)
        assertNull(ChatControl.parse("""{"kind":"other","target":"x"}"""))
        assertNull(ChatControl.parse("""{"kind":"quiz","target":"q1"}"""))
    }

    @Test
    fun enterChatHydrateUsesStoredReceiptsNotEmptyDefault() {
        val stored = listOf(
            vote("a", listOf(0), 1, "r1"),
            vote("b", listOf(1), 2, "r2"),
        )
        assertEquals(emptyList<PollReceipt>(), UiState().pollReceipts)
        assertEquals("0 голосов · видны", PollRules.footer(apply(emptyList()).totalVoters, false))
        val opened = UiState().copy(pollReceipts = stored)
        val hydrated = apply(opened.pollReceipts)
        assertEquals(2, hydrated.totalVoters)
        assertEquals(listOf(1, 1), hydrated.counts)
        assertEquals("2 голосов · видны", PollRules.footer(hydrated.totalVoters, false))
    }

    @Test
    fun mediaPayloadPollOmitsObjectAndPreviews() {
        val raw = """{"kind":"poll","g":"gid","q":"Куда на обед?","o":["Пицца","Суши"],"m":false,"qzid":"q-1"}"""
        val p = MediaPayload.parse(raw)
        assertEquals(MessageKind.POLL, p.messageKind())
        assertEquals("", p.objectId)
        assertEquals("gid", p.groupId)
        assertEquals("Куда на обед?", p.pollQuestion)
        assertEquals(listOf("Пицца", "Суши"), p.pollOptions)
        assertFalse(p.pollMulti)
        assertEquals("q-1", p.pollQzid)
        assertTrue(p.preview().startsWith("Опрос:"))
        val json = p.toJson()
        assertFalse(json.contains("object_id"))
        assertEquals(MessageKind.POLL, EnvelopeTypes.kindOf(EnvelopeTypes.MEDIA, json))
        assertTrue(PollRules.canCreate(inGroup = true, isMember = true))
        assertFalse(PollRules.canCreate(inGroup = false, isMember = true))
        assertFalse(PollRules.canCreate(inGroup = true, isMember = false))
    }

    private fun vote(voter: String, ix: List<Int>, ts: Long, id: String) = PollReceipt(
        id,
        "q1",
        voter,
        voter,
        ChatControl.VOTE,
        if (ix.isEmpty()) ChatControl.OP_CLEAR else ChatControl.OP_SET,
        ix,
        ts,
    )

    private fun apply(
        receipts: List<PollReceipt>,
        question: String = "q",
        options: List<String> = listOf("Пицца", "Суши"),
        ownerIds: Set<String> = emptySet(),
    ) = PollRules.apply(
        question,
        options,
        multi = false,
        qzid = "q1",
        receipts = receipts,
        selfId = "me",
        authorId = "author",
        organizerId = "org",
        ownerIds = ownerIds,
    )
}
