package app.rope.android.data

/**
 * Invite pane **Сохранить QR** writes the QR bitmap into the device gallery.
 * Distinct from share-sheet, clipboard copy, rotate, and TTL. Not envelope crypto.
 */
object SaveInviteQrRules {
    const val LABEL = "Сохранить QR"
    const val NOTICE = "QR в галерее"
    const val PREFIX = "rope://join?"
    const val MIME = "image/png"
    const val FILE = "rope-invite-qr.png"
    const val PICTURES = "Pictures/Rope/"

    fun accept(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val url = raw.trim()
        if (!url.startsWith(PREFIX)) return null
        if (url.length <= PREFIX.length) return null
        return url
    }

    fun enabled(raw: String?): Boolean = accept(raw) != null

    fun displayName(): String {
        if ('\n' in FILE || '\r' in FILE || '\u0000' in FILE) return "invite.png"
        if ('/' in FILE || '\\' in FILE) return "invite.png"
        return FILE
    }
}
