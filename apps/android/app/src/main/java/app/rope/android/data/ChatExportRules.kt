package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * User takeout of chats + local media. Not DeviceBackup. Not Downloads.
 * Inner zip is sealed by Rust UniFFI as ROBK — Kotlin does not AEAD the passphrase.
 */
data class ChatExportChat(
    val id: String,
    val title: String,
    val kind: String,
)

object ChatExportRules {
    const val SECTION = "Данные"
    const val TITLE = "Экспорт переписки"
    const val BODY =
        "Сохраните сообщения и файлы в зашифрованный файл. Ключ устройства и GitHub-токен туда не входят. Пароль только у вас."
    const val BUTTON = "Экспорт"
    const val HINT = "Пароль не уходит на сервер. Без него файл не открыть."
    const val FOOTER =
        "Восстановление из этого файла в приложении пока не поддерживается. Это копия для вас, не перенос ключа."
    const val BUSY = "Готовим архив…"
    const val DONE = "Экспорт сохранён"
    const val MIME = "application/octet-stream"
    const val EXT = "robk"
    const val MIN_PASS = 12
    const val KIND = "chat_takeout"
    const val APP = "rope"
    const val DEVICE_BACKUP_NAME = "rope-device.backup"

    val forbiddenKeys: Set<String> = setOf(
        "identity",
        "seed",
        "github_token",
        "ssh",
        "setup_token",
        "payload_key",
        "envelope",
    )

    fun hasMagic(raw: ByteArray): Boolean =
        raw.size >= 4 &&
            raw[0] == 0x52.toByte() &&
            raw[1] == 0x4F.toByte() &&
            raw[2] == 0x42.toByte() &&
            raw[3] == 0x4B.toByte()

    fun passphraseOk(pass: String, confirm: String): Boolean =
        pass.length >= MIN_PASS && pass == confirm && pass.any { !it.isWhitespace() }

    fun suggestedName(nowMs: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return "rope-chats-${fmt.format(Date(nowMs))}.$EXT"
    }

    fun chatKind(id: String): String = when {
        SavedMessagesRules.isSaved(id) -> "saved"
        ChatIds.isGroup(id) -> "group"
        else -> "dm"
    }

    fun skipMessage(msg: ChatMessage): Boolean = msg.deleted

    fun mediaAllowed(path: String?, mediaRoot: File): Boolean {
        val raw = path?.trim().orEmpty()
        if (raw.isEmpty()) return false
        val root = mediaRoot.canonicalFile
        val file = File(raw).canonicalFile
        if (!file.isFile) return false
        val prefix = root.path.let { if (it.endsWith(File.separator)) it else it + File.separator }
        return file.path.startsWith(prefix)
    }

    fun mediaRelName(msgId: String, src: File, used: MutableSet<String>): String {
        val id = msgId.filter { it.isLetterOrDigit() || it == '-' }.take(80).ifBlank { "m" }
        val ext = src.extension.lowercase().filter { it.isLetterOrDigit() }.take(8).ifBlank { "bin" }
        var name = "$id.$ext"
        var n = 2
        while (!used.add(name)) {
            name = "$id-$n.$ext"
            n++
        }
        return "media/$name"
    }

    fun jsonHasForbiddenKey(raw: String): Boolean =
        forbiddenKeys.any { key -> Regex("\"$key\"").containsMatchIn(raw) }

    fun rowJson(msg: ChatMessage, localMedia: String?): JSONObject = JSONObject()
        .put("id", msg.id)
        .put("peer_id", msg.peerDeviceId)
        .put("outgoing", msg.outgoing)
        .put("ts", msg.timestampMs)
        .put("kind", msg.kind.name)
        .put("text", msg.text)
        .put("extra", msg.extra)
        .put("sender_id", msg.senderId)
        .put("sender_name", msg.senderName)
        .put("deleted", false)
        .put("local_media", localMedia)

    fun manifest(
        nowMs: Long,
        messageCount: Int,
        mediaCount: Int,
        chats: List<ChatExportChat>,
    ): JSONObject {
        val arr = JSONArray()
        chats.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("kind", c.kind),
            )
        }
        return JSONObject()
            .put("v", 1)
            .put("kind", KIND)
            .put("exported_at_ms", nowMs)
            .put("app", APP)
            .put("message_count", messageCount)
            .put("media_count", mediaCount)
            .put("chats", arr)
    }

    fun writeInnerZip(
        zipFile: File,
        manifest: JSONObject,
        rows: List<JSONObject>,
        media: List<Pair<String, File>>,
    ) {
        zipFile.parentFile?.mkdirs()
        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("messages.jsonl"))
            val body = rows.joinToString("\n") { it.toString() }
            zip.write(body.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for ((rel, src) in media) {
                zip.putNextEntry(ZipEntry(rel))
                src.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}
