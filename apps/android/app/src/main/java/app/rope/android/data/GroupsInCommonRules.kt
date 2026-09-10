package app.rope.android.data

/**
 * Telegram-like groups in common on a 1:1 peer profile. Local membership
 * lists only — no extra Go round-trip, no LocalStore bump.
 */
object GroupsInCommonRules {
    const val SECTION = "Общие группы"

    fun canShow(peerId: String?, selfId: String?): Boolean {
        if (peerId.isNullOrBlank() || peerId == selfId) return false
        return !SavedMessagesRules.isSaved(peerId)
    }

    fun of(groups: List<RopeGroup>, peerId: String?, selfId: String?): List<RopeGroup> {
        if (!canShow(peerId, selfId) || selfId.isNullOrBlank()) return emptyList()
        val peer = peerId!!
        val me = selfId
        return groups
            .filter { peer in it.members && me in it.members }
            .sortedBy { it.name.lowercase() }
    }

    fun sectionLabel(count: Int): String = when {
        count <= 0 -> SECTION
        else -> "$SECTION · $count"
    }
}
