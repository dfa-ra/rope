package app.rope.android

import app.rope.android.data.GroupTextPayload
import app.rope.android.data.MediaPayload
import app.rope.android.data.WireIds
import app.rope.android.media.ImageCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaWireIdsTest {
    @Test
    fun parseRejectsControlBeforeTrim() {
        assertEquals("g1", WireIds.parse("  g1  "))
        assertEquals("obj-1", WireIds.parse("obj-1"))
        assertNull(WireIds.parse("g1\n"))
        assertNull(WireIds.parse("\nobj"))
        assertNull(WireIds.parse("id\r"))
        assertNull(WireIds.parse("id\u0000"))
        assertNull(WireIds.parse("null"))
        assertNull(WireIds.parse(""))
        assertNull(WireIds.parse(null))
    }

    @Test
    fun mediaPayloadGroupAndObjectRejectControl() {
        val gid = "11111111-2222-3333-4444-555555555555"
        val clean = MediaPayload.parse(
            """{"kind":"image","object_id":"o","sha256":"s","key_b64":"k","mime":"image/jpeg","name":"p.jpg","size":1,"group_id":"$gid"}""",
        )
        assertEquals("o", clean.objectId)
        assertEquals(gid, clean.groupId)
        val dirtyGroup = MediaPayload.parse(
            """{"kind":"image","object_id":"o","sha256":"s","key_b64":"k","mime":"image/jpeg","name":"p.jpg","size":1,"group_id":"$gid\n"}""",
        )
        assertNull(dirtyGroup.groupId)
        val dirtyObject = MediaPayload.parse(
            """{"kind":"image","object_id":"o\n","sha256":"s","key_b64":"k","mime":"image/jpeg","name":"p.jpg","size":1}""",
        )
        assertEquals("", dirtyObject.objectId)
        val caption = MediaPayload.parse(
            """{"kind":"image","object_id":"o","sha256":"s","key_b64":"k","mime":"image/jpeg","name":"p.jpg","size":1,"caption":"hi\nthere"}""",
        )
        assertEquals("hi\nthere", caption.caption)
        val reply = MediaPayload.parse(
            """{"kind":"image","object_id":"o","sha256":"s","key_b64":"k","mime":"image/jpeg","name":"p.jpg","size":1,"r":"m1\n"}""",
        )
        assertNull(reply.replyTo)
    }

    @Test
    fun groupTextGroupIdRejectsControl() {
        val gid = "11111111-2222-3333-4444-555555555555"
        val clean = GroupTextPayload.parse("""{"g":"$gid","t":"hi","e":1}""")
        assertEquals(gid, clean.groupId)
        val dirty = GroupTextPayload.parse("""{"g":"$gid\n","t":"hi","e":1}""")
        assertEquals("", dirty.groupId)
        val text = GroupTextPayload.parse("""{"g":"$gid","t":"line1\nline2","e":1}""")
        assertEquals("line1\nline2", text.text)
    }

    @Test
    fun mediaFileNameDoesNotStripNewlineIntoLiveId() {
        assertEquals("abc.jpg", ImageCodec.fileName("abc", "image/jpeg", "x"))
        val poisoned = ImageCodec.fileName("abc\n", "image/jpeg", "x")
        assertTrue(poisoned.startsWith("rejected."))
        assertTrue(!poisoned.startsWith("abc."))
    }
}
