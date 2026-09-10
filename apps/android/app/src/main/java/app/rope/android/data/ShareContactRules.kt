package app.rope.android.data

/**
 * Telegram-like share-contact from a 1:1 peer profile. Sends a local TEXT
 * card (name + device id) into another chat — not a forwarded message,
 * no LocalStore bump, no Go contact type.
 */
object ShareContactRules {
    const val ACTION = "Поделиться"
    const val BANNER = "Поделиться контактом"
    const val FALLBACK_NAME = "контакт"
    const val REJECT = "Нельзя отправить контакт в этот чат."

    fun canShare(peerId: String?, selfId: String?): Boolean {
        val peer = peerId?.trim().orEmpty()
        if (peer.isEmpty() || SavedMessagesRules.isSaved(peer)) return false
        val self = selfId?.trim().orEmpty()
        return self.isEmpty() || peer != self
    }

    fun canSendTo(destChatId: String?, peerId: String?): Boolean {
        val dest = destChatId?.trim().orEmpty()
        val peer = peerId?.trim().orEmpty()
        return dest.isNotEmpty() && peer.isNotEmpty() && dest != peer
    }

    fun displayName(name: String?): String =
        name?.trim().orEmpty().ifBlank { FALLBACK_NAME }

    fun body(name: String?, deviceId: String): String =
        "${displayName(name)}\n${deviceId.trim()}"

    fun preview(name: String?): String = displayName(name)
}
