package app.rope.android.data

/**
 * Mute audio on in-thread video notes (кружок). Not CallVideoRenderer. No FCM.
 */
object VideoNoteMuteRules {
    fun toggle(muted: Boolean): Boolean = !muted

    fun volume(muted: Boolean): Float = if (muted) 0f else 1f

    fun contentDescription(muted: Boolean): String =
        if (muted) "Включить звук" else "Выключить звук"
}
