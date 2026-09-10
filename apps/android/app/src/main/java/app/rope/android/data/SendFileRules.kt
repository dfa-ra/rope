package app.rope.android.data

/**
 * Telegram-like "send as file": skip photo/video compression and album packing.
 * Inner JSON stays type=2 with kind=file. Go never sees it. Not envelope crypto.
 */
object SendFileRules {
    const val ACTION = "Как файл"
    const val KIND = "file"
    const val HINT_BODY = "без сжатия, как документ"

    fun kind(mime: String, name: String, asFile: Boolean): String =
        if (asFile) KIND else VideoRules.kind(mime, name)

    /** Файл picker and «Как файл» skip ImageCodec / VideoCodec. */
    fun skipVisualPrep(asFile: Boolean): Boolean = asFile

    /** Gallery / recents stage photos into the album editor; as-file does not. */
    fun stageVisual(looksVisual: Boolean, asFile: Boolean): Boolean =
        looksVisual && !asFile

    fun albumEligible(kind: String, asFile: Boolean): Boolean =
        !asFile && VideoRules.albumEligible(kind)

    fun hint(count: Int): ComposerHintCopy {
        val n = count.coerceAtLeast(1)
        return ComposerHintCopy(
            kind = ComposerHintKind.MEDIA,
            title = if (n == 1) "Файл" else "Файлы",
            body = HINT_BODY,
            dismissContentDescription = MediaSendRules.DISMISS,
        )
    }
}
