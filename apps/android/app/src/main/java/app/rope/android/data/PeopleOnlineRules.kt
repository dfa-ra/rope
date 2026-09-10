package app.rope.android.data

/**
 * Telegram-like People filter: show only contacts currently online.
 * Session chrome only — no LocalStore bump.
 */
object PeopleOnlineRules {
    const val CHIP = "В сети"
    const val EMPTY_TITLE = "Никого нет в сети"
    const val EMPTY_BODY = "Кто выйдет в сеть — появится здесь."

    fun visible(people: List<DirectoryDevice>, onlineOnly: Boolean): List<DirectoryDevice> =
        if (!onlineOnly) people else people.filter { it.online }
}
