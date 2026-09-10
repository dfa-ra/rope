package app.rope.android.data

/**
 * Calls tab chips Все / Исходящие. Keys off the last CALL preview,
 * not a new hangup protocol. Distinct from Пропущенные (incoming /
 * «пропущен»). People-to-call stay on Все only.
 */
object OutgoingCallRules {
    const val CHIP_ALL = "Все"
    const val CHIP_OUTGOING = "Исходящие"
    const val EMPTY_TITLE = "Нет исходящих"
    const val EMPTY_BODY = "Нет исходящих звонков."

    fun enabledFromKv(raw: String?): Boolean = raw == "1"

    fun isOutgoing(last: ChatMessage?): Boolean {
        if (last == null || last.kind != MessageKind.CALL || last.deleted) return false
        val text = last.preview().trim()
        if (text.equals(CallLink.ringTimeoutDetail(false), ignoreCase = true)) return false
        if (text.equals(CallLink.ringTimeoutDetail(true), ignoreCase = true)) return true
        if (!last.outgoing) return false
        return text == VideoCallRules.recordLabel(video = false, outgoing = true) ||
            text == VideoCallRules.recordLabel(video = true, outgoing = true) ||
            text.equals(CallLink.offlineDetail(), ignoreCase = true)
    }

    fun recent(conversations: List<Conversation>, outgoingOnly: Boolean): List<Conversation> {
        val all = conversations.filter {
            it.last?.kind == MessageKind.CALL && !ArchiveRules.shouldHideFromMain(it)
        }
        return if (outgoingOnly) all.filter { isOutgoing(it.last) } else all
    }

    fun showPeople(outgoingOnly: Boolean): Boolean = !outgoingOnly

    fun emptyTitle(outgoingOnly: Boolean): String =
        if (outgoingOnly) EMPTY_TITLE else "Звонков ещё не было"

    fun emptyBody(outgoingOnly: Boolean, role: String?): String =
        if (outgoingOnly) EMPTY_BODY else RoleRules.callsEmptyBody(role)

    fun emptyAction(outgoingOnly: Boolean, role: String?): String? =
        if (outgoingOnly) null else RoleRules.peopleInviteAction(role)
}
