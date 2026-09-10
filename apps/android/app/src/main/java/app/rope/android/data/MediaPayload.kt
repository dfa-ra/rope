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
    val albumId: String? = null,
    val albumIndex: Int = 0,
    val albumCount: Int = 1,
    val forwardedFrom: String? = null,
    val caption: String? = null,
    val replyTo: String? = null,
    val replyPreview: String = "",
    val replyName: String = "",
    val quoteText: String = "",
    val quoteStart: Int = -1,
    val quoteEnd: Int = -1,
    val waveform: List<Int> = emptyList(),
) {
    fun withReply(
        replyTo: String?,
        replyPreview: String = "",
        replyName: String = "",
        quoteText: String = "",
        quoteStart: Int = -1,
        quoteEnd: Int = -1,
    ): MediaPayload = copy(
        replyTo = replyTo,
        replyPreview = replyPreview,
        replyName = replyName,
        quoteText = quoteText,
        quoteStart = quoteStart,
        quoteEnd = quoteEnd,
    )

    fun withoutReply(): MediaPayload = copy(
        replyTo = null,
        replyPreview = "",
        replyName = "",
        quoteText = "",
        quoteStart = -1,
        quoteEnd = -1,
    )

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
            val grouped = JsonIds.optional(albumId)
            if (grouped != null && albumCount > 1) {
                put("album_id", grouped)
                put("album_index", albumIndex.coerceAtLeast(0))
                put("album_count", albumCount)
            }
            JsonIds.optional(forwardedFrom)?.let { put("ff", it) }
            JsonIds.optional(caption)?.let { put("caption", it) }
            JsonIds.optional(replyTo)?.let { put("r", it) }
            if (replyPreview.isNotBlank()) put("rp", replyPreview)
            if (replyName.isNotBlank()) put("rn", replyName)
            QuoteSpanRules.put(this, quoteText, quoteStart, quoteEnd)
            if (waveform.isNotEmpty()) {
                val arr = JSONArray()
                waveform.take(VoicePlayback.BARS).forEach { arr.put(it.coerceIn(0, 31)) }
                put("wf", arr)
            }
        }
        .toString()

    fun messageKind(): MessageKind = when (kind) {
        "voice" -> MessageKind.VOICE
        "image" -> MessageKind.IMAGE
        "video" -> MessageKind.VIDEO
        VideoNoteRules.KIND -> MessageKind.VIDEO_NOTE
        "file" -> MessageKind.FILE
        else -> MessageKind.UNKNOWN
    }

    fun preview(): String {
        val cap = JsonIds.optional(caption)
        return when (kind) {
            "voice" -> "Голосовое · ${formatDuration(durationMs)}"
            "image" -> cap ?: if (!albumId.isNullOrBlank() && albumCount > 1) {
                AlbumRules.preview(albumCount, videoCount = 0)
            } else {
                "Фото"
            }
            "video" -> cap ?: if (!albumId.isNullOrBlank() && albumCount > 1) {
                AlbumRules.preview(albumCount, videoCount = albumCount)
            } else {
                VideoRules.preview(durationMs)
            }
            VideoNoteRules.KIND -> VideoNoteRules.preview(durationMs)
            "file" -> name.ifBlank { "Файл" }
            else -> cap ?: "Вложение"
        }
    }

    companion object {
        fun parse(raw: String): MediaPayload {
            val o = JSONObject(raw)
            val quote = QuoteSpanRules.read(o)
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
                albumId = JsonIds.optional(o.optString("album_id")),
                albumIndex = o.optInt("album_index", 0).coerceAtLeast(0),
                albumCount = o.optInt("album_count", 1).let { if (it <= 0) 1 else it },
                forwardedFrom = JsonIds.optional(o.optString("ff")),
                caption = JsonIds.optional(o.optString("caption")),
                replyTo = JsonIds.optional(o.optString("r")),
                replyPreview = o.optString("rp"),
                replyName = o.optString("rn"),
                quoteText = quote?.text.orEmpty(),
                quoteStart = quote?.start ?: -1,
                quoteEnd = quote?.end ?: -1,
                waveform = readWaveform(o.optJSONArray("wf")),
            )
        }

        private fun readWaveform(arr: JSONArray?): List<Int> {
            if (arr == null || arr.length() == 0) return emptyList()
            return buildList {
                for (i in 0 until arr.length().coerceAtMost(VoicePlayback.BARS)) {
                    add(arr.optInt(i, 0).coerceIn(0, 31))
                }
            }
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
    val replyTo: String? = null,
    val replyPreview: String = "",
    val replyName: String = "",
    val forwardedFrom: String? = null,
    val quoteText: String = "",
    val quoteStart: Int = -1,
    val quoteEnd: Int = -1,
    val linkPreview: PackedLinkPreview? = null,
) {
    fun toJson(): String = JSONObject()
        .put("g", groupId)
        .put("t", text)
        .put("e", epoch)
        .apply {
            JsonIds.optional(replyTo)?.let { put("r", it) }
            if (replyPreview.isNotBlank()) put("rp", replyPreview)
            if (replyName.isNotBlank()) put("rn", replyName)
            JsonIds.optional(forwardedFrom)?.let { put("ff", it) }
            QuoteSpanRules.put(this, quoteText, quoteStart, quoteEnd)
            LinkPreviewRules.put(this, linkPreview)
        }
        .toString()

    companion object {
        fun parse(raw: String): GroupTextPayload {
            val o = JSONObject(raw)
            val quote = QuoteSpanRules.read(o)
            return GroupTextPayload(
                groupId = o.optString("g"),
                text = o.optString("t"),
                epoch = o.optInt("e"),
                replyTo = JsonIds.optional(o.optString("r")),
                replyPreview = o.optString("rp"),
                replyName = o.optString("rn"),
                forwardedFrom = JsonIds.optional(o.optString("ff")),
                quoteText = quote?.text.orEmpty(),
                quoteStart = quote?.start ?: -1,
                quoteEnd = quote?.end ?: -1,
                linkPreview = LinkPreviewRules.read(o.optJSONObject("lp")),
            )
        }
    }
}

