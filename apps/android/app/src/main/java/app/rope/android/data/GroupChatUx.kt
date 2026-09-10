package app.rope.android.data

/**
 * Group-thread and group-list copy taken from Telegram / WhatsApp / Signal.
 * Protocol stays group_text + RECEIPT + existing group REST.
 */
object GroupChatUx {
    const val YOU = "Вы"
    const val CLUSTER_GAP_MS = 5 * 60 * 1000L

    val SENDER_COLORS = intArrayOf(
        0xFF7C9CB8.toInt(),
        0xFF8BA67A.toInt(),
        0xFFC4A574.toInt(),
        0xFFB08080.toInt(),
        0xFF9A8BB8.toInt(),
        0xFF7AADB0.toInt(),
    )

    fun listPreview(last: ChatMessage?, myDeviceId: String = ""): String? {
        if (last == null) return null
        val body = TextFmtRules.plain(last.preview())
        val name = when {
            last.outgoing || (myDeviceId.isNotBlank() && last.senderId == myDeviceId) -> YOU
            last.senderName.isNotBlank() -> last.senderName
            last.senderId.isNotBlank() -> last.senderId.take(8)
            else -> ""
        }
        return if (name.isBlank()) body else "$name: $body"
    }

    fun groupSubtitle(last: ChatMessage?, memberCount: Int, myDeviceId: String = ""): String =
        listPreview(last, myDeviceId) ?: "$memberCount участников"

    fun showSenderName(isGroup: Boolean, outgoing: Boolean, firstInCluster: Boolean): Boolean =
        isGroup && !outgoing && firstInCluster

    fun typingLine(names: List<String>, isGroup: Boolean = true): String {
        val clean = names.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (clean.isEmpty()) return ""
        if (!isGroup) return "печатает…"
        return when (clean.size) {
            1 -> "${clean[0]} печатает…"
            2 -> "${clean[0]} и ${clean[1]} печатают…"
            3 -> "${clean[0]}, ${clean[1]} и ${clean[2]} печатают…"
            else -> "${clean[0]}, ${clean[1]} и ещё ${clean.size - 2} печатают…"
        }
    }

    fun senderKey(msg: ChatMessage): String =
        msg.senderId.ifBlank { if (msg.outgoing) "out" else msg.peerDeviceId }

    fun sameCluster(prev: ChatMessage?, next: ChatMessage, gapMs: Long = CLUSTER_GAP_MS): Boolean {
        if (prev == null) return false
        if (prev.outgoing != next.outgoing) return false
        if (senderKey(prev) != senderKey(next)) return false
        if (!DateSeparatorRules.sameDay(prev.timestampMs, next.timestampMs)) return false
        return kotlin.math.abs(next.timestampMs - prev.timestampMs) <= gapMs
    }

    fun firstInCluster(messages: List<ChatMessage>, index: Int): Boolean {
        val cur = messages.getOrNull(index) ?: return true
        return !sameCluster(messages.getOrNull(index - 1), cur)
    }

    fun lastInCluster(messages: List<ChatMessage>, index: Int): Boolean {
        val cur = messages.getOrNull(index) ?: return true
        val following = messages.getOrNull(index + 1) ?: return true
        return !sameCluster(cur, following)
    }

    fun senderColorArgb(senderId: String, senderName: String = ""): Int {
        val key = senderId.ifBlank { senderName }
        if (key.isBlank()) return SENDER_COLORS[0]
        return SENDER_COLORS[Math.floorMod(key.hashCode(), SENDER_COLORS.size)]
    }

    fun mentionSpans(text: String, names: List<String>): List<IntRange> {
        if (text.isEmpty()) return emptyList()
        val sorted = names.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            .sortedByDescending { it.length }
        if (sorted.isEmpty()) return emptyList()
        val found = mutableListOf<IntRange>()
        val lower = text.lowercase()
        for (name in sorted) {
            val needle = "@${name.lowercase()}"
            var from = 0
            while (true) {
                val at = lower.indexOf(needle, from)
                if (at < 0) break
                val end = at + needle.length
                val overlaps = found.any { it.first < end && at < it.last + 1 }
                if (!overlaps) found += at until end
                from = at + 1
            }
        }
        return found.sortedBy { it.first }
    }

    fun organizerId(group: RopeGroup): String =
        group.createdBy.ifBlank { group.members.firstOrNull().orEmpty() }

    fun memberRoleLabel(deviceId: String, group: RopeGroup): String =
        if (deviceId.isNotBlank() && deviceId == organizerId(group)) "организатор" else "участник"

    fun memberDisplayName(deviceId: String, myId: String?, names: Map<String, String>): String =
        if (deviceId == myId) YOU else names[deviceId]?.ifBlank { null } ?: deviceId.take(8)

    fun replyQuoteName(replyName: String, outgoing: Boolean): String = when {
        replyName.isNotBlank() -> replyName
        outgoing -> YOU
        else -> "Ответ"
    }
}
