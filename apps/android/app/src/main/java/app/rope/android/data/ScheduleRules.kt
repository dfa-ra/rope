package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

/**
 * Local scheduled-send window and staging helpers.
 *
 * Queue lives on the device. Ciphertext is produced at fire, not at tap.
 * TTL/exp is whatever ChatPrefs.ttlSec is at fire — this helper never
 * snapshots a stored expiry.
 */
object ScheduleRules {
    const val MIN_DELAY_MS = 60_000L
    const val MAX_DELAY_MS = 366L * 24 * 60 * 60 * 1000
    const val MAX_PENDING = 100
    const val STAGED_DIR = "scheduled"

    const val KIND_TEXT = "text"
    const val KIND_PHOTO = "photo"
    const val KIND_IMAGE = "image"
    const val KIND_VIDEO = "video"
    const val KIND_FILE = "file"
    const val KIND_VOICE = "voice"
    const val KIND_VIDEO_NOTE = "video_note"
    const val KIND_ALBUM = "album"

    const val TOO_MANY = "Слишком много отложенных"
    const val NEED_CONTENT = "Сначала выберите фото или напишите текст"
    const val FAILED = "Не удалось отправить отложенное"
    const val OVERDUE = "отправится при следующем запуске"
    const val LIST_TITLE = "Отложенные"
    const val PENDING_HINT = "Есть отложенные"
    const val SILENT = "Без звука"
    const val DEFER = "Отложить"

    private val MONTHS = arrayOf(
        "янв", "фев", "мар", "апр", "мая", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек",
    )

    fun inWindow(nowMs: Long, fireAtMs: Long): Boolean {
        val delay = fireAtMs - nowMs
        return delay >= MIN_DELAY_MS && delay <= MAX_DELAY_MS
    }

    fun canEnqueue(pendingCount: Int): Boolean = pendingCount < MAX_PENDING

    /** TTL at fire is always the live preference, never a stored exp snapshot. */
    @Suppress("UNUSED_PARAMETER")
    fun ttlAtFire(currentTtlSec: Int, storedExpMs: Long? = null): Int =
        currentTtlSec.coerceAtLeast(0)

    fun isDue(nowMs: Long, fireAtMs: Long): Boolean = nowMs >= fireAtMs

    fun usesNetwork(peerId: String): Boolean = !SavedMessagesRules.skipNetwork(peerId)

    fun unlinkStaged(dir: File): Boolean {
        if (!dir.exists()) return true
        dir.walkBottomUp().forEach { it.delete() }
        return !dir.exists()
    }

    fun stagedDir(filesDir: File, id: String): File = File(filesDir, "$STAGED_DIR/$id")

    fun queuedToast(fireAtMs: Long): String = "Отправится ${formatWhen(fireAtMs)}"

    fun formatWhen(ms: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = MONTHS[cal.get(Calendar.MONTH)]
        val hm = "%02d:%02d".format(
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        )
        return "$day $month, $hm"
    }

    fun listTimeLabel(nowMs: Long, fireAtMs: Long): String =
        if (isDue(nowMs, fireAtMs)) OVERDUE else formatWhen(fireAtMs)

    fun countLabel(n: Int): String = "$LIST_TITLE ($n)"

    fun preview(kind: String, text: String): String {
        val cap = text.trim()
        return when (kind) {
            KIND_VOICE -> cap.ifBlank { "Голосовое" }
            KIND_VIDEO_NOTE -> cap.ifBlank { "Видеосообщение" }
            KIND_VIDEO -> cap.ifBlank { "Видео" }
            KIND_PHOTO, KIND_IMAGE -> cap.ifBlank { "Фото" }
            KIND_ALBUM -> cap.ifBlank { "Альбом" }
            KIND_FILE -> cap.ifBlank { "Файл" }
            else -> cap.ifBlank { "Сообщение" }
        }
    }