data class MessageMeta(
    val replyToId: String? = null,
    val replyPreview: String = "",
    val replyName: String = "",
    val forwardedFrom: String? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val quoteText: String = "",
    val quoteStart: Int = -1,
    val quoteEnd: Int = -1,
    val linkPreview: PackedLinkPreview? = null,
) {
    fun toJson(): String = JSONObject()
        .put("reply_to", replyToId ?: JSONObject.NULL)
        .put("reply_preview", replyPreview)
        .put("reply_name", replyName)
        .put("forwarded_from", forwardedFrom ?: JSONObject.NULL)
        .put("edited", edited)
        .put("deleted", deleted)
        .put("quote_text", quoteText)
        .put("quote_start", quoteStart)
        .put("quote_end", quoteEnd)
        .apply {
            LinkPreviewRules.put(this, linkPreview)
            JsonIds.optional(linkPreview?.localPath)?.let { put("lp_path", it) }
        }
        .toString()

    companion object {
        fun parse(raw: String?): MessageMeta {
            if (raw.isNullOrBlank()) return MessageMeta()
            return try {
                val o = JSONObject(raw)
                val packed = LinkPreviewRules.read(o.optJSONObject("lp"))
                MessageMeta(
                    replyToId = JsonIds.optional(o.optString("reply_to")),
                    replyPreview = o.optString("reply_preview"),
                    replyName = o.optString("reply_name"),
                    forwardedFrom = JsonIds.optional(o.optString("forwarded_from")),
                    edited = o.optBoolean("edited"),
                    deleted = o.optBoolean("deleted"),
                    quoteText = o.optString("quote_text"),
                    quoteStart = o.optInt("quote_start", -1),
                    quoteEnd = o.optInt("quote_end", -1),
                    linkPreview = packed?.copy(localPath = JsonIds.optional(o.optString("lp_path"))),
                )
            } catch (_: Exception) {
                MessageMeta()
            }
        }

        fun of(msg: ChatMessage) = MessageMeta(
            replyToId = msg.replyToId,
            replyPreview = msg.replyPreview,
            replyName = msg.replyName,
            forwardedFrom = msg.forwardedFrom,
            edited = msg.edited,
            deleted = msg.deleted,
            quoteText = msg.quoteText,
            quoteStart = msg.quoteStart,
            quoteEnd = msg.quoteEnd,
            linkPreview = msg.linkPreview,
        )
    }
}

