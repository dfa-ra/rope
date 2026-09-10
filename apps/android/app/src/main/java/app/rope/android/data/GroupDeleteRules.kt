package app.rope.android.data

/**
 * DELETE /v1/groups/{id} for everyone. Fail closed on CR/LF/NUL in the id
 * before trim. Organizer or server owner; caller must still be a member.
 */
object GroupDeleteRules {
    private val uuid = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    fun parseId(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val id = raw.trim()
        if (id.isEmpty() || id != raw) return null
        if (!uuid.matches(id)) return null
        return id
    }

    fun canDelete(
        isMember: Boolean,
        myId: String?,
        organizerId: String,
        serverRole: String?,
    ): Boolean {
        if (!isMember) return false
        if (RoleRules.isOwner(serverRole)) return true
        return !myId.isNullOrBlank() && myId == organizerId
    }
}
