package app.rope.android.data

/**
 * Telegram-like delete. Outgoing still sends RECEIPT DELETE.
 * Incoming is local hide only — no per-member list, no new envelope.
 */
object DeleteMessageRules {
    const val LABEL = "Удалить"
    const val FOR_ME = "Удалить у себя"

    fun canShow(msg: ChatMessage): Boolean = !msg.deleted

    fun menuLabel(msg: ChatMessage): String =
        if (msg.outgoing) LABEL else FOR_ME

    /** RECEIPT DELETE is for our own bubbles. Incoming hide stays on this device. */
    fun notifyPeer(outgoing: Boolean, skipNetwork: Boolean): Boolean =
        outgoing && !skipNetwork
}
