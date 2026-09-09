package app.rope.android.data

import java.io.File
import java.util.Locale

/**
 * Telegram-like media cache wipe. Deletes [filesDir]/media only.
 * Messages stay; local_path is cleared by LocalStore. Schema stays v6.
 */
object CacheRules {
    const val DIR = "media"
    const val TITLE = "Память"
    const val ACTION = "Очистить кэш"
    const val CONFIRM = "Скачанные фото, видео и файлы удалятся с этого телефона. Переписка останется."
    const val CANCEL = "Отмена"
    const val DONE = "Кэш очищен"

    fun hint(): String = "Только вложения. Ключи и история не трогаем."

    fun dir(filesDir: File): File = File(filesDir, DIR)

    fun bytesOf(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun deleteTree(dir: File): Boolean {
        if (!dir.exists()) return true
        return dir.deleteRecursively()
    }

    fun label(bytes: Long): String {
        if (bytes <= 0L) return "Пусто"
        if (bytes < 1024L) return "$bytes Б"
        if (bytes < 1024L * 1024L) return "${bytes / 1024L} КБ"
        val mb = bytes / (1024.0 * 1024.0)
        val ru = Locale("ru", "RU")
        return if (mb < 10.0) String.format(ru, "%.1f МБ", mb) else String.format(ru, "%.0f МБ", mb)
    }
}
