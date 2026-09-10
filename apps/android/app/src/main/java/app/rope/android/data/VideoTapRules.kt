package app.rope.android.data

/**
 * Tap on the fullscreen viewer: photos still dismiss, in-thread video
 * pauses/resumes. Close stays on X / system back. Not CallVideoRenderer.
 * LocalStore stays v6.
 */
object VideoTapRules {
    const val PLAY = "Смотреть"
    const val PAUSE = "Пауза"

    fun tapCloses(isVideo: Boolean): Boolean = !isVideo

    fun tapToggles(isVideo: Boolean): Boolean = isVideo
}
