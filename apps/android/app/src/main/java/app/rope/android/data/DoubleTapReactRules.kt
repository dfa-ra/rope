package app.rope.android.data

/**
 * Telegram-like double-tap on a bubble applies the first quick reaction (❤️).
 * Uses the existing [RopeRepository.react] toggle; no store bump.
 */
object DoubleTapReactRules {
    val EMOJI: String = ReactionPayload.EMOJIS.first()

    fun canReact(msg: ChatMessage, selecting: Boolean): Boolean =
        !msg.deleted && !selecting

    fun emojiOrNull(msg: ChatMessage, selecting: Boolean): String? =
        if (canReact(msg, selecting)) EMOJI else null
}
