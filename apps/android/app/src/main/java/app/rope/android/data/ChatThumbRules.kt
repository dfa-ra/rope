package app.rope.android.data

/**
 * Telegram-like last-media thumbnail on the chat list row.
 * Only already-cached local files; no extra download. Not envelope crypto.
 */
object ChatThumbRules {
    const val SIZE_DP = 48

    fun shows(last: ChatMessage?): Boolean {
        val m = last ?: return false
        if (m.deleted || m.localPath.isNullOrBlank()) return false
        return m.kind == MessageKind.IMAGE ||
            m.kind == MessageKind.VIDEO ||
            m.kind == MessageKind.VIDEO_NOTE
    }

    fun isVideo(kind: MessageKind): Boolean =
        kind == MessageKind.VIDEO || kind == MessageKind.VIDEO_NOTE
}
