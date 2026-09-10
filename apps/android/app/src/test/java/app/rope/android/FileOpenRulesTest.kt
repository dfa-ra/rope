package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.FileOpenRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class FileOpenRulesTest {
    private fun fileMsg(
        extra: String = "",
        localPath: String? = null,
        deleted: Boolean = false,
        text: String = "doc.pdf",
    ) = ChatMessage(
        id = "f1",
        peerDeviceId = "p",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.FILE,
        extra = extra,
        localPath = localPath,
        deleted = deleted,
    )

    @Test
    fun inheritStoreAndOpenFileKind() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("media", FileOpenRules.FILES_DIR)
        val doc = fileMsg()
        assertTrue(FileOpenRules.canOpen(doc))
        assertTrue(ChatActions.canOpen(doc))
        assertFalse(FileOpenRules.canOpen(doc.copy(deleted = true)))
        assertFalse(ChatActions.canOpen(doc.copy(deleted = true)))
        assertFalse(FileOpenRules.canOpen(doc.copy(kind = MessageKind.IMAGE)))
        assertTrue(ChatActions.canOpen(doc.copy(kind = MessageKind.IMAGE)))
        assertFalse(ChatActions.canOpen(doc.copy(kind = MessageKind.TEXT, text = "hi")))
    }

    @Test
    fun mimeAndNameFromPayload() {
        val p = MediaPayload("file", "obj", "ab", "KEY", "application/pdf", "report.pdf", 12)
        val msg = fileMsg(extra = p.toJson(), text = "Файл")
        assertEquals("application/pdf", FileOpenRules.mime(msg))
        assertEquals("report.pdf", FileOpenRules.displayName(msg))
        assertEquals(FileOpenRules.FALLBACK_MIME, FileOpenRules.sanitizeMime("application/pdf\n"))
        assertEquals(FileOpenRules.FALLBACK_MIME, FileOpenRules.sanitizeMime("text html"))
        assertEquals(FileOpenRules.FALLBACK_MIME, FileOpenRules.sanitizeMime(""))
        assertEquals("text/plain", FileOpenRules.sanitizeMime("text/plain"))
        assertEquals("passwd", FileOpenRules.displayName(fileMsg(text = "../etc/passwd")))
    }

    @Test
    fun allowOnlyUnderMediaDir() {
        val root = createTempDirectory("rope-file-open").toFile()
        try {
            val media = File(root, FileOpenRules.FILES_DIR).apply { mkdirs() }
            val ok = File(media, "obj.pdf").apply { writeText("hello") }
            val msg = fileMsg(
                extra = MediaPayload("file", "obj", "ab", "KEY", "application/pdf", "obj.pdf", 5).toJson(),
                localPath = ok.absolutePath,
            )
            val target = FileOpenRules.target(msg, media)!!
            assertEquals("application/pdf", target.mime)
            assertEquals("obj.pdf", target.name)
            assertTrue(FileOpenRules.allow(ok, media))

            val outside = File(root, "escape.pdf").apply { writeText("nope") }
            assertFalse(FileOpenRules.allow(outside, media))
            assertNull(FileOpenRules.target(msg.copy(localPath = outside.absolutePath), media))
            assertNull(FileOpenRules.target(msg.copy(localPath = null), media))
            assertNull(FileOpenRules.target(msg.copy(deleted = true), media))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectCrLfNameAndSymlinkEscape() {
        val root = createTempDirectory("rope-file-escape").toFile()
        try {
            val media = File(root, FileOpenRules.FILES_DIR).apply { mkdirs() }
            val outside = File(root, "obj.pdf").apply { writeText("out") }
            val link = File(media, "obj.pdf")
            java.nio.file.Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertFalse(FileOpenRules.allow(link, media))

            val nl = File(media, "obj.pdf\n")
            nl.writeText("x")
            assertFalse(FileOpenRules.allow(nl, media))
        } finally {
            root.deleteRecursively()
        }
    }
}
