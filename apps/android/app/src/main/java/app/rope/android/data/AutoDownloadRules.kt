package app.rope.android.data

import org.json.JSONObject

data class AutoDownloadPrefs(
    val photos: Boolean = true,
    val videos: Boolean = false,
    val files: Boolean = false,
)

/**
 * Telegram-like auto-download. Photos on, video/files off by default.
 * Voice and кружок always prefetch. LocalStore stays v6.
 */
object AutoDownloadRules {
    const val TITLE = "Автозагрузка"
    const val PHOTOS = "Фото"
    const val VIDEOS = "Видео"
    const val FILES = "Файлы"
    const val PLACEHOLDER = "Нажмите, чтобы скачать"

    fun hint(): String =
        "Голосовые и кружки качаются всегда. Видео и файлы — по нажатию, если выкл."

    fun prefetch(kind: MessageKind, prefs: AutoDownloadPrefs): Boolean = when (kind) {
        MessageKind.IMAGE -> prefs.photos
        MessageKind.VIDEO -> prefs.videos
        MessageKind.FILE -> prefs.files
        MessageKind.VOICE, MessageKind.VIDEO_NOTE -> true
        else -> false
    }

    fun parse(raw: String?): AutoDownloadPrefs {
        if (raw.isNullOrBlank()) return AutoDownloadPrefs()
        return try {
            val o = JSONObject(raw)
            AutoDownloadPrefs(
                photos = o.optBoolean("photos", true),
                videos = o.optBoolean("videos", false),
                files = o.optBoolean("files", false),
            )
        } catch (_: Exception) {
            AutoDownloadPrefs()
        }
    }

    fun toJson(prefs: AutoDownloadPrefs): String = JSONObject()
        .put("photos", prefs.photos)
        .put("videos", prefs.videos)
        .put("files", prefs.files)
        .toString()
}
