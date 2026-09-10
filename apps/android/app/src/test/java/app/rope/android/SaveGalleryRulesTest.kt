package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SaveGalleryRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SaveGalleryRulesTest {
    private fun media(
        kind: MessageKind,
        path: String? = "/media/x.jpg",
        extra: String = "",
        deleted: Boolean = false,
    ) = ChatMessage(
        id = "1",
        peerDeviceId = "p",
        outgoing = true,
        text = "",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        extra = extra,
        localPath = path,
        deleted = deleted,
    )

    @Test
    fun canSaveDownloadedPhotoAndVideoNotFile() {
        assertTrue(SaveGalleryRules.canSave(media(MessageKind.IMAGE)))
        assertTrue(SaveGalleryRules.canSave(media(MessageKind.VIDEO)))
        assertFalse(SaveGalleryRules.canSave(media(MessageKind.FILE)))
        assertFalse(SaveGalleryRules.canSave(media(MessageKind.IMAGE, deleted = true)))
        assertFalse(SaveGalleryRules.canSave(media(MessageKind.IMAGE, path = null)))
        assertFalse(SaveGalleryRules.canSave(media(MessageKind.TEXT, path = "/media/x.jpg")))
        assertEquals("Сохранить", SaveGalleryRules.ACTION)
        assertEquals("Сохранено в галерею", SaveGalleryRules.NOTICE)
        assertEquals(SaveGalleryRules.PICTURES, SaveGalleryRules.relativePath(false))
        assertEquals(SaveGalleryRules.MOVIES, SaveGalleryRules.relativePath(true))
        assertEquals(SaveGalleryRules.PICTURES, SaveGalleryRules.relativePath(media(MessageKind.IMAGE)))
        assertEquals(SaveGalleryRules.MOVIES, SaveGalleryRules.relativePath(media(MessageKind.VIDEO)))
        assertFalse(SaveGalleryRules.isVideo(media(MessageKind.IMAGE)))
        assertTrue(SaveGalleryRules.isVideo(media(MessageKind.VIDEO)))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun mimePrefersPayloadThenExtension() {
        val png = MediaPayload("image", "o", "ab", "K", "image/png", "a.png", 12).toJson()
        val img = File("a.jpg")
        assertEquals("image/png", SaveGalleryRules.mimeFor(media(MessageKind.IMAGE, extra = png), img))
        assertEquals("image/jpeg", SaveGalleryRules.mimeFor(media(MessageKind.IMAGE), img))
        assertEquals("video/mp4", SaveGalleryRules.mimeFor(media(MessageKind.VIDEO), File("a.mp4")))
        assertNull(SaveGalleryRules.sanitizedMime("image/png\n"))
        assertEquals("image/png", SaveGalleryRules.sanitizedMime("  image/png  "))
    }

    @Test
    fun allowOnlyBytesUnderMedia() {
        val root = createTempDirectory("rope-save-gallery").toFile()
        try {
            val mediaDir = File(root, SaveGalleryRules.MEDIA_DIR).apply { mkdirs() }
            val ok = File(mediaDir, "shot.jpg")
            ok.writeBytes(ByteArray(SaveGalleryRules.MIN_BYTES.toInt() + 8))
            assertTrue(SaveGalleryRules.allow(ok, mediaDir))
            assertEquals(ok, SaveGalleryRules.saveFile(media(MessageKind.IMAGE, path = ok.absolutePath), mediaDir))
            assertEquals("shot.jpg", SaveGalleryRules.displayName(ok))

            val outside = File(root, "identity.ropi.enc")
            outside.writeBytes(ByteArray(SaveGalleryRules.MIN_BYTES.toInt() + 8))
            assertFalse(SaveGalleryRules.allow(outside, mediaDir))
            assertNull(SaveGalleryRules.saveFile(media(MessageKind.IMAGE, path = outside.absolutePath), mediaDir))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectCrLfNameAndSymlinkEscape() {
        val root = createTempDirectory("rope-save-gallery-escape").toFile()
        try {
            val mediaDir = File(root, SaveGalleryRules.MEDIA_DIR).apply { mkdirs() }
            val outside = File(root, "secret.jpg")
            outside.writeBytes(ByteArray(SaveGalleryRules.MIN_BYTES.toInt() + 8))
            val link = File(mediaDir, "obj.jpg")
            java.nio.file.Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertFalse(SaveGalleryRules.allow(link, mediaDir))

            val nl = File(mediaDir, "obj.jpg\n")
            nl.writeBytes(ByteArray(SaveGalleryRules.MIN_BYTES.toInt() + 8))
            assertFalse(SaveGalleryRules.allow(nl, mediaDir))
        } finally {
            root.deleteRecursively()
        }
    }
}
