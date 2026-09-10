package app.rope.android.data

/**
 * Own avatar from Settings. Local file + kv `avatar_path` only.
 * Not a nickname, not envelope crypto. LocalStore stays v6.
 */
object SetPhotoRules {
    const val TITLE = "Фото профиля"
    const val HINT = "Только на этом устройстве. Не шифрование."
    const val NEED_PHOTO = "Нужно фото"

    fun parse(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) return null
        val v = raw.trim()
        return v.takeIf { it.isNotEmpty() }
    }

    fun acceptsMime(mime: String): Boolean = mime.lowercase().startsWith("image/")
}
