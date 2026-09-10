package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ShareMediaRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ShareMediaRulesTest {
    private fun media(
        kind: MessageKind,
        path: String? = "/media/x.bin",
        extra: String = "",
        deleted: Boolean = false,
        text: String = "",
    ) = ChatMessage(
        id = "1",
        peerDeviceId = "p",
        outgoing = true,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        extra = extra,
        localPath = path,
        deleted = deleted,
    )

    @Test
    fun canShareDownloadedImageVideoFile() {
        assertTrue(ShareMediaRules.canShare(media(MessageKind.IMAGE)))
        assertTrue(ShareMediaRules.canShare(media(MessageKind.VIDEO)))
        assertTrue(ShareMediaRules.canShare(media(MessageKind.FILE)))
        assertFalse(ShareMediaRules.canShare(media(MessageKind.IMAGE, deleted = true)))
        assertFalse(ShareMediaRules.canShare(media(MessageKind.IMAGE, path = null)))
        assertFalse(ShareMediaRules.canShare(media(MessageKind.TEXT, path = "/media/x.jpg")))
        assertFalse(ShareMediaRules.canShare(media(MessageKind.VOICE, path = "/media/x.m4a")))
        assertFalse(ShareMediaRules.canShare(media(MessageKind.VIDEO_NOTE, path = "/media/x.mp4")))
        assertEquals("Поделиться", ShareMediaRules.ACTION)
        assertEquals("Поделиться", ShareMediaRules.CHOOSER)
        assertEquals("android.intent.action.SEND", ShareMediaRules.SEND_ACTION)
        assertEquals("android.intent.extra.STREAM", ShareMediaRules.EXTRA_STREAM)
        assertEquals("app.rope.android.files", ShareMediaRules.authority("app.rope.android"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun mimePrefersPayloadThenExtension() {
        val png = MediaPayload("image", "o", "ab", "K", "image/png", "a.png", 12).toJson()
        val dirty = """{"kind":"image","object_id":"o","sha256":"ab","key_b64":"K","mime":"image/png\n","name":"a.png","size":12}"""
        val img = File("a.jpg")
        assertEquals("image/png", ShareMediaRules.mimeFor(media(MessageKind.IMAGE, extra = png), img))
        assertEquals("image/jpeg", ShareMediaRules.mimeFor(media(MessageKind.IMAGE, extra = dirty), img))
        assertEquals("image/jpeg", ShareMediaRules.mimeFor(media(MessageKind.IMAGE), img))
        assertEquals("image/webp", ShareMediaRules.mimeFor(media(MessageKind.IMAGE), File("a.webp")))
        assertEquals("video/mp4", ShareMediaRules.mimeFor(media(MessageKind.VIDEO), File("a.mp4")))
        assertEquals("application/octet-stream", ShareMediaRules.mimeFor(media(MessageKind.FILE), File("a.bin")))
    }

    @Test
    fun allowOnlyBytesUnderMedia() {
        val root = createTempDirectory("rope-share-media").toFile()
        try {
            val mediaDir = File(root, ShareMediaRules.MEDIA_DIR).apply { mkdirs() }
            val ok = File(mediaDir, "clip.mp4")
            ok.writeBytes(ByteArray(ShareMediaRules.MIN_BYTES.toInt() + 8))
            val video = media(MessageKind.VIDEO, path = ok.absolutePath)
            assertTrue(ShareMediaRules.allow(ok, mediaDir))
            assertEquals(ok, ShareMediaRules.shareFile(video, mediaDir))

            val small = File(mediaDir, "tiny.bin")
            small.writeBytes(ByteArray(4))
            assertFalse(ShareMediaRules.allow(small, mediaDir))

            val outside = File(root, "identity.ropi.enc")
            outside.writeBytes(ByteArray(ShareMediaRules.MIN_BYTES.toInt() + 8))
            assertFalse(ShareMediaRules.allow(outside, mediaDir))
            assertNull(ShareMediaRules.shareFile(media(MessageKind.FILE, path = outside.absolutePath), mediaDir))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectCrLfNameAndSymlinkEscape() {
        val root = createTempDirectory("rope-share-media-escape").toFile()
        try {
            val mediaDir = File(root, ShareMediaRules.MEDIA_DIR).apply { mkdirs() }
            val outside = File(root, "secret.bin")
            outside.writeBytes(ByteArray(ShareMediaRules.MIN_BYTES.toInt() + 8))
            val link = File(mediaDir, "obj.bin")
            java.nio.file.Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertFalse(ShareMediaRules.allow(link, mediaDir))

            val nl = File(mediaDir, "obj.bin\n")
            nl.writeBytes(ByteArray(ShareMediaRules.MIN_BYTES.toInt() + 8))
            assertFalse(ShareMediaRules.allow(nl, mediaDir))
        } finally {
            root.deleteRecursively()
        }
    }
}
