package app.rope.android.data

/**
 * Calls tab chips Все / Входящие. Keys off the last CALL preview,
 * not a new hangup protocol. Distinct from Пропущенные (unanswered
 * only) and Исходящие. People-to-call stay on Все only.
 */
object IncomingCallRules {
    const val CHIP_ALL = "Все"
    const val CHIP_INCOMING = "Входящие"
    const val EMPTY_TITLE = "Нет входящих"
    const val EMPTY_BODY = "Нет входящих звонков."

    fun enabledFromKv(raw: String?): Boolean = raw == "1"

    fun isIncoming(last: ChatMessage?): Boolean {
        if (last == null || last.kind != MessageKind.CALL || last.deleted) return false
        val text = last.preview().trim()
        if (text.equals(CallLink.ringTimeoutDetail(false), ignoreCase = true)) return true
        if (text.equals(CallLink.ringTimeoutDetail(true), ignoreCase = true)) return false
        if (last.outgoing) return false
        return text == VideoCallRules.recordLabel(video = false, outgoing = false) ||
            text == VideoCallRules.recordLabel(video = true, outgoing = false)
    }

    fun recent(conversations: List<Conversation>, incomingOnly: Boolean): List<Conversation> {
        val all = conversations.filter {
            it.last?.kind == MessageKind.CALL && !ArchiveRules.shouldHideFromMain(it)
        }
        return if (incomingOnly) all.filter { isIncoming(it.last) } else all
    }

    fun showPeople(incomingOnly: Boolean): Boolean = !incomingOnly

    fun emptyTitle(incomingOnly: Boolean): String =
        if (incomingOnly) EMPTY_TITLE else "Звонков ещё не было"

    fun emptyBody(incomingOnly: Boolean, role: String?): String =
        if (incomingOnly) EMPTY_BODY else RoleRules.callsEmptyBody(role)

    fun emptyAction(incomingOnly: Boolean, role: String?): String? =
        if (incomingOnly) null else RoleRules.peopleInviteAction(role)
}
