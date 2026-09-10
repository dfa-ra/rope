package app.rope.android.data

/**
 * Telegram-like local Saved Messages / Избранное. The thread lives only in
 * LocalStore — no VPS vault, no FCM, no cloud Saved 2.0.
 */
object SavedMessagesRules {
    const val ID = "saved:"
    const val TITLE = "Избранное"
    const val IDLE_SUBTITLE = "Только на этом устройстве"
    const val IDLE_TITLE = "Избранное пусто"
    const val IDLE_BODY = "Перешлите сюда сообщения или напишите заметку."
    const val LOCAL_OBJECT_PREFIX = "local-"

    fun isSaved(id: String?): Boolean = id == ID

    fun isSaved(c: Conversation): Boolean = c.id == ID

    fun visible(mode: ChatListMode): Boolean = mode == ChatListMode.ALL

    fun canCall(id: String?): Boolean = !isSaved(id)

    fun opensPeerProfile(id: String?): Boolean = !isSaved(id)

    fun skipNetwork(id: String?): Boolean = isSaved(id)

    fun isLocalObject(objectId: String?): Boolean =
        JsonIds.optional(objectId)?.startsWith(LOCAL_OBJECT_PREFIX) == true

    fun localObjectId(raw: String): String = LOCAL_OBJECT_PREFIX + raw.trim()

    fun defaultPrefs(existing: ChatPrefs?): ChatPrefs = existing ?: ChatPrefs(pinned = true)

    fun stubPeer(): DirectoryDevice =
        DirectoryDevice(ID, "", TITLE, ByteArray(0), "", online = false)

    fun conversation(
        last: ChatMessage?,
        prefs: ChatPrefs,
        myDeviceId: String,
    ): Conversation {
        val subtitle = ChatListPreviewRules.copy(
            last = last,
            draft = prefs.draft,
            isGroup = false,
            myDeviceId = myDeviceId,
            saved = true,
        ).text
        return Conversation(
            id = ID,
            title = TITLE,
            subtitle = subtitle,
            isGroup = false,
            online = false,
            last = last,
            peer = stubPeer(),
            pinned = prefs.pinned,
            muted = prefs.muted,
            unread = 0,
            archived = prefs.archived,
            draft = prefs.draft,
        )
    }
}
