package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatStorageRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStorageRulesTest {
    @Test
    fun mediaKindsCountAndTextIsSkipped() {
        val photo = media("/a.jpg")
        val text = media("/t.txt", kind = MessageKind.TEXT)
        val deleted = media("/d.jpg", deleted = true)
        val voice = media("/v.m4a", kind = MessageKind.VOICE)
        val paths = ChatStorageRules.cachedPaths(listOf(photo, text, deleted, voice))
        assertEquals(listOf("/a.jpg", "/v.m4a"), paths)
        assertTrue(ChatStorageRules.isMediaKind(MessageKind.IMAGE))
        assertTrue(ChatStorageRules.isMediaKind(MessageKind.VIDEO))
        assertTrue(ChatStorageRules.isMediaKind(MessageKind.FILE))
        assertTrue(ChatStorageRules.isMediaKind(MessageKind.VIDEO_NOTE))
        assertFalse(ChatStorageRules.isMediaKind(MessageKind.TEXT))
        assertFalse(ChatStorageRules.isMediaKind(MessageKind.GROUP_TEXT))
    }

    @Test
    fun bytesSumDistinctFiles() {
        val sizes = mapOf("/a.jpg" to 1500L, "/b.mp4" to 2500L)
        val paths = listOf("/a.jpg", "/a.jpg", "/b.mp4")
        assertEquals(4000L, ChatStorageRules.bytesOf(paths) { sizes[it] ?: 0L })
        assertEquals(0L, ChatStorageRules.bytesOf(listOf(" ", "null")) { 99L })
        assertFalse(ChatStorageRules.canClear(0L))
        assertTrue(ChatStorageRules.canClear(1L))
        assertEquals("Пусто", ChatStorageRules.label(0))
        assertEquals("500 Б", ChatStorageRules.label(500))
        assertEquals("2 КБ", ChatStorageRules.label(2048))
        assertTrue(ChatStorageRules.label(2_000_000L).contains("МБ"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun sharedForwardFileIsNotDeleted() {
        val chat = listOf("/shared.jpg", "/only-here.jpg", "")
        val others = listOf("/shared.jpg", "/other.jpg")
        assertEquals(listOf("/only-here.jpg"), ChatStorageRules.filesSafeToDelete(chat, others))
        assertEquals(emptyList<String>(), ChatStorageRules.filesSafeToDelete(listOf("/shared.jpg"), others))
        assertEquals(listOf("/shared.jpg", "/only-here.jpg"), ChatStorageRules.filesSafeToDelete(chat, emptyList()))
    }

    @Test
    fun copyDoesNotNameGlobalCache() {
        assertEquals("Память", ChatStorageRules.TITLE)
        assertEquals("Очистить файлы чата", ChatStorageRules.ACTION)
        assertTrue(ChatStorageRules.HINT.contains("этого чата"))
        assertFalse(ChatStorageRules.HINT.contains("FCM"))
        assertFalse(ChatStorageRules.ACTION.contains("кэш"))
        assertTrue(ChatStorageRules.CONFIRM.contains("этого чата"))
        assertEquals("Файлы чата очищены", ChatStorageRules.DONE)
    }

    private fun media(
        path: String,
        kind: MessageKind = MessageKind.IMAGE,
        deleted: Boolean = false,
    ) = ChatMessage(
        id = path,
        peerDeviceId = "p",
        outgoing = true,
        text = "x",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        localPath = path,
        deleted = deleted,
    )
}
