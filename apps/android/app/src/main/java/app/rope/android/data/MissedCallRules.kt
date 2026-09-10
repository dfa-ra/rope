package app.rope.android.data

/**
 * Calls tab chips Все / Пропущенные. Keys off the last CALL preview,
 * not a new hangup protocol. People-to-call stay on Все only.
 */
object MissedCallRules {
    const val CHIP_ALL = "Все"
    const val CHIP_MISSED = "Пропущенные"
    const val EMPTY_TITLE = "Нет пропущенных"
    const val EMPTY_BODY = "Нет входящих без ответа."

    fun enabledFromKv(raw: String?): Boolean = raw == "1"

    fun isMissed(last: ChatMessage?): Boolean {
        if (last == null || last.kind != MessageKind.CALL || last.deleted) return false
        val text = last.preview().trim()
        if (text.equals(CallLink.ringTimeoutDetail(false), ignoreCase = true)) return true
        if (last.outgoing) return false
        return text == VideoCallRules.recordLabel(video = false, outgoing = false) ||
            text == VideoCallRules.recordLabel(video = true, outgoing = false)
    }

    fun recent(conversations: List<Conversation>, missedOnly: Boolean): List<Conversation> {
        val all = conversations.filter {
            it.last?.kind == MessageKind.CALL && !ArchiveRules.shouldHideFromMain(it)
        }
        return if (missedOnly) all.filter { isMissed(it.last) } else all
    }

    fun showPeople(missedOnly: Boolean): Boolean = !missedOnly

    fun emptyTitle(missedOnly: Boolean): String =
        if (missedOnly) EMPTY_TITLE else "Звонков ещё не было"

    fun emptyBody(missedOnly: Boolean, role: String?): String =
        if (missedOnly) EMPTY_BODY else RoleRules.callsEmptyBody(role)

    fun emptyAction(missedOnly: Boolean, role: String?): String? =
        if (missedOnly) null else RoleRules.peopleInviteAction(role)
}