data class ChatControl(
    val kind: String,
    val targetId: String,
    val emoji: String = "",
    val op: String = "",
    val text: String = "",
) {
    fun toJson(): String = JSONObject()
        .put("v", 1)
        .put("kind", kind)
        .put("target", targetId)
        .put("emoji", emoji)
        .put("op", op)
        .put("text", text)
        .toString()

    companion object {
        const val REACTION = "reaction"
        const val EDIT = "edit"
        const val DELETE = "delete"
        const val TYPING = "typing"
        const val PIN = "pin"

        private val kinds = setOf(REACTION, EDIT, DELETE, TYPING, PIN)

        fun parse(raw: String): ChatControl? {
            val o = try {
                JSONObject(raw)
            } catch (_: Exception) {
                return null
            }
            val kind = o.optString("kind")
            if (kind !in kinds) return null
            val target = JsonIds.optional(o.optString("target")) ?: if (kind == TYPING) "typing" else return null
            return ChatControl(
                kind = kind,
                targetId = target,
                emoji = o.optString("emoji").trim(),
                op = o.optString("op").ifBlank { ReactionPayload.SET },
                text = o.optString("text"),
            )
        }
    }
}

data class PackedText(
    val text: String,
    val replyTo: String? = null,
    val replyPreview: String = "",
    val replyName: String = "",
    val forwardedFrom: String? = null,
    val quoteText: String = "",
    val quoteStart: Int = -1,
    val quoteEnd: Int = -1,
    val linkPreview: PackedLinkPreview? = null,
)

object TextBody {
    fun encode(
        text: String,
        replyTo: String?,
        replyPreview: String,
        replyName: String,
        forwardedFrom: String? = null,
        quoteText: String = "",
        quoteStart: Int = -1,
        quoteEnd: Int = -1,
        preview: PackedLinkPreview? = null,
    ): String {
        val from = JsonIds.optional(forwardedFrom)
        val lp = preview?.takeIf { it.title.isNotBlank() && it.url.isNotBlank() }
        if (from != null) {
            return JSONObject().put("t", text).put("ff", from).apply { LinkPreviewRules.put(this, lp) }.toString()
        }
        if (replyTo.isNullOrBlank() && lp == null) return text
        return JSONObject()
            .put("t", text)
            .apply {
                if (!replyTo.isNullOrBlank()) {
                    put("r", replyTo)
                    put("rp", replyPreview)
                    put("rn", replyName)
                    QuoteSpanRules.put(this, quoteText, quoteStart, quoteEnd)
                }
                LinkPreviewRules.put(this, lp)
            }
            .toString()
    }

    fun decode(raw: String): PackedText {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return PackedText(raw)
        return try {
            val o = JSONObject(trimmed)
            if (!o.has("t")) return PackedText(raw)
            val quote = QuoteSpanRules.read(o)
            PackedText(
                text = o.optString("t"),
                replyTo = JsonIds.optional(o.optString("r")),
                replyPreview = o.optString("rp"),
                replyName = o.optString("rn"),
                forwardedFrom = JsonIds.optional(o.optString("ff")),
                quoteText = quote?.text.orEmpty(),
                quoteStart = quote?.start ?: -1,
                quoteEnd = quote?.end ?: -1,
                linkPreview = LinkPreviewRules.read(o.optJSONObject("lp")),
            )
        } catch (_: Exception) {
            PackedText(raw)
        }
    }
}

