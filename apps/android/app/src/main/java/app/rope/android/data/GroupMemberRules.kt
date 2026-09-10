package app.rope.android.data

/**
 * Telegram-like tap on a group member in group info. Opens that person's
 * 1:1 chat. No LocalStore bump, no Go.
 */
object GroupMemberRules {
    const val ACTION = "Написать"
    const val NOTICE_MISSING = "Нет в справочнике"

    fun canOpen(memberId: String?, selfId: String?): Boolean {
        val member = PeerIds.normalize(memberId)
        if (member.isEmpty() || SavedMessagesRules.isSaved(member) || ChatIds.isGroup(member)) {
            return false
        }
        val self = PeerIds.normalize(selfId)
        return self.isEmpty() || member != self
    }
}
