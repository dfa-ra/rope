package app.rope.android.data

enum class MessageStatus {
    CREATED,
    SENT_TO_SERVER,
    DELIVERED_TO_DEVICE,
}

enum class MessageKind {
    TEXT,
    VOICE,
    IMAGE,
    FILE,
    GROUP_TEXT,
    CALL,
    UNKNOWN,
}

data class ServerProfile(
    val host: String,
    val port: Int,
    val serverId: String,
    val fingerprint: String,
    val useTls: Boolean,
    val role: String,
    val memberId: String,
    val deviceId: String,
    val displayName: String = "",
    val iceServersJson: String = "",
)

data class SshTarget(
    val host: String,
    val sshPort: Int = 22,
    val user: String = "root",
    val listenPort: Int = 8443,
)

data class DirectoryDevice(
    val deviceId: String,
    val memberId: String,
    val displayName: String,
    val publicIdentity: ByteArray,
    val lastSeen: String,
    val online: Boolean = false,
)

data class ChatMessage(
    val id: String,
    val peerDeviceId: String,
    val outgoing: Boolean,
    val text: String,
    val status: MessageStatus,
    val timestampMs: Long,
    val envelope: ByteArray? = null,
    val kind: MessageKind = MessageKind.TEXT,
    val extra: String = "",
    val groupId: String? = null,
    val localPath: String? = null,
    val senderId: String = "",
    val senderName: String = "",
    val reactions: List<Reaction> = emptyList(),
    val replyToId: String? = null,
    val replyPreview: String = "",
    val replyName: String = "",
    val edited: Boolean = false,
    val deleted: Boolean = false,
) {
    fun preview(): String = when {
        deleted -> "Сообщение удалено"
        text.isNotBlank() -> text
        else -> "Сообщение"
    }
}

data class RopeGroup(
    val groupId: String,
    val name: String,
    val epoch: Int,
    val members: List<String>,
)

data class Conversation(
    val id: String,
    val title: String,
    val subtitle: String,
    val isGroup: Boolean,
    val online: Boolean,
    val last: ChatMessage?,
    val peer: DirectoryDevice? = null,
    val group: RopeGroup? = null,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val unread: Int = 0,
)

enum class CallPhase { RINGING_IN, RINGING_OUT, ACTIVE, ENDED }

data class CallInfo(
    val callId: String,
    val peerDeviceId: String,
    val peerName: String,
    val outgoing: Boolean,
    val phase: CallPhase,
    val payload: String = "",
    val media: String = "",
    val link: CallLinkState = CallLinkState.RINGING,
    val hasTurn: Boolean = false,
)

object ChatIds {
    const val GROUP_PREFIX = "g:"

    fun group(groupId: String): String = GROUP_PREFIX + groupId

    fun isGroup(id: String): Boolean = id.startsWith(GROUP_PREFIX)

    fun rawGroupId(id: String): String = id.removePrefix(GROUP_PREFIX)

    fun isOpenableGroup(id: String): Boolean {
        if (!isGroup(id)) return false
        return JsonIds.optional(rawGroupId(id)) != null
    }
}

/** Android org.json.optString(JSONObject.NULL) returns the literal "null". */
object JsonIds {
    fun optional(raw: String?): String? {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty() || v.equals("null", ignoreCase = true)) return null
        return v
    }
}

object ChatRouting {
    fun mediaChatId(groupId: String?, senderDeviceId: String, knownGroups: Set<String>): String {
        val gid = JsonIds.optional(groupId)
        return if (gid != null && gid in knownGroups) ChatIds.group(gid) else senderDeviceId
    }

    fun showLeftoverThread(id: String): Boolean = !ChatIds.isGroup(id)
}

enum class ThemeMode { LIGHT, DARK }
