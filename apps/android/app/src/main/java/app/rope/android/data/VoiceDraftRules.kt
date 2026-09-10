package app.rope.android.data

/**
 * Keep a locked voice take when leaving the thread. Unlocked hold still
 * cancels. Not a text draft. LocalStore stays v6.
 */
data class VoiceDraft(
    val chatId: String,
    val path: String,
    val durationMs: Long,
    val waveform: List<Int> = emptyList(),
)

object VoiceDraftRules {
    fun keep(locked: Boolean, recording: Boolean): Boolean = locked && recording

    fun parseChatId(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (hasCtl(raw)) return null
        val id = JsonIds.optional(raw) ?: return null
        if (hasCtl(id) || id.length > 128) return null
        return id
    }

    fun parsePath(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (hasCtl(raw)) return null
        val v = raw.trim()
        return v.takeIf { it.isNotEmpty() }
    }

    fun sameChat(draftChatId: String?, openChatId: String?): Boolean {
        val a = parseChatId(draftChatId) ?: return false
        val b = parseChatId(openChatId) ?: return false
        return a == b
    }

    fun restore(draft: VoiceDraft?, openChatId: String?): VoiceDraft? {
        val d = draft ?: return null
        if (!sameChat(d.chatId, openChatId)) return null
        if (parsePath(d.path) == null) return null
        if (d.durationMs < 0L) return null
        return d
    }

    fun cancelOnLeave(locked: Boolean): Boolean = !locked

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0
}
