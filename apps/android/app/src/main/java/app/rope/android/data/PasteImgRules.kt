package app.rope.android.data

/**
 * Paste a clipboard / IME image into the composer the same way Attach → gallery
 * stages pending photo URIs. Reject CR/LF/NUL. LocalStore stays v6. Not FCM.
 */
object PasteImgRules {
    const val LABEL = "Вставить фото"
    const val MAX_URI = 2048
    val MIME_TYPES = arrayOf("image/*")

    data class Piece(
        val uri: String? = null,
        val mime: String? = null,
        val text: String? = null,
    )

    fun acceptMime(mime: String?): Boolean {
        val m = mime?.trim()?.lowercase().orEmpty()
        if (m.isEmpty()) return false
        return m == "image/*" || m.startsWith("image/")
    }

    fun canPaste(editing: Boolean, recording: Boolean): Boolean = !editing && !recording

    fun sanitizeUri(raw: String?): String? {
        if (raw == null) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val u = raw.trim()
        if (u.isEmpty() || u.length > MAX_URI) return null
        val lower = u.lowercase()
        if (lower.startsWith("content://") || lower.startsWith("file://")) return u
        return null
    }

    fun looksLikeImagePath(uri: String): Boolean {
        val n = uri.substringAfterLast('/').substringBefore('?').lowercase()
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") ||
            n.endsWith(".webp") || n.endsWith(".heic") || n.endsWith(".gif") ||
            n.endsWith(".bmp")
    }

    fun descriptionHasImage(mimes: List<String>): Boolean = mimes.any { acceptMime(it) }

    fun uris(pieces: List<Piece>, descriptionHasImage: Boolean): List<String> {
        val out = ArrayList<String>(pieces.size.coerceAtMost(AlbumRules.MAX_PHOTOS))
        for (p in pieces) {
            val uri = sanitizeUri(p.uri) ?: continue
            val ok = acceptMime(p.mime) || descriptionHasImage || looksLikeImagePath(uri)
            if (ok && uri !in out) out += uri
            if (out.size >= AlbumRules.MAX_PHOTOS) break
        }
        return out
    }

    fun intercept(
        editing: Boolean,
        recording: Boolean,
        pieces: List<Piece>,
        descriptionHasImage: Boolean,
    ): List<String> {
        if (!canPaste(editing, recording)) return emptyList()
        return uris(pieces, descriptionHasImage)
    }
}
