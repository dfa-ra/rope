package app.rope.android.data

/**
 * Telegram-like copy of a photo/video caption. Caption lives in
 * [MediaPayload] extra (Go never sees it). [ChatActions.canCopy] used to
 * look at [ChatMessage.text] only. No LocalStore bump.
 */
object CopyCaptionRules {
    fun isPhotoOrVideo(msg: ChatMessage): Boolean =
        msg.kind == MessageKind.IMAGE || msg.kind == MessageKind.VIDEO

    fun caption(msg: ChatMessage): String? {
        if (msg.deleted || !isPhotoOrVideo(msg)) return null
        return MediaSendRules.captionOf(msg)
    }

    fun hasCaption(msg: ChatMessage): Boolean = caption(msg) != null

    /**
     * Clipboard text. Photo/video prefer extra caption so a stored
     * preview placeholder ("Фото") does not hide the real caption.
     */
    fun clip(msg: ChatMessage): String? {
        if (msg.deleted) return null
        caption(msg)?.let { return it }
        return msg.text.takeIf { it.isNotBlank() }
    }
}
