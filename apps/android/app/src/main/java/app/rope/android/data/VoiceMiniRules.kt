package app.rope.android.data

/**
 * Telegram-like floating voice mini-player. Shown while a voice note is
 * active (playing or paused) and the user is not looking at that thread.
 * No CallVideoRenderer. LocalStore stays v6.
 */
object VoiceMiniRules {
    const val CLOSE = "Остановить"
    const val PLAY = "Слушать"
    const val PAUSE = "Пауза"

    fun visible(
        activeId: String?,
        inPlayingThread: Boolean,
        liveCall: Boolean,
        allowChrome: Boolean,
    ): Boolean {
        if (!allowChrome || liveCall) return false
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
        return "Голосовое"
    }

    fun subtitle(speed: Float, playing: Boolean): String {
        val rate = VoicePlayback.speedLabel(speed)
        return if (playing) rate else "пауза · $rate"
    }
}
