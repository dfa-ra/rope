package app.rope.android.data

/**
 * Telegram-like floating mini-player for in-thread video (not the call overlay).
 * Shown while a clip is playing or paused and the user is not looking at that
 * thread. No CallVideoRenderer. LocalStore stays v6.
 */
object VideoPipRules {
    const val CLOSE = "Закрыть"
    const val PLAY = "Смотреть"
    const val PAUSE = "Пауза"
    const val FALLBACK_TITLE = "Видео"

    fun visible(
        activeId: String?,
        inPlayingThread: Boolean,
        liveCall: Boolean,
        signedIn: Boolean,
        viewing: Boolean,
    ): Boolean {
        if (!signedIn || liveCall || viewing) return false
        if (activeId.isNullOrBlank()) return false
        return !inPlayingThread
    }

    fun inPlayingThread(screenIsChat: Boolean, openChatId: String?, clipChatId: String?): Boolean {
        if (!screenIsChat) return false
        if (openChatId.isNullOrBlank() || clipChatId.isNullOrBlank()) return false
        return openChatId == clipChatId
    }

    fun title(groupName: String?, peerName: String?, saved: Boolean): String {
        if (saved) return SavedMessagesRules.TITLE
        val group = groupName?.trim().orEmpty()
        if (group.isNotEmpty()) return group
        val peer = peerName?.trim().orEmpty()
        if (peer.isNotEmpty()) return peer
        return FALLBACK_TITLE
    }

    fun subtitle(playing: Boolean): String = if (playing) "видео" else "пауза"

    fun showInline(
        playingId: String?,
        messageId: String,
        screenIsChat: Boolean,
        viewing: Boolean,
        liveCall: Boolean,
    ): Boolean {
        if (playingId.isNullOrBlank() || playingId != messageId) return false
        return screenIsChat && !viewing && !liveCall
    }
}