    fun rowKind(parts: List<StagedPart>): String {
        if (parts.isEmpty()) return KIND_TEXT
        val albumish = parts.count { it.kind == KIND_IMAGE || it.kind == KIND_PHOTO || it.kind == KIND_VIDEO }
        if (albumish > 1) return KIND_ALBUM
        return when (val k = parts.first().kind) {
            KIND_IMAGE -> KIND_PHOTO
            else -> k
        }
    }

    fun hourFromNow(nowMs: Long): Long = nowMs + 60L * 60L * 1000L

    fun tomorrowNine(nowMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun writeManifest(dir: File, parts: List<StagedPart>) {
        val arr = JSONArray()
        parts.forEach { p ->
            arr.put(
                JSONObject()
                    .put("file", p.file)
                    .put("kind", p.kind)
                    .put("name", p.name)
                    .put("mime", p.mime)
                    .put("duration_ms", p.durationMs)
                    .put("waveform", JSONArray().apply { p.waveform.forEach { put(it) } }),
            )
        }
        File(dir, "manifest.json").writeText(JSONObject().put("parts", arr).toString())
    }

    fun readManifest(dir: File): List<StagedPart> {
        val raw = File(dir, "manifest.json").takeIf { it.isFile }?.readText().orEmpty()
        if (raw.isBlank()) {
            return dir.listFiles()
                ?.filter { it.isFile && it.name != "manifest.json" }
                ?.sortedBy { it.name }
                ?.map { StagedPart(it.name, KIND_FILE, it.name, "application/octet-stream") }
                .orEmpty()
        }
        return try {
            val arr = JSONObject(raw).optJSONArray("parts") ?: JSONArray()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val wf = o.optJSONArray("waveform")
                    val wave = buildList {
                        if (wf != null) {
                            for (j in 0 until wf.length()) add(wf.optInt(j))
                        }
                    }
                    add(
                        StagedPart(
                            file = o.optString("file"),
                            kind = o.optString("kind"),
                            name = o.optString("name"),
                            mime = o.optString("mime"),
                            durationMs = o.optLong("duration_ms"),
                            waveform = wave,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun packReply(
        id: String?,
        preview: String,
        name: String,
        quoteText: String = "",
        quoteStart: Int = -1,
        quoteEnd: Int = -1,
    ): String {
        if (id.isNullOrBlank()) return ""
        return JSONObject()
            .put("id", id)
            .put("preview", preview)
            .put("name", name)
            .put("qt", quoteText)
            .put("qs", quoteStart)
            .put("qe", quoteEnd)
            .toString()
    }

    fun unpackReply(raw: String): Triple<String?, String, String> {
        if (raw.isBlank()) return Triple(null, "", "")
        return try {
            val o = JSONObject(raw)
            Triple(
                JsonIds.optional(o.optString("id")),
                o.optString("preview"),
                o.optString("name"),
            )
        } catch (_: Exception) {
            Triple(null, "", "")
        }
    }

    fun unpackQuote(raw: String): Triple<String, Int, Int> {
        if (raw.isBlank()) return Triple("", -1, -1)
        return try {
            val o = JSONObject(raw)
            Triple(o.optString("qt"), o.optInt("qs", -1), o.optInt("qe", -1))
        } catch (_: Exception) {
            Triple("", -1, -1)
        }
    }
}

data class StagedPart(
    val file: String,
    val kind: String,
    val name: String,
    val mime: String,
    val durationMs: Long = 0,
    val waveform: List<Int> = emptyList(),
)

data class ScheduledSend(
    val id: String,
    val peerId: String,
    val fireAtMs: Long,
    val silent: Boolean,
    val kind: String,
    val text: String = "",
    val replyJson: String = "",
    val mediaDir: String = "",
)

/** Inner-JSON silent flag. Never a WSS / envelope header. */
object SilentFlag {
    const val KEY = "ns"

    fun put(o: JSONObject, silent: Boolean) {
        if (silent) o.put(KEY, 1)
    }

    fun read(o: JSONObject): Boolean = o.optInt(KEY) == 1
}
