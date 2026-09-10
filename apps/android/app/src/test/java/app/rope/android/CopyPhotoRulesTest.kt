package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.CopyPhotoRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CopyPhotoRulesTest {
    private fun photo(
        text: String = "",
        path: String? = "/media/x.jpg",
        deleted: Boolean = false,
        kind: MessageKind = MessageKind.IMAGE,
    ) = ChatMessage(
        id = "1",
        peerDeviceId = "p",
        outgoing = true,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        localPath = path,
        deleted = deleted,
    )

    @Test
    fun canCopyImageNeedsDownloadedPhoto() {
        assertTrue(CopyPhotoRules.canCopyImage(photo()))
        assertTrue(ChatActions.canCopy(photo()))
        assertTrue(ChatActions.canCopy(photo(text = "подпись")))
        assertFalse(CopyPhotoRules.canCopyImage(photo(deleted = true)))
        assertFalse(ChatActions.canCopy(photo(deleted = true)))
        assertFalse(CopyPhotoRules.canCopyImage(photo(path = null)))
        assertFalse(ChatActions.canCopy(photo(path = null)))
        assertFalse(CopyPhotoRules.canCopyImage(photo(path = "  ")))
        assertFalse(CopyPhotoRules.canCopyImage(photo(kind = MessageKind.TEXT, path = "/media/x.jpg")))
        assertFalse(CopyPhotoRules.canCopyImage(photo(kind = MessageKind.VIDEO, path = "/media/x.mp4")))
        assertFalse(ChatActions.canCopy(photo(kind = MessageKind.VIDEO, path = "/media/x.mp4")))
        assertTrue(ChatActions.canCopy(photo(kind = MessageKind.TEXT, text = "hi", path = null)))
        assertEquals("image", CopyPhotoRules.CLIP_LABEL)
        assertEquals("app.rope.android.files", CopyPhotoRules.authority("app.rope.android"))
        assertEquals("app.rope.android.debug.files", CopyPhotoRules.authority("app.rope.android.debug"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun allowOnlyImageBytesUnderMedia() {
        val root = createTempDirectory("rope-copy-photo").toFile()
        try {
            val media = File(root, CopyPhotoRules.MEDIA_DIR).apply { mkdirs() }
            val ok = File(media, "obj-1.jpg")
            ok.writeBytes(ByteArray(CopyPhotoRules.MIN_BYTES.toInt() + 8))
            assertTrue(CopyPhotoRules.allow(ok, media))
            assertEquals("image/jpeg", CopyPhotoRules.mimeFor(ok))
            assertEquals("image/png", CopyPhotoRules.mimeFor(File(media, "a.png")))
            assertEquals("image/webp", CopyPhotoRules.mimeFor(File(media, "a.webp")))
            assertEquals("image/gif", CopyPhotoRules.mimeFor(File(media, "a.gif")))
            assertEquals(
                arrayOf("image/jpeg", CopyPhotoRules.URI_LIST_MIME).toList(),
                CopyPhotoRules.clipMimeTypes(ok).toList(),
            )
            assertEquals(ok, CopyPhotoRules.clipFile(photo(path = ok.absolutePath), media))

            val small = File(media, "tiny.jpg")
            small.writeBytes(ByteArray(4))
            assertFalse(CopyPhotoRules.allow(small, media))
            assertNull(CopyPhotoRules.clipFile(photo(path = small.absolutePath), media))

            val outside = File(root, "identity.ropi.enc")
            outside.writeBytes(ByteArray(CopyPhotoRules.MIN_BYTES.toInt() + 8))
            assertFalse(CopyPhotoRules.allow(outside, media))
            assertNull(CopyPhotoRules.clipFile(photo(path = outside.absolutePath), media))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectCrLfNameAndSymlinkEscape() {
        val root = createTempDirectory("rope-copy-photo-escape").toFile()
        try {
            val media = File(root, CopyPhotoRules.MEDIA_DIR).apply { mkdirs() }
            val outside = File(root, "secret.jpg")
            outside.writeBytes(ByteArray(CopyPhotoRules.MIN_BYTES.toInt() + 8))
            val link = File(media, "obj.jpg")
            java.nio.file.Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertFalse(CopyPhotoRules.allow(link, media))

            val nl = File(media, "obj.jpg\n")
            nl.writeBytes(ByteArray(CopyPhotoRules.MIN_BYTES.toInt() + 8))
            assertFalse(CopyPhotoRules.allow(nl, media))
        } finally {
            root.deleteRecursively()
        }
    }
}