object ChatActions {
    fun canReply(msg: ChatMessage): Boolean = !msg.deleted

    fun canForward(msg: ChatMessage, protect: Boolean = false): Boolean =
        ProtectContentRules.canForward(msg, protect)

    fun canEdit(msg: ChatMessage): Boolean =
        msg.outgoing &&
            !msg.deleted &&
            (msg.kind == MessageKind.TEXT || msg.kind == MessageKind.GROUP_TEXT)

    fun canDelete(msg: ChatMessage): Boolean = msg.outgoing && !msg.deleted

    fun canCopy(msg: ChatMessage, protect: Boolean = false): Boolean =
        ProtectContentRules.canCopy(msg, protect)

    fun canPin(msg: ChatMessage): Boolean = !msg.deleted

    fun canOpen(msg: ChatMessage): Boolean =
        !msg.deleted && (msg.kind == MessageKind.IMAGE || msg.kind == MessageKind.VIDEO)
}

data class ChatPrefs(
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val unread: Int = 0,
    val lastReadMs: Long = 0,
    val draft: String = "",
    val pinnedMessageId: String? = null,
    val archived: Boolean = false,
    val protect: Boolean = false,
) {
    fun toJson(): String = JSONObject()
        .put("pinned", pinned)
        .put("muted", muted)
        .put("unread", unread)
        .put("last_read_ms", lastReadMs)
        .put("draft", draft)
        .put("pinned_message", pinnedMessageId ?: JSONObject.NULL)
        .put("archived", archived)
        .put("protect", protect)
        .toString()

    companion object {
        fun parse(raw: String?): ChatPrefs {
            if (raw.isNullOrBlank()) return ChatPrefs()
            return try {
                val o = JSONObject(raw)
                ChatPrefs(
                    pinned = o.optBoolean("pinned"),
                    muted = o.optBoolean("muted"),
                    unread = o.optInt("unread"),
                    lastReadMs = o.optLong("last_read_ms"),
                    draft = o.optString("draft"),
                    pinnedMessageId = JsonIds.optional(o.optString("pinned_message")),
                    archived = o.optBoolean("archived"),
                    protect = o.optBoolean("protect"),
                )
            } catch (_: Exception) {
                ChatPrefs()
            }
        }
    }
}

enum class ChatListMode { ALL, GROUPS, CALLS, ARCHIVE }

enum class ChatListHit(val rank: Int) {
    NONE(0),
    PREVIEW(1),
    TITLE(2),
    TITLE_PREFIX(3),
}

object QueryHighlight {
    fun firstRange(text: String, query: String): IntRange? {
        val q = ChatListRules.normalize(query)
        if (q.isEmpty() || text.isEmpty()) return null
        val word = ChatListRules.wordPrefixIndex(text, query)
        val start = if (word >= 0) word else text.indexOf(q, ignoreCase = true)
        if (start < 0 || start >= text.length) return null
        val end = (start + q.length).coerceAtMost(text.length)
        if (start >= end) return null
        return start until end
    }
}

object ChatListRules {
    private val presenceSubtitles = setOf("в сети", "не в сети")
    private val whitespace = Regex("\\s+")

    fun normalize(query: String): String = query.trim().replace(whitespace, " ").lowercase()

    fun searching(query: String): Boolean = normalize(query).isNotEmpty()

    /** First original-string index where a Unicode letter/digit word starts with the normalized query, or -1. */
    fun wordPrefixIndex(text: String, query: String): Int {
        val q = normalize(query)
        if (q.isEmpty() || text.isEmpty()) return -1
        var i = 0
        while (i < text.length) {
            while (i < text.length && !text[i].isLetterOrDigit()) i++
            if (i >= text.length) break
            val start = i
            while (i < text.length && text[i].isLetterOrDigit()) i++
            val folded = text.substring(start, i).lowercase()
            if (folded.startsWith(q)) return start
        }
        return -1
    }

