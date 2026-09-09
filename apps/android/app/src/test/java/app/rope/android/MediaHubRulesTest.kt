package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.MediaHubRules
import app.rope.android.data.MediaHubTab
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.PackedLinkPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaHubRulesTest {
    @Test
    fun mediaIncludesVideoSkipsVoiceAndDeleted() {
        val photo = msg("p", 10L, MessageKind.IMAGE)
        val video = msg("v", 20L, MessageKind.VIDEO)
        val voice = msg("w", 30L, MessageKind.VOICE)
        val gone = msg("d", 40L, MessageKind.IMAGE, deleted = true)
        val media = MediaHubRules.media(listOf(photo, video, voice, gone))
        assertEquals(listOf("v", "p"), media.map { it.id })
        assertTrue(media.all { MediaHubRules.opensViewer(it) })
        assertFalse(MediaHubRules.opensViewer(voice))
        assertTrue(MediaHubRules.usesVideoPoster(MessageKind.VIDEO))
        assertFalse(MediaHubRules.usesVideoPoster(MessageKind.IMAGE))
        assertEquals(320, MediaHubRules.TILE_EDGE)
    }

    @Test
    fun filesAndVoiceTabs() {
        val file = msg("f", 1L, MessageKind.FILE, extra = """{"kind":"file","name":"акт.pdf"}""")
        val voice = msg("w", 2L, MessageKind.VOICE)
        val note = msg("n", 3L, MessageKind.VIDEO_NOTE)
        val all = listOf(file, voice, note)
        assertEquals(listOf("f"), MediaHubRules.files(all).map { it.id })
        assertEquals(listOf("n", "w"), MediaHubRules.voice(all).map { it.id })
        assertEquals("акт.pdf", MediaHubRules.fileLabel(file))
        assertEquals("Кружок", MediaHubRules.voiceLabel(note))
        assertTrue(MediaHubRules.items(all, MediaHubTab.FILES).isNotEmpty())
        assertTrue(MediaHubRules.items(all, MediaHubTab.MEDIA).isEmpty())
    }

    @Test
    fun linksFromPackedPreviewAndHttpsText() {
        val packed = msg("a", 20L, MessageKind.TEXT).copy(
            linkPreview = PackedLinkPreview("https://example.com/x", "example.com", "Заголовок"),
        )
        val https = msg("b", 10L, MessageKind.TEXT, text = "смотри https://rope.example/z")
        val plain = msg("c", 30L, MessageKind.TEXT, text = "без ссылки")
        val hits = MediaHubRules.links(listOf(packed, https, plain))
        assertEquals(listOf("a", "b"), hits.map { it.id })
        assertEquals("Заголовок", MediaHubRules.linkLabel(packed))
        assertTrue(MediaHubRules.linkLabel(https).contains("https://"))
        assertFalse(MediaHubRules.isLink(plain))
    }

    @Test
    fun tabCopy() {
        assertEquals("Медиа", MediaHubRules.tabLabel(MediaHubTab.MEDIA))
        assertEquals("Медиа 2", MediaHubRules.tabCaption(MediaHubTab.MEDIA, 2))
        assertEquals("Медиа", MediaHubRules.tabCaption(MediaHubTab.MEDIA, 0))
        assertEquals("Нет фото и видео", MediaHubRules.emptyTitle(MediaHubTab.MEDIA))
        assertEquals("Нет файлов", MediaHubRules.emptyTitle(MediaHubTab.FILES))
        assertEquals("Нет ссылок", MediaHubRules.emptyTitle(MediaHubTab.LINKS))
        assertEquals("Нет голосовых", MediaHubRules.emptyTitle(MediaHubTab.VOICE))
        assertEquals(3, MediaHubRules.GRID_COLUMNS)
        assertEquals(MediaHubRules.SECTION, "Общие медиа")
    }

    private fun msg(
        id: String,
        ts: Long,
        kind: MessageKind,
        deleted: Boolean = false,
        extra: String = "",
        text: String = "x",
    ) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = kind,
        extra = extra,
        deleted = deleted,
    )
}
