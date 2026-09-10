package app.rope.android.data

/**
 * Telegram-like one-shot «notify when this 1:1 peer comes online».
 * Local heads-up on the live WSS presence list. LocalStore stays v6. Not FCM.
 */
object OnlineAlertRules {
    const val LABEL = "Сообщить, когда в сети"
    const val HINT = "Один раз, когда этот человек появится в сети."
    const val BODY = "появился(ась) в сети"
    const val FALLBACK_TITLE = "Контакт"
    const val MAX_ID = 128

    fun sanitizeId(raw: String?): String? {
        if (raw == null) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val id = raw.trim()
        if (id.isEmpty() || id.length > MAX_ID) return null
        if (id.any { ch -> ch != ':' && ch != '-' && !ch.isLetterOrDigit() }) return null
        if (ChatIds.isGroup(id) || SavedMessagesRules.isSaved(id)) return null
        return id
    }

    fun canEnable(id: String?, isGroup: Boolean = false, saved: Boolean = false): Boolean {
        if (isGroup || saved) return false
        return sanitizeId(id) != null
    }

    fun armedOf(conversations: List<Conversation>, peerId: String?): Boolean {
        val id = sanitizeId(peerId) ?: return false
        return conversations.any { it.onlineAlert && PeerIds.same(it.id, id) }
    }

    fun appeared(wasOnline: Boolean, nowOnline: Boolean): Boolean = !wasOnline && nowOnline

    fun watching(appForeground: Boolean, onThisPeer: Boolean): Boolean =
        appForeground && onThisPeer

    fun shouldNotify(
        armed: Boolean,
        wasOnline: Boolean,
        nowOnline: Boolean,
        globalMuted: Boolean,
        watching: Boolean,
    ): Boolean {
        if (!armed || globalMuted || watching) return false
        return appeared(wasOnline, nowOnline)
    }

    fun shouldConsume(armed: Boolean, wasOnline: Boolean, nowOnline: Boolean): Boolean =
        armed && appeared(wasOnline, nowOnline)

    fun consume(prefs: ChatPrefs): ChatPrefs = prefs.copy(onlineAlert = false)

    fun title(name: String?): String = name?.trim().orEmpty().ifBlank { FALLBACK_TITLE }

    fun notifyId(peerId: String): Int = ("online-alert:$peerId").hashCode()

    fun present(id: String, onlineIds: Set<String>): Boolean {
        val n = sanitizeId(id) ?: return false
        return onlineIds.any { PeerIds.same(sanitizeId(it), n) }
    }
}
