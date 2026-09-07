package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject

data class MediaPayload(
    val kind: String,
    val objectId: String,
    val sha256: String,
    val keyB64: String,
    val mime: String,
    val name: String,
    val size: Long,
    val durationMs: Long = 0,
    val groupId: String? = null,
) {
    fun toJson(): String = JSONObject()
        .put("kind", kind)
        .put("object_id", objectId)
        .put("sha256", sha256)
        .put("key_b64", keyB64)
        .put("mime", mime)
        .put("name", name)
        .put("size", size)
        .put("duration_ms", durationMs)
        .apply {
            JsonIds.optional(groupId)?.let { put("group_id", it) }
        }
        .toString()

    fun messageKind(): MessageKind = when (kind) {
        "voice" -> MessageKind.VOICE
        "image" -> MessageKind.IMAGE
        "file" -> MessageKind.FILE
        else -> MessageKind.UNKNOWN
    }

    fun preview(): String = when (kind) {
        "voice" -> "Голосовое · ${formatDuration(durationMs)}"
        "image" -> "Фото"
        "file" -> name.ifBlank { "Файл" }
        else -> "Вложение"
    }

    companion object {
        fun parse(raw: String): MediaPayload {
            val o = JSONObject(raw)
            return MediaPayload(
                kind = o.optString("kind"),
                objectId = o.optString("object_id"),
                sha256 = o.optString("sha256"),
                keyB64 = o.optString("key_b64"),
                mime = o.optString("mime"),
                name = o.optString("name"),
                size = o.optLong("size"),
                durationMs = o.optLong("duration_ms"),
                groupId = JsonIds.optional(o.optString("group_id")),
            )
        }

        fun formatDuration(ms: Long): String {
            val total = (ms / 1000).coerceAtLeast(0)
            return "%d:%02d".format(total / 60, total % 60)
        }
    }
}

data class GroupTextPayload(
    val groupId: String,
    val text: String,
    val epoch: Int,
) {
    fun toJson(): String = JSONObject()
        .put("g", groupId)
        .put("t", text)
        .put("e", epoch)
        .toString()

    companion object {
        fun parse(raw: String): GroupTextPayload {
            val o = JSONObject(raw)
            return GroupTextPayload(
                groupId = o.optString("g"),
                text = o.optString("t"),
                epoch = o.optInt("e"),
            )
        }
    }
}

object EnvelopeTypes {
    const val TEXT: UByte = 1u
    const val MEDIA: UByte = 2u
    const val GROUP_TEXT: UByte = 3u
    const val CALL: UByte = 4u
    const val RECEIPT: UByte = 5u

    fun kindOf(msgType: UByte, extra: String): MessageKind = when (msgType) {
        TEXT -> MessageKind.TEXT
        MEDIA -> if (extra.isBlank()) MessageKind.UNKNOWN else MediaPayload.parse(extra).messageKind()
        GROUP_TEXT -> MessageKind.GROUP_TEXT
        CALL -> MessageKind.CALL
        RECEIPT -> MessageKind.TEXT
        else -> MessageKind.UNKNOWN
    }
}

object ReactionCodec {
    fun parse(raw: String?): List<Reaction> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val emoji = o.optString("emoji").trim()
                    val deviceId = o.optString("device_id")
                    if (emoji.isNotEmpty() && deviceId.isNotBlank()) {
                        add(Reaction(emoji, deviceId, o.optString("name")))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toJson(items: List<Reaction>): String {
        val arr = JSONArray()
        items.forEach { r ->
            arr.put(
                JSONObject()
                    .put("emoji", r.emoji)
                    .put("device_id", r.deviceId)
                    .put("name", r.displayName),
            )
        }
        return arr.toString()
    }

    fun grouped(items: List<Reaction>): List<Pair<String, List<Reaction>>> =
        items.groupBy { it.emoji }.entries.map { it.key to it.value }
}

data class Reaction(
    val emoji: String,
    val deviceId: String,
    val displayName: String = "",
)

data class ReactionPayload(
    val targetId: String,
    val emoji: String,
    val op: String,
) {
    fun toJson(): String = JSONObject()
        .put("v", 1)
        .put("kind", "reaction")
        .put("target", targetId)
        .put("emoji", emoji)
        .put("op", op)
        .toString()

    companion object {
        const val SET = "set"
        const val CLEAR = "clear"
        val EMOJIS = listOf("👍", "❤️", "😂", "🔥", "😮", "😢", "👏")

        fun parse(raw: String): ReactionPayload? {
            val o = JSONObject(raw)
            if (o.optString("kind") != "reaction") return null
            val target = JsonIds.optional(o.optString("target")) ?: return null
            val emoji = o.optString("emoji").trim()
            if (emoji.isEmpty()) return null
            val op = o.optString("op").ifBlank { SET }
            return ReactionPayload(target, emoji, op)
        }
    }
}

object MessageTime {
    fun label(ms: Long, now: Long = System.currentTimeMillis()): String {
        if (ms <= 0L) return ""
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        val today = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val hm = "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        val sameDay = cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
        return if (sameDay) hm else "${cal.get(java.util.Calendar.DAY_OF_MONTH)}.${cal.get(java.util.Calendar.MONTH) + 1} $hm"
    }

    fun meta(status: MessageStatus, outgoing: Boolean, timestampMs: Long, now: Long = System.currentTimeMillis()): String {
        val time = label(timestampMs, now)
        val mark = ComposerRules.statusLabel(status, outgoing)
        return listOf(time, mark).filter { it.isNotBlank() }.joinToString(" · ")
    }
}

object ComposerRules {
    /** Hold-to-record is a single gesture. */
    const val VOICE_TAPS_TO_SEND = 1

    /** Attach → pick from gallery. */
    const val PHOTO_TAPS_TO_SEND = 2

    /** Full-screen incoming call: one tap to answer. */
    const val ANSWER_CALL_TAPS = 1

    /** Chats FAB → name → create. */
    const val GROUP_CREATE_TAPS = 2

    fun showSendButton(draft: String, recording: Boolean): Boolean =
        draft.isNotBlank() && !recording

    fun showMicButton(draft: String, recording: Boolean): Boolean =
        draft.isBlank() || recording

    fun statusLabel(status: MessageStatus, outgoing: Boolean): String = when (status) {
        MessageStatus.CREATED -> "ожидает"
        MessageStatus.SENT_TO_SERVER -> "на сервере"
        MessageStatus.DELIVERED_TO_DEVICE -> if (outgoing) "доставлено" else ""
    }
}
