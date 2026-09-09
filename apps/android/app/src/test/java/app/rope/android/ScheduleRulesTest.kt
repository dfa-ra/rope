package app.rope.android

import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.ScheduleRules
import app.rope.android.data.SilentFlag
import app.rope.android.data.StagedPart
import app.rope.android.data.TextBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ScheduleRulesTest {
    @Test
    fun windowRejectsTooSoonAndTooFar() {
        val now = 1_700_000_000_000L
        assertFalse(ScheduleRules.inWindow(now, now))
        assertFalse(ScheduleRules.inWindow(now, now + 5_000))
        assertFalse(ScheduleRules.inWindow(now, now + 59_000))
        assertTrue(ScheduleRules.inWindow(now, now + ScheduleRules.MIN_DELAY_MS))
        assertTrue(ScheduleRules.inWindow(now, now + ScheduleRules.MAX_DELAY_MS))
        assertFalse(ScheduleRules.inWindow(now, now + ScheduleRules.MAX_DELAY_MS + 1))
        assertFalse(ScheduleRules.inWindow(now, now - 1_000))
    }

    @Test
    fun capStopsAtOneHundred() {
        assertTrue(ScheduleRules.canEnqueue(0))
        assertTrue(ScheduleRules.canEnqueue(99))
        assertFalse(ScheduleRules.canEnqueue(100))
        assertFalse(ScheduleRules.canEnqueue(101))
    }

    @Test
    fun ttlAtFireUsesLivePrefNotStoredExp() {
        assertEquals(0, ScheduleRules.ttlAtFire(0, storedExpMs = 9_999_999_999L))
        assertEquals(86_400, ScheduleRules.ttlAtFire(86_400, storedExpMs = 1L))
        assertEquals(0, ScheduleRules.ttlAtFire(-5, storedExpMs = 1L))
    }

    @Test
    fun savedNeverUsesNetworkAtFire() {
        assertFalse(ScheduleRules.usesNetwork(SavedMessagesRules.ID))
        assertTrue(SavedMessagesRules.skipNetwork(SavedMessagesRules.ID))
        assertTrue(ScheduleRules.usesNetwork("device-1"))
        assertTrue(ScheduleRules.usesNetwork("g:gid"))
    }

    @Test
    fun overdueAfterKillIsStillDue() {
        val now = 1_700_000_000_000L
        assertTrue(ScheduleRules.isDue(now, now))
        assertTrue(ScheduleRules.isDue(now, now - 60_000))
        assertFalse(ScheduleRules.isDue(now, now + 1))
        assertEquals(ScheduleRules.OVERDUE, ScheduleRules.listTimeLabel(now, now - 1))
    }

    @Test
    fun unlinkStagedRemovesCopiedBytes() {
        val dir = File.createTempFile("sched", "dir").apply {
            delete()
            mkdirs()
        }
        File(dir, "part_0").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(dir, "manifest.json").writeText("{}")
        assertTrue(dir.exists())
        assertTrue(ScheduleRules.unlinkStaged(dir))
        assertFalse(dir.exists())
        assertTrue(ScheduleRules.unlinkStaged(dir))
    }

    @Test
    fun manifestRoundtrip() {
        val dir = File.createTempFile("schedm", "dir").apply {
            delete()
            mkdirs()
        }
        try {
            ScheduleRules.writeManifest(
                dir,
                listOf(StagedPart("part_0", "image", "a.jpg", "image/jpeg", 0)),
            )
            val parts = ScheduleRules.readManifest(dir)
            assertEquals(1, parts.size)
            assertEquals("image", parts[0].kind)
            assertEquals("a.jpg", parts[0].name)
        } finally {
            ScheduleRules.unlinkStaged(dir)
        }
    }
}

class SilentPackTest {
    @Test
    fun encodeWithNsIsJson() {
        val packed = TextBody.encode("привет", null, "", "", silent = true)
        assertTrue(packed.startsWith("{"))
        assertTrue(packed.contains("\"ns\":1"))
        val body = TextBody.decode(packed)
        assertEquals("привет", body.text)
        assertTrue(body.silent)
    }

    @Test
    fun encodeWithoutFlagsStaysRaw() {
        assertEquals("привет", TextBody.encode("привет", null, "", ""))
        assertFalse(TextBody.decode("привет").silent)
    }

    @Test
    fun silentFlagOmitsWhenFalse() {
        val o = JSONObject().put("t", "x")
        SilentFlag.put(o, false)
        assertFalse(o.has("ns"))
        SilentFlag.put(o, true)
        assertEquals(1, o.getInt("ns"))
        assertTrue(SilentFlag.read(o))
    }

    @Test
    fun mediaAndGroupPackNs() {
        val media = app.rope.android.data.MediaPayload(
            kind = "image",
            objectId = "o",
            sha256 = "ab",
            keyB64 = "k",
            mime = "image/jpeg",
            name = "a.jpg",
            size = 1,
            silent = true,
        ).toJson()
        assertTrue(media.contains("\"ns\":1"))
        assertTrue(app.rope.android.data.MediaPayload.parse(media).silent)
        val group = app.rope.android.data.GroupTextPayload("g", "hi", 1, silent = true).toJson()
        assertTrue(group.contains("\"ns\":1"))
        assertTrue(app.rope.android.data.GroupTextPayload.parse(group).silent)
        val quiet = app.rope.android.data.GroupTextPayload("g", "hi", 1, silent = false).toJson()
        assertFalse(quiet.contains("\"ns\""))
    }
}
