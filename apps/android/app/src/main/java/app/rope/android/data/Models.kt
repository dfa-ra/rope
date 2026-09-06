package app.rope.android.data

enum class MessageStatus {
    CREATED,
    SENT_TO_SERVER,
    DELIVERED_TO_DEVICE,
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
)
