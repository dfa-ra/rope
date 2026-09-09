package app.rope.android.data

/**
 * Telegram-like shared-media hub on peer profile and group info.
 * Filters LocalStore rows already in this chat — no Go, no schema bump.
 */
enum class MediaHubTab { MEDIA, FILES, LINKS, VOICE }

object MediaHubRules {
    const val GRID_COLUMNS = 3
    const val TILE_EDGE = 320
    const val SECTION = "Общие медиа"

    fun items(messages: List<ChatMessage>, tab: MediaHubTab): List<ChatMessage> = when (tab) {
        MediaHubTab.MEDIA -> media(messages)
        MediaHubTab.FILES -> files(messages)
        MediaHubTab.LINKS -> links(messages)
        MediaHubTab.VOICE -> voice(messages)
    }

    fun media(messages: List<ChatMessage>): List<ChatMessage> =
        newest(messages.filter { !it.deleted && (it.kind == MessageKind.IMAGE || it.kind == MessageKind.VIDEO) })

    fun files(messages: List<ChatMessage>): List<ChatMessage> =
        newest(messages.filter { !it.deleted && it.kind == MessageKind.FILE })

    fun links(messages: List<ChatMessage>): List<ChatMessage> =
        newest(messages.filter { isLink(it) })

    fun voice(messages: List<ChatMessage>): List<ChatMessage> =
        newest(messages.filter { !it.deleted && (it.kind == MessageKind.VOICE || it.kind == MessageKind.VIDEO_NOTE) })

    fun isLink(msg: ChatMessage): Boolean {
        if (msg.deleted) return false
        if (msg.linkPreview != null) return true
        return LinkPreviewRules.firstHttps(msg.text) != null
    }

    fun opensViewer(msg: ChatMessage): Boolean =
        !msg.deleted && (msg.kind == MessageKind.IMAGE || msg.kind == MessageKind.VIDEO)

    fun usesVideoPoster(kind: MessageKind): Boolean = kind == MessageKind.VIDEO

    fun tabLabel(tab: MediaHubTab): String = when (tab) {
        MediaHubTab.MEDIA -> "Медиа"
        MediaHubTab.FILES -> "Файлы"
        MediaHubTab.LINKS -> "Ссылки"
        MediaHubTab.VOICE -> "Голос"
    }

    fun tabCaption(tab: MediaHubTab, count: Int): String {
        val name = tabLabel(tab)
        return if (count <= 0) name else "$name $count"
    }

    fun emptyTitle(tab: MediaHubTab): String = when (tab) {
        MediaHubTab.MEDIA -> "Нет фото и видео"
        MediaHubTab.FILES -> "Нет файлов"
        MediaHubTab.LINKS -> "Нет ссылок"
        MediaHubTab.VOICE -> "Нет голосовых"
    }

    fun emptyBody(tab: MediaHubTab): String = when (tab) {
        MediaHubTab.MEDIA -> "Фото и видео из этой переписки появятся здесь."
        MediaHubTab.FILES -> "Документы из этой переписки появятся здесь."
        MediaHubTab.LINKS -> "Ссылки из этой переписки появятся здесь."
        MediaHubTab.VOICE -> "Голосовые и кружки из этой переписки появятся здесь."
    }

    fun fileLabel(msg: ChatMessage): String {
        if (msg.extra.isNotBlank()) {
            val name = runCatching { MediaPayload.parse(msg.extra).name }.getOrNull()?.trim().orEmpty()
            if (name.isNotEmpty()) return name
        }
        return msg.text.trim().ifBlank { "Файл" }
    }

    fun linkLabel(msg: ChatMessage): String {
        val lp = msg.linkPreview
        if (lp != null) {
            return lp.title.trim().ifBlank { lp.host }.ifBlank { lp.url }
        }
        return LinkPreviewRules.firstHttps(msg.text).orEmpty()
    }

    fun voiceLabel(msg: ChatMessage): String {
        if (msg.kind == MessageKind.VIDEO_NOTE) return "Кружок"
        if (msg.extra.isNotBlank()) {
            val preview = runCatching { MediaPayload.parse(msg.extra).preview() }.getOrNull()
            if (!preview.isNullOrBlank()) return preview
        }
        return "Голосовое"
    }

    private fun newest(messages: List<ChatMessage>): List<ChatMessage> =
        messages.sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenByDescending { it.id })
}