    fun visible(c: Conversation, mode: ChatListMode): Boolean = when (mode) {
        ChatListMode.ALL, ChatListMode.ARCHIVE -> true
        ChatListMode.GROUPS -> c.isGroup
        ChatListMode.CALLS -> c.last?.kind == MessageKind.CALL
    }

    fun hit(c: Conversation, query: String): ChatListHit {
        val q = normalize(query)
        if (q.isEmpty()) return ChatListHit.NONE
        val title = c.title.lowercase()
        if (title.startsWith(q) || wordPrefixIndex(c.title, query) >= 0) return ChatListHit.TITLE_PREFIX
        if (title.contains(q)) return ChatListHit.TITLE
        val hay = buildString {
            c.last?.preview()?.takeIf { it.isNotBlank() }?.let { append(it).append('\n') }
            if (c.subtitle.isNotBlank() && c.subtitle.lowercase() !in presenceSubtitles) {
                append(c.subtitle)
            }
        }.lowercase()
        if (hay.contains(q)) return ChatListHit.PREVIEW
        return ChatListHit.NONE
    }

    fun matches(c: Conversation, query: String, mode: ChatListMode = ChatListMode.ALL): Boolean {
        if (!visible(c, mode)) return false
        if (!searching(query)) return true
        return hit(c, query) != ChatListHit.NONE
    }

    fun compare(a: Conversation, b: Conversation, query: String = ""): Int {
        if (searching(query)) {
            val rank = hit(b, query).rank.compareTo(hit(a, query).rank)
            if (rank != 0) return rank
            return (b.last?.timestampMs ?: 0L).compareTo(a.last?.timestampMs ?: 0L)
        }
        val aSavedPin = if (SavedMessagesRules.isSaved(a.id) && a.pinned) 1 else 0
        val bSavedPin = if (SavedMessagesRules.isSaved(b.id) && b.pinned) 1 else 0
        val savedPin = bSavedPin.compareTo(aSavedPin)
        if (savedPin != 0) return savedPin
        val pin = b.pinned.compareTo(a.pinned)
        if (pin != 0) return pin
        return (b.last?.timestampMs ?: 0L).compareTo(a.last?.timestampMs ?: 0L)
    }

    fun rows(
        conversations: List<Conversation>,
        query: String,
        mode: ChatListMode = ChatListMode.ALL,
    ): List<Conversation> = conversations.filter { matches(it, query, mode) }
        .sortedWith { a, b -> compare(a, b, query) }

    fun pinnedBlock(rows: List<Conversation>, query: String): List<Conversation> =
        if (searching(query)) emptyList() else rows.filter { it.pinned }

    fun unpinnedBlock(rows: List<Conversation>, query: String): List<Conversation> =
        if (searching(query)) rows else rows.filter { !it.pinned }

    fun showPinDivider(rows: List<Conversation>, query: String): Boolean =
        pinnedBlock(rows, query).isNotEmpty() && unpinnedBlock(rows, query).isNotEmpty()
}

enum class UnreadBadgeKind { NONE, ACCENT, MUTED }

/** Telegram-like chat-list unread badge: accent when unmuted, gray when muted. */
object UnreadBadgeRules {
    const val CAP = 99

    fun kind(unread: Int, muted: Boolean): UnreadBadgeKind = when {
        unread <= 0 -> UnreadBadgeKind.NONE
        muted -> UnreadBadgeKind.MUTED
        else -> UnreadBadgeKind.ACCENT
    }

    fun visible(unread: Int): Boolean = unread > 0

    fun label(unread: Int): String = when {
        unread <= 0 -> ""
        unread > CAP -> "$CAP+"
        else -> unread.toString()
    }

    fun emphasizeTitle(unread: Int): Boolean = unread > 0
}

