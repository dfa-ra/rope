package app.rope.android.data

/**
 * Loudspeaker vs earpiece for in-thread voice notes. Not a call speaker.
 * Default is speaker, like Telegram voice bubbles.
 */
object VoiceSpeakerRules {
    const val DEFAULT_SPEAKER = true

    fun toggle(speakerOn: Boolean): Boolean = !speakerOn

    fun label(speakerOn: Boolean): String = if (speakerOn) "динамик" else "трубка"

    fun contentDescription(speakerOn: Boolean): String =
        if (speakerOn) "Играть в трубку" else "Играть с динамика"
}
