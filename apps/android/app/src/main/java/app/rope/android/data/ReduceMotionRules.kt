package app.rope.android.data

/**
 * Telegram-like in-app reduce-motion. Kv-only. ORs with the system
 * animator/transition scale already read by rememberReduceMotion.
 * LocalStore schema stays v6. Logo colors stay put.
 */
object ReduceMotionRules {
    const val TITLE = "Меньше анимации"

    fun hint(): String =
        "Системная настройка анимации тоже учитывается. Цвета логотипа не меняются."

    fun shouldReduce(system: Boolean, pref: Boolean): Boolean = system || pref
}
