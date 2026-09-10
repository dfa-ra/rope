package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import app.rope.android.data.PinListRules
import app.rope.android.data.ReactionPayload
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinListRulesTest {
    @Test
    fun toggleAddsSecondWithoutReplacingLegacyId() {
        val first = PinListRules.toggle(emptyList(), "m1")
        assertEquals(listOf("m1"), first)
        val both = PinListRules.toggle(first, "m2")
        assertEquals(listOf("m1", "m2"), both)
        assertEquals("m2", PinListRules.latest(both))
        assertTrue(PinListRules.contains(both, "m1"))
        val dropped = PinListRules.toggle(both, "m1")
        assertEquals(listOf("m2"), dropped)
        assertFalse(PinListRules.contains(dropped, "m1"))
        assertEquals(emptyList<String>(), PinListRules.toggle(listOf("m2"), "m2"))
        assertEquals(listOf("m1"), PinListRules.toggle(listOf("m1"), "  "))
        assertEquals(listOf("m1"), PinListRules.toggle(listOf("m1"), null))
    }

    @Test
    fun remoteSetAppendsAndClearRemovesOnlyTarget() {
        val ids = listOf("m1", "m2")
        assertEquals(listOf("m1", "m2", "m3"), PinListRules.applyRemote(ids, "m3", clear = false))
        assertEquals(listOf("m1", "m3"), PinListRules.applyRemote(ids, "m2", clear = true))
        assertEquals(ids, PinListRules.applyRemote(ids, "  ", clear = false))
        val moved = PinListRules.applyRemote(ids, "m1", clear = false)
        assertEquals(listOf("m2", "m1"), moved)
        assertEquals(ReactionPayload.CLEAR, "clear")
        assertEquals(ReactionPayload.SET, "set")
    }

    @Test
    fun visibleSkipsDeletedAndBarCyclesOldestToNewest() {
        val messages = listOf(
            msg("m1", "один"),
            msg("m2", "два", deleted = true),
            msg("m3", "три"),
        )
        val vis = PinListRules.visible(messages, listOf("m1", "m2", "m3"))
        assertEquals(listOf("m1", "m3"), vis.map { it.id })
        assertEquals("m3", PinListRules.barMessage(vis, null)?.id)
        assertEquals("m1", PinListRules.barMessage(vis, "m1")?.id)
        assertEquals("m3", PinListRules.barMessage(vis, "gone")?.id)
        assertEquals("m3", PinListRules.nextShown(listOf("m1", "m3"), "m1"))
        assertEquals("m1", PinListRules.nextShown(listOf("m1", "m3"), "m3"))
        assertEquals("m3", PinListRules.nextShown(listOf("m1", "m3"), "gone"))
        assertEquals("m1", PinListRules.nextShown(listOf("m1"), "m1"))
        assertNull(PinListRules.nextShown(emptyList(), "m1"))
        assertNull(PinListRules.barMessage(emptyList(), "m1"))
    }

    @Test
    fun barLabelAndUnpinAllNeedTwo() {
        assertEquals(PinListRules.LABEL_ONE, PinListRules.barLabel(0))
        assertEquals(PinListRules.LABEL_ONE, PinListRules.barLabel(1))
        assertEquals("3 закреплённых", PinListRules.barLabel(3))
        assertFalse(PinListRules.showList(1))
        assertTrue(PinListRules.showList(2))
        assertFalse(PinListRules.showUnpinAll(1))
        assertTrue(PinListRules.showUnpinAll(2))
        assertEquals("Открепить все", PinListRules.UNPIN_ALL)
        assertEquals("Закреплённые", PinListRules.LIST_TITLE)
    }

    @Test
    fun prefsRoundTripWritesArrayAndReadsLegacySingle() {
        val written = ChatPrefs.parse(
            ChatPrefs(pinnedMessageId = "m1", pinnedMessageIds = listOf("m1", "m2")).toJson(),
        )
        assertEquals(listOf("m1", "m2"), written.pinnedMessageIds)
        assertEquals("m2", written.pinnedMessageId)
        val o = JSONObject(written.toJson())
        assertEquals("m2", o.getString("pinned_message"))
        assertEquals(2, o.getJSONArray("pinned_messages").length())
        val legacy = JSONObject()
            .put("pinned_message", "old")
            .toString()
        val parsed = ChatPrefs.parse(legacy)
        assertEquals(listOf("old"), parsed.pinnedMessageIds)
        assertEquals("old", parsed.pinnedMessageId)
        val emptyArr = JSONObject()
            .put("pinned_message", "stale")
            .put("pinned_messages", JSONArray())
            .toString()
        val fallback = ChatPrefs.parse(emptyArr)
        assertEquals(listOf("stale"), fallback.pinnedMessageIds)
        val cleared = ChatPrefs.parse(PinListRules.withPins(ChatPrefs(pinnedMessageId = "m1"), emptyList()).toJson())
        assertTrue(cleared.pinnedMessageIds.isEmpty())
        assertNull(cleared.pinnedMessageId)
        val stored = PinListRules.stored(emptyList(), "m9")
        assertEquals(listOf("m9"), stored)
        val fromIds = PinListRules.stored(listOf("a", "b"), "ignored")
        assertEquals(listOf("a", "b"), fromIds)
    }

    @Test
    fun capDropsOldestAndIgnoresBlank() {
        val many = (1..PinListRules.MAX).map { "m$it" }
        val overflow = PinListRules.toggle(many, "new")
        assertEquals(PinListRules.MAX, overflow.size)
        assertEquals("m2", overflow.first())
        assertEquals("new", overflow.last())
        assertFalse(overflow.contains("m1"))
        assertEquals(emptyList<String>(), PinListRules.normalize(listOf(" ", "null", null, "")))
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg(id: String, text: String, deleted: Boolean = false) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        deleted = deleted,
    )
}
