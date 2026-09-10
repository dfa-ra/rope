package app.rope.android.data

/**
 * Loop chip on in-thread video and the full-screen viewer.
 * Video notes already loop; this is regular video. Not CallVideoRenderer.
 */
object VideoLoopRules {
    fun toggle(looping: Boolean): Boolean = !looping

    fun label(looping: Boolean): String = if (looping) "повтор" else "один раз"

    fun contentDescription(looping: Boolean): String =
        if (looping) "Играть один раз" else "Зациклить"
}
