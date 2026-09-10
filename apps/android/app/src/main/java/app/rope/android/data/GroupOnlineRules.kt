package app.rope.android.data

/**
 * Telegram-like group header: «N участников · M в сети».
 * Uses the local presence set already on [UiState.onlineIds] — no new protocol.
 */
object GroupOnlineRules {
    const val OFFLINE = "все офлайн"

    fun onlineCount(
        members: Collection<String>,
        onlineIds: Set<String>,
        myId: String? = null,
        selfOnline: Boolean = false,
    ): Int {
        val seen = mutableSetOf<String>()
        var n = 0
        for (id in members) {
            val key = PeerIds.normalize(id)
            if (key.isBlank() || !seen.add(key)) continue
            val mine = PeerIds.same(id, myId)
            val on = if (mine) {
                selfOnline || onlineIds.any { PeerIds.same(it, id) }
            } else {
                onlineIds.any { PeerIds.same(it, id) }
            }
            if (on) n++
        }
        return n
    }

    fun subtitle(memberCount: Int, onlineCount: Int): String {
        val members = "$memberCount участников"
        if (onlineCount <= 0) return "$members · $OFFLINE"
        return "$members · $onlineCount в сети"
    }
}
