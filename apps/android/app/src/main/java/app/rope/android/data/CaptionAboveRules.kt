package app.rope.android.data

/**
 * Telegram Chat Settings: show photo/video captions above the media.
 * Kv only; LocalStore stays v6. Go never sees it.
 */
object CaptionAboveRules {
    const val LABEL = "Подпись сверху"

    fun hint(): String =
        "Подпись над фото и видео, как в Telegram. На сервер не уходит."

    fun showAbove(enabled: Boolean): Boolean = enabled

    fun showBelow(enabled: Boolean): Boolean = !enabled
}
