package app.rope.android.data

/**
 * Mute in-thread video (bubble + viewer). Not CallVideoRenderer. No FCM.
 */
object VideoMuteRules {
    fun toggle(muted: Boolean): Boolean = !muted

    fun volume(muted: Boolean): Float = if (muted) 0f else 1f

    fun label(muted: Boolean): String = if (muted) "без звука" else "звук"

    fun contentDescription(muted: Boolean): String =
        if (muted) "Включить звук" else "Выключить звук"
}
