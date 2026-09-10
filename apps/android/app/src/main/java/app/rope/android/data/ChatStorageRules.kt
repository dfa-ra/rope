package app.rope.android.data

/**
 * Per-chat media bytes and a wipe of that chat's downloaded files.
 * Not the global Settings cache (#102). LocalStore stays v6.
 * No FCM, no CallVideoRenderer, no name/logo/color.
 */
object ChatStorageRules {
    const val TITLE = "Память"
    const val ACTION = "Очистить файлы чата"
    const val CONFIRM_ACTION = "Точно очистить"
    const val CONFIRM = "Скачанные фото, видео и файлы этого чата удалятся с этого телефона. Переписка останется."
    const val CANCEL = "Отмена"
    const val DONE = "Файлы чата очищены"
    const val HINT = "Только вложения этого чата. Другие чаты не трогаем."

    fun isMediaKind(kind: MessageKind): Boolean = when (kind) {
        MessageKind.IMAGE, MessageKind.VIDEO, MessageKind.VOICE, MessageKind.FILE, MessageKind.VIDEO_NOTE -> true
        else -> false
    }

    fun cachedPaths(messages: List<ChatMessage>): List<String> =
        messages
            .filter { isMediaKind(it.kind) && !it.deleted }
            .mapNotNull { JsonIds.optional(it.localPath) }

    fun bytesOf(paths: Collection<String>, lengthOf: (String) -> Long): Long =
        paths.mapNotNull { JsonIds.optional(it) }.distinct().sumOf { lengthOf(it).coerceAtLeast(0L) }

    fun canClear(bytes: Long): Boolean = bytes > 0L

    /** Delete only files this chat uniquely holds. Shared forwards stay. */
    fun filesSafeToDelete(chatPaths: Collection<String>, otherPaths: Collection<String>): List<String> {
        val others = otherPaths.mapNotNull { JsonIds.optional(it) }.toSet()
        return chatPaths.mapNotNull { JsonIds.optional(it) }.distinct().filter { it !in others }
    }

    fun label(bytes: Long): String {
        if (bytes <= 0L) return "Пусто"
        if (bytes < 1024L) return "$bytes Б"
        if (bytes < 1024L * 1024L) return "${bytes / 1024L} КБ"
        val mb = bytes / (1024.0 * 1024.0)
        val shown = if (mb < 10.0) {
            val tenths = kotlin.math.round(mb * 10.0) / 10.0
            if (tenths == tenths.toLong().toDouble()) "${tenths.toLong()} МБ" else "$tenths МБ"
        } else {
            "${mb.toLong()} МБ"
        }
        return shown.replace('.', ',')
    }
}
