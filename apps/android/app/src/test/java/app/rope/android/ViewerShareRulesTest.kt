package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ViewerShareRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ViewerShareRulesTest {
    private fun media(
        kind: MessageKind = MessageKind.IMAGE,
        path: String? = "/media/x.jpg",
        deleted: Boolean = false,
    ) = ChatMessage(
        id = "1",
        peerDeviceId = "p",
        outgoing = true,
        text = "",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        localPath = path,
        deleted = deleted,
    )

    @Test
    fun canShareLivePhotoAndVideo() {
        assertEquals("Поделиться", ViewerShareRules.ACTION)
        assertEquals("Поделиться", ViewerShareRules.CHOOSER)
        assertEquals("android.intent.action.SEND", ViewerShareRules.SEND_ACTION)
        assertEquals("android.intent.extra.STREAM", ViewerShareRules.EXTRA_STREAM)
        assertEquals("app.rope.android.files", ViewerShareRules.authority("app.rope.android"))
        assertTrue(ViewerShareRules.canShare(media(MessageKind.IMAGE)))
        assertTrue(ViewerShareRules.canShare(media(MessageKind.VIDEO, "/media/x.mp4")))
        assertFalse(ViewerShareRules.canShare(media(deleted = true)))
        assertFalse(ViewerShareRules.canShare(media(path = null)))
        assertFalse(ViewerShareRules.canShare(media(path = "  ")))
        assertFalse(ViewerShareRules.canShare(media(MessageKind.VIDEO_NOTE, "/media/x.mp4")))
        assertFalse(ViewerShareRules.canShare(media(MessageKind.TEXT, path = "/media/x.jpg")))
        val photo = media()
        val video = media(kind = MessageKind.VIDEO, path = "/media/b.mp4")
        assertEquals(video, ViewerShareRules.current(listOf(photo, video), 1, photo))
        assertEquals(photo, ViewerShareRules.current(emptyList(), 9, photo))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun allowOnlyBytesUnderMedia() {
        val root = createTempDirectory("rope-viewer-share").toFile()
        try {
            val dir = File(root, ViewerShareRules.MEDIA_DIR).apply { mkdirs() }
            val jpg = File(dir, "obj-1.jpg").apply { writeBytes(ByteArray(ViewerShareRules.MIN_BYTES.toInt() + 8)) }
            val mp4 = File(dir, "clip.mp4").apply { writeBytes(ByteArray(ViewerShareRules.MIN_BYTES.toInt() + 8)) }
            assertTrue(ViewerShareRules.allow(jpg, dir))
            assertTrue(ViewerShareRules.allow(mp4, dir))
            assertEquals("image/jpeg", ViewerShareRules.mimeFor(jpg))
            assertEquals("image/png", ViewerShareRules.mimeFor(File(dir, "a.png")))
            assertEquals("video/mp4", ViewerShareRules.mimeFor(mp4))
            assertEquals("video/webm", ViewerShareRules.mimeFor(File(dir, "a.webm")))
            assertEquals(jpg, ViewerShareRules.shareFile(media(path = jpg.absolutePath), dir))
            assertEquals(mp4, ViewerShareRules.shareFile(media(MessageKind.VIDEO, mp4.absolutePath), dir))

            val tiny = File(dir, "tiny.jpg").apply { writeBytes(ByteArray(4)) }
            assertFalse(ViewerShareRules.allow(tiny, dir))

            val outside = File(root, "identity.ropi.enc").apply {
                writeBytes(ByteArray(ViewerShareRules.MIN_BYTES.toInt() + 8))
            }
            assertFalse(ViewerShareRules.allow(outside, dir))
            assertNull(ViewerShareRules.shareFile(media(path = outside.absolutePath), dir))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectCrLfNameAndSymlinkEscape() {
        val root = createTempDirectory("rope-viewer-share-escape").toFile()
        try {
            val dir = File(root, ViewerShareRules.MEDIA_DIR).apply { mkdirs() }
            val outside = File(root, "secret.jpg").apply {
                writeBytes(ByteArray(ViewerShareRules.MIN_BYTES.toInt() + 8))
            }
            val link = File(dir, "obj.jpg")
            java.nio.file.Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertFalse(ViewerShareRules.allow(link, dir))

            val nl = File(dir, "obj.jpg\n")
            nl.writeBytes(ByteArray(ViewerShareRules.MIN_BYTES.toInt() + 8))
            assertFalse(ViewerShareRules.allow(nl, dir))
        } finally {
            root.deleteRecursively()
        }
    }
}
