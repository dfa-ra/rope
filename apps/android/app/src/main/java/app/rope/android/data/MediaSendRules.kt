package app.rope.android.data

/**
 * Telegram-like media send editor: one caption on photo/video send, and
 * videos may share [album_id] with photos. Caption lives in type=2 JSON
 * (Go never sees it). Empty caption omits the key.
 */
object MediaSendRules {
    const val CAPTION_MAX = 1024
    const val PLACEHOLDER = "Подпись"
    const val DISMISS = "Отменить вложение"

    fun normalize(raw: String?): String? {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return null
        return if (v.length <= CAPTION_MAX) v else v.take(CAPTION_MAX)
    }

    /** Caption rides the first album member (or the single photo/video). */
    fun onFirstOnly(index: Int, caption: String?): String? =
        if (index == 0) normalize(caption) else null

    fun captionOf(msg: ChatMessage): String? =
        runCatching { normalize(MediaPayload.parse(msg.extra).caption) }.getOrNull()

    fun albumCaption(members: List<ChatMessage>): String? =
        members.asSequence().mapNotNull { captionOf(it) }.firstOrNull()

    fun hint(count: Int, videos: Int = 0, photos: Int = 0): ComposerHintCopy {
        val n = count.coerceAtLeast(1)
        val title = when {
            n == 1 && videos >= 1 -> "Видео"
            n == 1 -> "Фото"
            else -> AlbumRules.preview(n, videos.coerceAtLeast(0))
        }
        val body = if (n == 1) "подпись к вложению" else "одна подпись на альбом"
        return ComposerHintCopy(
            kind = ComposerHintKind.MEDIA,
            title = title,
            body = body,
            dismissContentDescription = DISMISS,
        )
    }
}
