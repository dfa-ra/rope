package app.rope.android.data

/**
 * Telegram-like "create group" from a 1:1 peer profile. Pre-checks that
 * person on the existing New Group screen. No LocalStore bump.
 */
object NewGroupWithRules {
    const val ACTION = "Создать группу"

    fun canStart(peerId: String?, selfId: String?): Boolean {
        if (peerId.isNullOrBlank() || peerId == selfId) return false
        return !SavedMessagesRules.isSaved(peerId)
    }

    fun picks(peerId: String): Set<String> = setOf(peerId)
}
