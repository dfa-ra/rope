package app.rope.android

import app.rope.android.data.ChatExportChat
import app.rope.android.data.ChatExportRules
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import app.rope.android.update.DeviceBackup
import app.rope.android.update.PublicBackupRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class ChatExportRulesTest {
    private val msg = ChatMessage(
        id = "m1",
        peerDeviceId = "peer",
        outgoing = true,
        text = "привет",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.TEXT,
        envelope = byteArrayOf(1, 2, 3),
    )

    @Test
    fun passphraseMin12AndConfirm() {
        assertFalse(ChatExportRules.passphraseOk("short", "short"))
        assertFalse(ChatExportRules.passphraseOk("twelvechars!", "twelvechars?"))
        assertTrue(ChatExportRules.passphraseOk("twelvechars!", "twelvechars!"))
        assertEquals(12, ChatExportRules.MIN_PASS)
        assertTrue(ChatExportRules.MIN_PASS < 25)
    }

    @Test
    fun filenameIsRobkNotDeviceBackup() {
        val name = ChatExportRules.suggestedName(1_789_000_000_000L)
        assertTrue(name.matches(Regex("rope-chats-\\d{8}\\.robk")))
        assertTrue(name.endsWith(".robk"))
        assertEquals("rope-chats-20260910.robk", name)
        assertFalse(name.contains("device"))
        assertFalse(name.contains(ChatExportRules.DEVICE_BACKUP_NAME))
        assertEquals("rope-device.backup", DeviceBackup.FILE_NAME)
    }

    @Test
    fun denylistKeepsIdentityOutOfManifestAndRows() {
        val manifest = ChatExportRules.manifest(
            nowMs = 10L,
            messageCount = 1,
            mediaCount = 0,
            chats = listOf(ChatExportChat(SavedMessagesRules.ID, SavedMessagesRules.TITLE, "saved")),
        )
        val row = ChatExportRules.rowJson(msg, null)
        assertFalse(ChatExportRules.jsonHasForbiddenKey(manifest.toString()))
        assertFalse(ChatExportRules.jsonHasForbiddenKey(row.toString()))
        ChatExportRules.forbiddenKeys.forEach { key ->
            assertFalse(manifest.has(key))
            assertFalse(row.has(key))
        }
        assertEquals("chat_takeout", manifest.getString("kind"))
        assertEquals("saved", ChatExportRules.chatKind(SavedMessagesRules.ID))
        assertEquals("group", ChatExportRules.chatKind(ChatIds.group("uuid")))
        assertEquals("dm", ChatExportRules.chatKind("abc"))
        assertTrue(ChatExportRules.skipMessage(msg.copy(deleted = true)))
        assertFalse(ChatExportRules.skipMessage(msg))
    }

    @Test
    fun mediaPrefixRejectsEscapeAndMissing() {
        val root = File(System.getProperty("java.io.tmpdir"), "rope-media-root").apply { mkdirs() }
        val ok = File(root, "pic.jpg").apply { writeText("x") }
        val outside = File(System.getProperty("java.io.tmpdir"), "rope-media-out.jpg").apply { writeText("y") }
        assertTrue(ChatExportRules.mediaAllowed(ok.path, root))
        assertFalse(ChatExportRules.mediaAllowed(outside.path, root))
        assertFalse(ChatExportRules.mediaAllowed(null, root))
        assertFalse(ChatExportRules.mediaAllowed(File(root, "missing.bin").path, root))
        val used = mutableSetOf<String>()
        val rel = ChatExportRules.mediaRelName("m1", ok, used)
        assertTrue(rel.startsWith("media/"))
        assertFalse(rel.contains(".."))
    }

    @Test
    fun innerZipHasManifestJsonlAndMedia() {
        val dir = File(System.getProperty("java.io.tmpdir"), "rope-export-zip").apply {
            deleteRecursively()
            mkdirs()
        }
        val mediaRoot = File(dir, "media").apply { mkdirs() }
        val pic = File(mediaRoot, "a.jpg").apply { writeBytes(byteArrayOf(7, 8, 9)) }
        val used = mutableSetOf<String>()
        val rel = ChatExportRules.mediaRelName("m1", pic, used)
        val zip = File(dir, "inner.zip")
        ChatExportRules.writeInnerZip(
            zip,
            ChatExportRules.manifest(2L, 1, 1, listOf(ChatExportChat("peer", "Анна", "dm"))),
            listOf(ChatExportRules.rowJson(msg, rel)),
            listOf(rel to pic),
        )
        ZipFile(zip).use { z ->
            val names = z.entries().toList().map { it.name }.toSet()
            assertTrue(names.contains("manifest.json"))
            assertTrue(names.contains("messages.jsonl"))
            assertTrue(names.contains(rel))
            val manifest = z.getInputStream(z.getEntry("manifest.json")).readBytes().decodeToString()
            assertFalse(ChatExportRules.jsonHasForbiddenKey(manifest))
        }
    }

    @Test
    fun robkMagicIsNotDeviceBackupAndNotDownloads() {
        val robk = byteArrayOf(0x52, 0x4F, 0x42, 0x4B, 1, 2, 3)
        assertTrue(ChatExportRules.hasMagic(robk))
        assertFalse(DeviceBackup.isSealed(robk))
        assertFalse(DeviceBackup.allowAutoRestore(robk))
        assertFalse(PublicBackupRules.allowIdentityDump)
        assertFalse(PublicBackupRules.allowRobkDownloads)
        try {
            DeviceBackup.open(robk) { error("must not decrypt robk") }
            org.junit.Assert.fail("expected robk reject")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("экспорт"))
        }
    }
}