object MessageSearch {
    fun matches(msg: ChatMessage, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return msg.text.lowercase().contains(q) ||
            msg.senderName.lowercase().contains(q) ||
            msg.preview().lowercase().contains(q)
    }
}

object TypingRules {
    const val TTL_MS = 3500L
    const val SEND_EVERY_MS = 2000L

    fun shouldSend(lastSentAt: Long, now: Long, draft: String): Boolean =
        draft.isNotBlank() && now - lastSentAt >= SEND_EVERY_MS

    fun isActive(untilMs: Long, now: Long): Boolean = untilMs > now
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
        val EMOJIS = listOf("❤️", "👌", "🤯", "😃", "👍", "😇", "😢")

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

    fun meta(
        status: MessageStatus,
        outgoing: Boolean,
        timestampMs: Long,
        edited: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): String {
        val time = label(timestampMs, now)
        val mark = ComposerRules.statusLabel(status, outgoing)
        val edit = if (edited) "изм." else ""
        return listOf(time, edit, mark).filter { it.isNotBlank() }.joinToString(" · ")
    }

    fun lastSeenLabel(raw: String, online: Boolean, now: Long = System.currentTimeMillis()): String {
        if (online) return "в сети"
        val ms = parseRfc3339(raw) ?: return "не в сети — дойдёт, когда появится"
        return "был(а) ${label(ms, now)}"
    }

    fun parseRfc3339(raw: String): Long? {
        if (raw.isBlank()) return null
        return try {
            java.time.Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

enum class VoiceGesture { HOLD, CANCEL, LOCK }

object ComposerRules {
    /** Hold-to-record is a single gesture. */
    const val VOICE_TAPS_TO_SEND = 1

    /** Slide up this many px while holding to lock recording. */
    const val VOICE_LOCK_SLIDE_UP = 80f

    /** Slide left this many px while holding to cancel. */
    const val VOICE_CANCEL_SLIDE_LEFT = 80f

    /** Attach → pick from gallery → caption send. */
    const val PHOTO_TAPS_TO_SEND = 3

    /** Full-screen incoming call: one tap to answer. */
    const val ANSWER_CALL_TAPS = 1

    /** Chats FAB → name → create. */
    const val GROUP_CREATE_TAPS = 2

    fun showAttach(editing: Boolean): Boolean = !editing

    fun showSendButton(
        draft: String,
        recording: Boolean,
        recordingLocked: Boolean = false,
        pendingMedia: Boolean = false,
    ): Boolean =
        (pendingMedia && !recording) || (draft.isNotBlank() && !recording) || recordingLocked

    fun showMicButton(draft: String, recording: Boolean, recordingLocked: Boolean = false): Boolean =
        (draft.isBlank() || recording) && !recordingLocked

    fun shouldLockVoice(deltaY: Float): Boolean = deltaY <= -VOICE_LOCK_SLIDE_UP

    fun shouldCancelVoice(deltaX: Float): Boolean = deltaX <= -VOICE_CANCEL_SLIDE_LEFT

    fun voiceGesture(deltaX: Float, deltaY: Float): VoiceGesture {
        val cancel = shouldCancelVoice(deltaX)
        val lock = shouldLockVoice(deltaY)
        return when {
            cancel && lock ->
                if (kotlin.math.abs(deltaX) >= kotlin.math.abs(deltaY)) VoiceGesture.CANCEL else VoiceGesture.LOCK
            cancel -> VoiceGesture.CANCEL
            lock -> VoiceGesture.LOCK
            else -> VoiceGesture.HOLD
        }
    }

    fun statusLabel(status: MessageStatus, outgoing: Boolean): String = when (status) {
        MessageStatus.CREATED -> "ожидает"
        MessageStatus.SENT_TO_SERVER -> "на сервере"
        MessageStatus.DELIVERED_TO_DEVICE -> if (outgoing) "доставлено" else ""
    }
}
