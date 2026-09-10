package app.rope.android.data

/**
 * Send a gallery clip as a round video note (кружок). Same inner kind as
 * hold-to-record; envelope type stays 2. LocalStore stays v6.
 */
object NoteGalleryRules {
    const val ACTION = "Как видеосообщение"
    const val TITLE = "Видеосообщение"
    const val TOO_LONG = "до 60 с для видеосообщения"
    const val HINT_BODY = "кружок из галереи"

    fun shows(count: Int, videos: Int, durationMs: Long): Boolean =
        count == 1 && videos == 1 && durationOk(durationMs)

    fun durationOk(durationMs: Long): Boolean =
        durationMs == 0L || VideoNoteRules.fitsDuration(durationMs)

    fun sendAsNote(want: Boolean, count: Int, videos: Int, durationMs: Long): Boolean =
        want && shows(count, videos, durationMs)

    fun kind(asNote: Boolean): String =
        if (asNote) VideoNoteRules.KIND else "video"

    fun caption(asNote: Boolean, caption: String?): String? =
        if (asNote) null else caption

    fun keepToggle(want: Boolean, count: Int): Boolean = want && count == 1

    fun hint(asNote: Boolean, count: Int, videos: Int): ComposerHintCopy =
        if (asNote) {
            ComposerHintCopy(
                kind = ComposerHintKind.MEDIA,
                title = TITLE,
                body = HINT_BODY,
                dismissContentDescription = MediaSendRules.DISMISS,
            )
        } else {
            MediaSendRules.hint(count, videos)
        }
}
