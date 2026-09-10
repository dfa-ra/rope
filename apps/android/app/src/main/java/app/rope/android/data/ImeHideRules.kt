package app.rope.android.data

/**
 * Telegram-like hide-IME on a user drag of the chat list or thread.
 * Programmatic jumps and fling leftovers do not dismiss the keyboard.
 * No Go, no LocalStore bump.
 */
object ImeHideRules {
    const val MIN_DY = 2f

    fun isUserDrag(source: String): Boolean = when (source) {
        "Drag", "UserInput" -> true
        else -> false
    }

    fun shouldHide(deltaY: Float, userDrag: Boolean): Boolean =
        userDrag && kotlin.math.abs(deltaY) >= MIN_DY
}
