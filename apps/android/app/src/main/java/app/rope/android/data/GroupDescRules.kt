package app.rope.android.data

/**
 * Group description. Fail closed on CR/LF/NUL before trim. 0–120 runes.
 * Organizer or server owner may PATCH; any member may read.
 */
object GroupDescRules {
    const val MAX = 120

    fun parse(raw: String?): String? {
        if (raw == null) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val desc = raw.trim()
        if (desc.codePointCount(0, desc.length) > MAX) return null
        return desc
    }

    fun canEdit(
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
