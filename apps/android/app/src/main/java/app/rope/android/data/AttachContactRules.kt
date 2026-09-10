package app.rope.android.data

/**
 * Attach-sheet **Контакт**. Picks a 1:1 directory peer and sends a local TEXT
 * card (name + device id) into the open thread. Distinct from profile
 * «Поделиться» (share that peer into another chat). No new MessageKind,
 * no Android address book, no LocalStore bump.
 */
object AttachContactRules {
    const val LABEL = "Контакт"
    const val TITLE = "Контакт"
    const val EMPTY = "Нет контактов"
    const val EMPTY_BODY = "Когда появятся люди, их можно будет отправить сюда."
    const val REJECT = "Нельзя отправить этот контакт."
    const val FALLBACK_NAME = "контакт"
    const val MAX_ID = 128
    const val MAX_NAME = 64

    fun cleanId(raw: String?): String? {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty() || t.length > MAX_ID) return null
        if (t.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return t
    }

    fun displayName(name: String?): String {
        val t = buildString {
            for (ch in name.orEmpty().trim()) {
                append(if (ch == '\n' || ch == '\r' || ch == '\u0000') ' ' else ch)
            }
        }.trim().take(MAX_NAME)
        return t.ifBlank { FALLBACK_NAME }
    }

    fun canPick(deviceId: String?, selfId: String?): Boolean {
        val id = cleanId(deviceId) ?: return false
        if (SavedMessagesRules.isSaved(id)) return false
        val self = cleanId(selfId) ?: return true
        return !PeerIds.same(id, self) && id != self
    }

    fun canSendInto(peerId: String?, groupId: String?): Boolean {
        val gid = groupId?.trim().orEmpty()
        if (gid.isNotEmpty()) {
            return gid.length <= MAX_ID && gid.none { it == '\n' || it == '\r' || it == '\u0000' }
        }
        return cleanId(peerId) != null
    }

    fun candidates(devices: List<DirectoryDevice>, selfId: String?): List<DirectoryDevice> =
        devices.filter { canPick(it.deviceId, selfId) }
            .sortedBy { displayName(it.displayName).lowercase() }

    fun pick(devices: List<DirectoryDevice>, deviceId: String?, selfId: String?): DirectoryDevice? {
        if (!canPick(deviceId, selfId)) return null
        val id = cleanId(deviceId) ?: return null
        return devices.find { PeerIds.same(it.deviceId, id) || cleanId(it.deviceId) == id }
            ?.takeIf { canPick(it.deviceId, selfId) }
    }

    fun body(name: String?, deviceId: String): String? {
        val id = cleanId(deviceId) ?: return null
        if (SavedMessagesRules.isSaved(id)) return null
        val text = "${displayName(name)}\n$id"
        if (text.any { it == '\r' || it == '\u0000' }) return null
        return text
    }
}
