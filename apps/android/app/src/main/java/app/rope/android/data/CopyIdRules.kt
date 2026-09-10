package app.rope.android.data

/**
 * Telegram-like copy identifier from a profile. Clipboard is local —
 * no Go, no DeviceBackup, no LocalStore bump.
 */
object CopyIdRules {
    const val ACTION = "Скопировать ID"
    const val PREVIEW_MAX = 12

    fun canCopy(id: String?): Boolean =
        !id.isNullOrBlank() && !SavedMessagesRules.isSaved(id)

    fun clip(id: String?): String? {
        val t = id?.trim().orEmpty()
        return t.takeIf { canCopy(t) }
    }

    fun preview(id: String?): String {
        val t = clip(id) ?: return ""
        return if (t.length <= PREVIEW_MAX) t else t.take(8) + "…"
    }
}
