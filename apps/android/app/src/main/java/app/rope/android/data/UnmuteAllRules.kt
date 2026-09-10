package app.rope.android.data

/**
 * Settings «Включить звук во всех»: clears per-chat mute.
 * Does not flip the global «Без звука» switch. Not FCM.
 */
object UnmuteAllRules {
    fun unmute(prefs: ChatPrefs): ChatPrefs =
        if (!prefs.muted) prefs else prefs.copy(muted = false)

    fun mutedCount(prefs: Map<String, ChatPrefs>): Int =
        prefs.values.count { it.muted }

    fun mutedCount(conversations: List<Conversation>): Int =
        conversations.count { it.muted }

    fun anyMuted(count: Int): Boolean = count > 0

    fun unmuted(all: Map<String, ChatPrefs>): Map<String, ChatPrefs> =
        all.mapValues { unmute(it.value) }

    fun actionLabel(): String = "Включить звук во всех"

    fun hint(): String =
        "Снимает без звука с чатов в списке. Глобальный переключатель не трогает."

    fun notice(count: Int): String = when {
        count <= 0 -> ""
        count == 1 -> "Звук включён в 1 чате"
        else -> "Звук включён в $count чатах"
    }
}
