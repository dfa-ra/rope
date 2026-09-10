package app.rope.android

import app.rope.android.data.GroupTextPayload
import app.rope.android.data.LocalStore
import app.rope.android.data.SpoilerRules
import app.rope.android.data.TextBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoilerRulesTest {
    @Test
    fun stripsMarksAndRecordsRanges() {
        val (plain, ranges) = SpoilerRules.parse("привет ||секрет|| конец")
        assertEquals("привет секрет конец", plain)
        assertEquals(listOf(7 until 13), ranges)
        assertEquals("секрет", plain.substring(ranges[0]))
        val none = SpoilerRules.parse("без спойлера")
        assertEquals("без спойлера", none.first)
        assertTrue(none.second.isEmpty())
        val (two, both) = SpoilerRules.parse("||a|| и ||bb||")
        assertEquals("a и bb", two)
        assertEquals(2, both.size)
        assertTrue(SpoilerRules.covers(both, 0))
        assertFalse(SpoilerRules.covers(both, 2))
        assertEquals("привет ||секрет|| конец", SpoilerRules.wrap(plain, ranges))
    }

    @Test
    fun innerJsonRoundtrip() {
        val o = JSONObject().put("t", "привет секрет")
        SpoilerRules.put(o, listOf(7 until 13))
        val got = SpoilerRules.read(o)
        assertEquals(listOf(7 until 13), got)
        val packed = TextBody.encode("привет секрет", null, "", "", spoilers = listOf(7 until 13))
        val body = TextBody.decode(packed)
        assertEquals("привет секрет", body.text)
        assertEquals(listOf(7 until 13), body.spoilers)
        assertEquals("plain", TextBody.encode("plain", null, "", ""))
        val group = GroupTextPayload(
            "g1",
            "привет секрет",
            1,
            spoilers = listOf(7 until 13),
        )
        val parsed = GroupTextPayload.parse(group.toJson())
        assertEquals(listOf(7 until 13), parsed.spoilers)
        assertEquals("привет секрет", parsed.text)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("||", SpoilerRules.MARK)
    }
}
