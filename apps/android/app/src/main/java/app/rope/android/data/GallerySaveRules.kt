package app.rope.android.data

/**
 * Telegram-like save-to-gallery. Kv-only. Photos and videos only —
 * voice, files, and кружок stay in app storage. LocalStore stays v6.
 */
object GallerySaveRules {
    const val SECTION = "Медиа"
    const val TITLE = "Сохранять в галерею"

    fun hint(): String =
        "Фото и видео после скачивания — в галерею. Голос и файлы не трогаем. Сервер альбом не видит."

    fun shouldCopy(kind: MessageKind, enabled: Boolean): Boolean =
        enabled && (kind == MessageKind.IMAGE || kind == MessageKind.VIDEO)

    fun supported(sdk: Int): Boolean = sdk >= 29

    fun isVideoMime(mime: String): Boolean = mime.trim().lowercase().startsWith("video/")

    fun displayName(name: String, objectId: String): String {
        val base = name.substringAfterLast('/').trim().ifBlank { "rope" }
        val stem = if (base.contains('.')) base else "$base.jpg"
        val id = objectId.trim().take(8)
        return if (id.isEmpty()) stem else "$id-$stem"
    }
}
