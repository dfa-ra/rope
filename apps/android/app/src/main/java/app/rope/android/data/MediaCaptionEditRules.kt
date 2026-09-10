package app.rope.android.data

/**
 * Telegram-like edit of an outgoing photo/video caption. Caption stays in
 * type=2 inner JSON (Go never sees it). LocalStore v6 — no schema bump.
 * No FCM, no CallVideoRenderer, no name/logo/color.
 */
object MediaCaptionEditRules {
    fun isCaptionKind(kind: MessageKind?): Boolean =
        kind == MessageKind.IMAGE || kind == MessageKind.VIDEO

    /** First album member (or a single photo/video) holds the one caption. */
    fun eligibleKind(msg: ChatMessage): Boolean {
        if (!isCaptionKind(msg.kind)) return false
        if (msg.extra.isBlank()) return true
        val payload = runCatching { MediaPayload.parse(msg.extra) }.getOrNull() ?: return true
        return payload.albumCount <= 1 || payload.albumIndex == 0
    }

    fun canEdit(msg: ChatMessage): Boolean =
        msg.outgoing && !msg.deleted && eligibleKind(msg)

    fun draft(msg: ChatMessage): String =
        if (isCaptionKind(msg.kind)) MediaSendRules.captionOf(msg).orEmpty() else msg.text

    fun allowBlankCommit(msg: ChatMessage): Boolean = isCaptionKind(msg.kind)

    fun apply(msg: ChatMessage, raw: String): Applied? {
        if (!isCaptionKind(msg.kind)) return null
        val payload = runCatching { MediaPayload.parse(msg.extra) }.getOrNull() ?: return null
        val next = payload.copy(caption = MediaSendRules.normalize(raw))
        return Applied(preview = next.preview(), extra = next.toJson())
    }

    data class Applied(val preview: String, val extra: String)
}
