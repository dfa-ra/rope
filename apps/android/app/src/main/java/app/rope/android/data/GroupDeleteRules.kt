package app.rope.android.data

/**
 * Organizer confirm **Удалить группу**. Local [LocalStore.deleteGroup] only.
 * Distinct from leave. Do not call Go DELETE.
 */
object GroupDeleteRules {
    const val LABEL = "Удалить группу"
    const val CONFIRM = "Точно удалить группу"
    const val CANCEL = "Отмена"
    const val NOTICE = "Группа удалена"

    fun canDelete(
        isMember: Boolean,
        myId: String?,
        organizerId: String,
        serverRole: String?,
    ): Boolean = RoleRules.canManageGroupMembers(isMember, myId, organizerId, serverRole)

    fun notice(name: String): String {
        val n = name.trim()
        if (n.isEmpty() || '\n' in n || '\r' in n || '\u0000' in n) return NOTICE
        return "«$n» удалена"
    }
}
