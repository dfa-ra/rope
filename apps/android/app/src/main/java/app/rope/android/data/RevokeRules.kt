package app.rope.android.data

/**
 * Owner-only server kick. Guests never see the control — hide, do not disable.
 * Calls existing POST /v1/admin/revoke-member. Kotlin never implements crypto.
 */
object RevokeRules {
    fun canRevoke(role: String?): Boolean = RoleRules.isOwner(role)

    fun canRevokeTarget(
        actorRole: String?,
        actorMemberId: String?,
        actorDeviceId: String?,
        target: DirectoryDevice,
    ): Boolean {
        if (!canRevoke(actorRole)) return false
        if (PeerIds.same(actorDeviceId, target.deviceId)) return false
        if (PeerIds.same(actorMemberId, target.memberId)) return false
        if (RoleRules.isOwner(target.role)) return false
        return target.memberId.isNotBlank()
    }

    /**
     * Last remaining owner device must not brick the instance via revoke-device.
     * Count is live owner devices, not owner members. People UI uses
     * revoke-member only; this stays unused in UI.
     */
    fun canRevokeDevice(
        actorRole: String?,
        actorDeviceId: String?,
        target: DirectoryDevice,
        ownerDeviceCount: Int,
    ): Boolean {
        if (!canRevoke(actorRole)) return false
        if (PeerIds.same(actorDeviceId, target.deviceId)) return false
        if (RoleRules.isOwner(target.role) && ownerDeviceCount <= 1) return false
        return target.deviceId.isNotBlank()
    }

    fun actionLabel(): String = "Исключить"

    fun confirmPrompt(name: String): String {
        val who = displayName(name, "этого человека")
        return "Исключить $who с сервера?"
    }

    fun confirmBody(): String = "Все устройства этого человека потеряют доступ."

    fun confirmAction(): String = "Точно исключить"

    fun cancelAction(): String = "Отмена"

    fun noticeRevoked(name: String): String {
        val who = displayName(name, "человек")
        return "$who исключён"
    }

    /** CR/LF/NUL in a directory name must not split confirm/notice chrome. */
    private fun displayName(name: String, blank: String): String {
        if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\u0000') >= 0) {
            return blank
        }
        return name.trim().ifBlank { blank }
    }

    fun peopleHint(): String = "Исключить — доступ к вашему серверу пропадёт."

    fun settingsHint(): String = "Исключить человека — вкладка Люди. Owner only."
}
