package app.rope.android

import app.rope.android.data.ChatListPreviewKind
import app.rope.android.data.ChatListPreviewRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.GroupChatUx
import app.rope.android.data.LinkPreviewRules
import app.rope.android.data.LocalStore
import app.rope.android.data.LpChatRules
import app.rope.android.data.MessageStatus
import app.rope.android.data.PackedLinkPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LpChatRulesTest {
    private fun lp(
        url: String = "https://example.com/a",
        host: String = "example.com",
        title: String = "Page title",
    ) = PackedLinkPreview(url = url, host = host, title = title)

    private fun msg(
        text: String,
        preview: PackedLinkPreview? = lp(),
        outgoing: Boolean = false,
        deleted: Boolean = false,
        senderName: String = "",
        senderId: String = "",
    ) = ChatMessage(
        id = "m",
        peerDeviceId = "peer",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1_000L,
        senderId = senderId,
        senderName = senderName,
        deleted = deleted,
        linkPreview = preview,
    )

    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Ссылка", LpChatRules.LABEL)
        assertFalse(LpChatRules.LABEL.contains("FCM", ignoreCase = true))
    }

    @Test
    fun urlOnlyUsesOgTitle() {
        val url = "https://example.com/a"
        assertTrue(LpChatRules.urlOnly(url, lp()))
        assertTrue(LpChatRules.urlOnly("https://example.com/a.", lp()))
        assertFalse(LpChatRules.urlOnly("смотри https://example.com/a", lp()))
        assertEquals("Page title", LpChatRules.display(lp()))
        assertEquals("example.com", LpChatRules.display(lp(title = "example.com")))
        assertEquals("example.com", LpChatRules.display(lp(title = "")))
        assertEquals("Page title", LpChatRules.body(msg(url)))
        assertEquals("смотри https://example.com/a", LpChatRules.body(msg("смотри https://example.com/a")))
        assertEquals("Сообщение удалено", LpChatRules.body(msg(url, deleted = true)))
        assertEquals(url, LpChatRules.body(msg(url, preview = null)))
    }

    @Test
    fun chatListUsesTitleForUrlOnlyLast() {
        val last = msg("https://example.com/a", senderName = "Аня", senderId = "d2")
        val dm = ChatListPreviewRules.copy(last, draft = "", isGroup = false, myDeviceId = "me")
        assertEquals(ChatListPreviewKind.LAST, dm.kind)
        assertEquals("Page title", dm.text)
        val mine = ChatListPreviewRules.copy(
            last.copy(outgoing = true, senderId = "me"),
            draft = "",
            isGroup = false,
            myDeviceId = "me",
        )
        assertEquals("Вы: Page title", mine.text)
        val group = ChatListPreviewRules.copy(
            last,
            draft = "",
            isGroup = true,
            myDeviceId = "me",
        )
        assertEquals("Аня: Page title", group.text)
        assertEquals("Аня: Page title", GroupChatUx.listPreview(last, "me"))
        val parsed = LinkPreviewRules.parse("https://example.com/a")!!
        assertEquals("example.com", LpChatRules.display(parsed))
        val longTitle = "я".repeat(ChatListPreviewRules.BODY_MAX + 12)
        assertTrue(LpChatRules.display(lp(title = longTitle)).length <= ChatListPreviewRules.BODY_MAX)
    }
}
