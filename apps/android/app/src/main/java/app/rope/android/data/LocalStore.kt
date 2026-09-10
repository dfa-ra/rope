package app.rope.android.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.rope.android.update.DeviceBackup
import app.rope.android.data.ThemeMode
import app.rope.android.data.JsonIds
import app.rope.android.data.ChatIds
import app.rope.android.data.MediaPayload
import java.security.KeyStore

class LocalStore(context: Context) : SQLiteOpenHelper(context, "rope-local.db", null, VERSION) {
    private val payloadKey: SecretKey by lazy { payloadKey() }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
              id TEXT PRIMARY KEY,
              peer_id TEXT NOT NULL,
              outgoing INTEGER NOT NULL,
              body_enc BLOB NOT NULL,
              status TEXT NOT NULL,
              ts INTEGER NOT NULL,
              envelope BLOB,
              kind TEXT NOT NULL DEFAULT 'TEXT',
              extra TEXT NOT NULL DEFAULT '',
              group_id TEXT,
              local_path TEXT,
              sender_id TEXT NOT NULL DEFAULT '',
              sender_name TEXT NOT NULL DEFAULT '',
              reactions TEXT NOT NULL DEFAULT '[]',
              meta TEXT NOT NULL DEFAULT '{}'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE kv (
              k TEXT PRIMARY KEY,
              v TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE groups (
              id TEXT PRIMARY KEY,
              name TEXT NOT NULL,
              epoch INTEGER NOT NULL,
              members TEXT NOT NULL,
              created_by TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE messages ADD COLUMN envelope BLOB")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE messages ADD COLUMN kind TEXT NOT NULL DEFAULT 'TEXT'")
            db.execSQL("ALTER TABLE messages ADD COLUMN extra TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE messages ADD COLUMN group_id TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN local_path TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN sender_id TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE messages ADD COLUMN sender_name TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS groups (
                  id TEXT PRIMARY KEY,
                  name TEXT NOT NULL,
                  epoch INTEGER NOT NULL,
                  members TEXT NOT NULL
                )
                """.trimIndent(),
            )
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE messages ADD COLUMN reactions TEXT NOT NULL DEFAULT '[]'")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE messages ADD COLUMN meta TEXT NOT NULL DEFAULT '{}'")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE groups ADD COLUMN created_by TEXT NOT NULL DEFAULT ''")
        }
    }

    fun saveProfile(p: ServerProfile) {
        val o = JSONObject()
            .put("host", p.host)
            .put("port", p.port)
            .put("serverId", p.serverId)
            .put("fingerprint", p.fingerprint)
            .put("useTls", p.useTls)
            .put("role", p.role)
            .put("memberId", p.memberId)
            .put("deviceId", p.deviceId)
            .put("displayName", p.displayName)
            .put("iceServersJson", IceAtRest.seal(p.iceServersJson) { encryptBytes(it) })
        put("profile", o.toString())
    }

    fun profile(): ServerProfile? {
        val raw = get("profile") ?: return null
        val o = JSONObject(raw)
        val storedIce = o.optString("iceServersJson")
        val ice = IceAtRest.open(storedIce) { decryptBytes(it) }
        val p = ServerProfile(
            host = o.getString("host"),
            port = o.getInt("port"),
            serverId = o.getString("serverId"),
            fingerprint = o.getString("fingerprint"),
            useTls = o.getBoolean("useTls"),
            role = o.getString("role"),
            memberId = o.getString("memberId"),
            deviceId = o.getString("deviceId"),
            displayName = o.optString("displayName"),
            iceServersJson = ice,
        )
        if (ice.isNotBlank() && storedIce.isNotBlank() && !SecretKv.isWrapped(storedIce)) {
            saveProfile(p)
        }
        return p
    }

    fun insertMessage(msg: ChatMessage) {
        val existing = message(msg.id)
        if (existing != null && !ChatControlRules.shouldReplace(existing.senderId, msg.senderId)) {
            return
        }
        val merged = if (existing == null) {
            msg
        } else {
            msg.copy(
                reactions = msg.reactions.ifEmpty { existing.reactions },
                localPath = msg.localPath ?: existing.localPath,
                replyToId = msg.replyToId ?: existing.replyToId,
                replyPreview = msg.replyPreview.ifBlank { existing.replyPreview },
                replyName = msg.replyName.ifBlank { existing.replyName },
                quoteText = msg.quoteText.ifBlank { existing.quoteText },
                quoteStart = if (msg.quoteStart >= 0) msg.quoteStart else existing.quoteStart,
                quoteEnd = if (msg.quoteEnd >= 0) msg.quoteEnd else existing.quoteEnd,
                forwardedFrom = msg.forwardedFrom ?: existing.forwardedFrom,
                edited = msg.edited || existing.edited,
                deleted = msg.deleted || existing.deleted,
                linkPreview = mergePreview(msg.linkPreview, existing.linkPreview),
            )
        }
        writableDatabase.execSQL(
            """
            INSERT OR REPLACE INTO messages(
              id, peer_id, outgoing, body_enc, status, ts, envelope,
              kind, extra, group_id, local_path, sender_id, sender_name, reactions, meta
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(),
            arrayOf(
                merged.id,
                merged.peerDeviceId,
                if (merged.outgoing) 1 else 0,
                encrypt(merged.text),
                merged.status.name,
                merged.timestampMs,
                merged.envelope,
                merged.kind.name,
                merged.extra,
                merged.groupId,
                merged.localPath,
                merged.senderId,
                merged.senderName,
                ReactionCodec.toJson(merged.reactions),
                MessageMeta.of(merged).toJson(),
            ),
        )
    }

    fun message(id: String): ChatMessage? {
        val c = readableDatabase.rawQuery(
            """
            SELECT id, peer_id, outgoing, body_enc, status, ts, envelope,
                   kind, extra, group_id, local_path, sender_id, sender_name, reactions, meta
            FROM messages WHERE id = ?
            """.trimIndent(),
            arrayOf(id),
        )
        c.use { return if (it.moveToFirst()) row(it) else null }
    }

    fun applyReaction(targetId: String, emoji: String, deviceId: String, displayName: String, clear: Boolean): Boolean {
        val msg = message(targetId) ?: return false
        val next = if (clear) {
            msg.reactions.filterNot { it.deviceId == deviceId && it.emoji == emoji }
        } else {
            msg.reactions.filterNot { it.deviceId == deviceId && it.emoji == emoji } +
                Reaction(emoji, deviceId, displayName)
        }
        writableDatabase.execSQL(
            "UPDATE messages SET reactions = ? WHERE id = ?",
            arrayOf(ReactionCodec.toJson(next), targetId),
        )
        return true
    }

    fun updateStatus(id: String, status: MessageStatus) {
        writableDatabase.execSQL("UPDATE messages SET status = ? WHERE id = ?", arrayOf(status.name, id))
    }

    fun updateLocalPath(id: String, path: String) {
        writableDatabase.execSQL("UPDATE messages SET local_path = ? WHERE id = ?", arrayOf(path, id))
    }

    fun editMessage(id: String, text: String): Boolean {
        val msg = message(id) ?: return false
        val oldUrl = LinkPreviewRules.firstHttps(msg.text)
        val newUrl = LinkPreviewRules.firstHttps(text)
        val keep = !oldUrl.isNullOrBlank() && oldUrl == newUrl
        val nextPreview = if (keep) msg.linkPreview else null
        writableDatabase.execSQL(
            "UPDATE messages SET body_enc = ?, meta = ? WHERE id = ?",
            arrayOf(encrypt(text), MessageMeta.of(msg.copy(text = text, edited = true, linkPreview = nextPreview)).toJson(), id),
        )
        return true
    }

    fun updateLinkThumb(id: String, path: String) {
        val msg = message(id) ?: return
        val lp = msg.linkPreview?.copy(localPath = path) ?: return
        writableDatabase.execSQL(
            "UPDATE messages SET meta = ? WHERE id = ?",
            arrayOf(MessageMeta.of(msg.copy(linkPreview = lp)).toJson(), id),
        )
    }

    fun markDeleted(id: String): Boolean {
        val msg = message(id) ?: return false
        writableDatabase.execSQL(
            "UPDATE messages SET body_enc = ?, extra = '', local_path = NULL, meta = ? WHERE id = ?",
            arrayOf(
                encrypt(""),
                MessageMeta.of(msg.copy(deleted = true, text = "", extra = "")).toJson(),
                id,
            ),
        )
        return true
    }

    fun messages(peerId: String): List<ChatMessage> {
        val c = readableDatabase.rawQuery(
            """
            SELECT id, peer_id, outgoing, body_enc, status, ts, envelope,
                   kind, extra, group_id, local_path, sender_id, sender_name, reactions, meta
            FROM messages WHERE peer_id = ? ORDER BY ts ASC
            """.trimIndent(),
            arrayOf(peerId),
        )
        val out = mutableListOf<ChatMessage>()
        c.use {
            while (it.moveToNext()) {
                out += row(it)
            }
        }
        return out
    }

    fun conversations(): List<Pair<String, ChatMessage?>> {
        val c = readableDatabase.rawQuery("SELECT DISTINCT peer_id FROM messages", null)
        val ids = mutableListOf<String>()
        c.use { while (it.moveToNext()) ids += it.getString(0) }
        return ids.map { id -> id to messages(id).lastOrNull() }
    }

    fun pendingOutgoing(): List<ChatMessage> {
        val c = readableDatabase.rawQuery(
            """
            SELECT id, peer_id, outgoing, body_enc, status, ts, envelope,
                   kind, extra, group_id, local_path, sender_id, sender_name, reactions, meta
            FROM messages WHERE outgoing = 1 AND status = ?
            """.trimIndent(),
            arrayOf(MessageStatus.CREATED.name),
        )
        val out = mutableListOf<ChatMessage>()
        c.use {
            while (it.moveToNext()) {
                out += row(it)
            }
        }
        return out
    }

    fun saveGroups(groups: List<RopeGroup>) {
        val prev = groups().associate { it.groupId to it.createdBy }
        writableDatabase.beginTransaction()
        try {
            writableDatabase.execSQL("DELETE FROM groups")
            for (g in groups) {
                upsertGroup(g.copy(createdBy = g.createdBy.ifBlank { prev[g.groupId].orEmpty() }))
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun upsertGroup(g: RopeGroup) {
        val members = JSONArray().apply { g.members.forEach { put(it) } }.toString()
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO groups(id, name, epoch, members, created_by) VALUES(?,?,?,?,?)",
            arrayOf(g.groupId, g.name, g.epoch, members, g.createdBy),
        )
    }

    fun deleteGroup(id: String) {
        writableDatabase.execSQL("DELETE FROM groups WHERE id = ?", arrayOf(id))
    }

    fun groups(): List<RopeGroup> {
        val c = readableDatabase.rawQuery("SELECT id, name, epoch, members, created_by FROM groups", null)
        val out = mutableListOf<RopeGroup>()
        c.use {
            while (it.moveToNext()) {
                val arr = JSONArray(it.getString(3))
                val members = buildList { for (i in 0 until arr.length()) add(arr.getString(i)) }
                val createdBy = if (it.columnCount > 4 && !it.isNull(4)) it.getString(4).orEmpty() else ""
                out += RopeGroup(it.getString(0), it.getString(1), it.getInt(2), members, createdBy)
            }
        }
        return out
    }

    fun group(id: String): RopeGroup? = groups().find { it.groupId == id }

    fun saveGithubToken(token: String) {
        if (token.isBlank()) return
        put("github_token", SecretKv.wrap(token) { encryptBytes(it) })
    }

    fun githubToken(): String? {
        val stored = get("github_token") ?: return null
        val plain = SecretKv.unwrap(stored) { decryptBytes(it) } ?: return null
        if (plain.isNotBlank() && !SecretKv.isWrapped(stored)) {
            put("github_token", SecretKv.wrap(plain) { encryptBytes(it) })
        }
        return plain.takeIf { it.isNotBlank() }
    }

    fun saveSshTarget(t: SshTarget) {
        put(
            "ssh_target",
            JSONObject()
                .put("host", t.host)
                .put("sshPort", t.sshPort)
                .put("user", t.user)
                .put("listenPort", t.listenPort)
                .toString(),
        )
    }

    fun sshTarget(): SshTarget? {
        val raw = get("ssh_target") ?: return null
        val o = JSONObject(raw)
        return SshTarget(
            host = o.getString("host"),
            sshPort = o.optInt("sshPort", 22),
            user = o.optString("user", "root"),
            listenPort = o.optInt("listenPort", 8443),
        )
    }

    fun saveTheme(mode: ThemeMode) {
        put("theme", mode.name)
    }

    fun saveNotificationsMuted(muted: Boolean) {
        put("notifications_muted", if (muted) "1" else "0")
    }

    fun notificationsMuted(): Boolean = get("notifications_muted") == "1"

    fun saveLinkPreviews(enabled: Boolean) {
        put("link_previews", if (enabled) "1" else "0")
    }

    fun linkPreviewsEnabled(): Boolean = get("link_previews") != "0"

    fun saveCompactChats(on: Boolean) {
        put(CompactChatRules.KEY, if (on) "1" else "0")
    }

    fun compactChats(): Boolean = CompactChatRules.enabledFromKv(get(CompactChatRules.KEY))

    fun allChatPrefs(): Map<String, ChatPrefs> {
        val raw = get("chat_prefs") ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    val item = o.opt(key)
                    val json = when (item) {
                        is JSONObject -> item.toString()
                        else -> item?.toString()
                    }
                    put(key, ChatPrefs.parse(json))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun chatPrefs(id: String): ChatPrefs = allChatPrefs()[id] ?: ChatPrefs()

    fun saveChatPrefs(id: String, prefs: ChatPrefs) {
        val all = allChatPrefs().toMutableMap()
        all[id] = prefs
        val o = JSONObject()
        all.forEach { (key, value) -> o.put(key, JSONObject(value.toJson())) }
        put("chat_prefs", o.toString())
    }

    fun themeMode(defaultDark: Boolean): ThemeMode {
        return when (get("theme")?.lowercase()) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> if (defaultDark) ThemeMode.DARK else ThemeMode.LIGHT
        }
    }

    fun rehomeMisroutedMedia() {
        val c = readableDatabase.rawQuery(
            """
            SELECT id, peer_id, outgoing, kind, extra, group_id, sender_id
            FROM messages
            """.trimIndent(),
            null,
        )
        val fixes = mutableListOf<Pair<String, String>>()
        c.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val peerId = it.getString(1)
                val outgoing = it.getInt(2) == 1
                val extra = it.getString(4).orEmpty()
                val storedGroup = JsonIds.optional(if (it.isNull(5)) null else it.getString(5))
                val senderId = it.getString(6).orEmpty()
                val extraGroup = runCatching { JsonIds.optional(MediaPayload.parse(extra).groupId) }.getOrNull()
                val claimed = storedGroup ?: extraGroup ?: if (ChatIds.isGroup(peerId)) JsonIds.optional(ChatIds.rawGroupId(peerId)) else null
                val misrouted = ChatIds.isGroup(peerId) && claimed == null
                if (!misrouted || outgoing || senderId.isBlank()) continue
                fixes += id to senderId
            }
        }
        for ((id, senderId) in fixes) {
            writableDatabase.execSQL(
                "UPDATE messages SET peer_id = ?, group_id = NULL WHERE id = ?",
                arrayOf(senderId, id),
            )
        }
    }

    fun applyBackup(backup: DeviceBackup) {
        if (backup.profileJson.isNotBlank()) put("profile", backup.profileJson)
        if (backup.githubToken.isNotBlank()) saveGithubToken(backup.githubToken)
        if (backup.sshJson.isNotBlank()) put("ssh_target", backup.sshJson)
    }

    fun profileJson(): String? = get("profile")

    fun sshJson(): String? = get("ssh_target")

    fun newId(): String = UUID.randomUUID().toString()

    private fun mergePreview(incoming: PackedLinkPreview?, existing: PackedLinkPreview?): PackedLinkPreview? {
        val base = incoming ?: existing ?: return null
        return base.copy(localPath = incoming?.localPath ?: existing?.localPath)
    }

    private fun row(c: android.database.Cursor): ChatMessage {
        val kind = runCatching { MessageKind.valueOf(c.getString(7)) }.getOrDefault(MessageKind.TEXT)
        val meta = if (c.columnCount > 14 && !c.isNull(14)) MessageMeta.parse(c.getString(14)) else MessageMeta()
        val text = if (meta.deleted) "" else decrypt(c.getBlob(3))
        return ChatMessage(
            id = c.getString(0),
            peerDeviceId = c.getString(1),
            outgoing = c.getInt(2) == 1,
            text = text,
            status = MessageStatus.valueOf(c.getString(4)),
            timestampMs = c.getLong(5),
            envelope = if (c.isNull(6)) null else c.getBlob(6),
            kind = kind,
            extra = c.getString(8).orEmpty(),
            groupId = if (c.isNull(9)) null else c.getString(9),
            localPath = if (c.isNull(10)) null else c.getString(10),
            senderId = c.getString(11).orEmpty(),
            senderName = c.getString(12).orEmpty(),
            reactions = if (c.columnCount > 13 && !c.isNull(13)) ReactionCodec.parse(c.getString(13)) else emptyList(),
            replyToId = meta.replyToId,
            replyPreview = meta.replyPreview,
            replyName = meta.replyName,
            quoteText = meta.quoteText,
            quoteStart = meta.quoteStart,
            quoteEnd = meta.quoteEnd,
            forwardedFrom = meta.forwardedFrom,
            edited = meta.edited,
            deleted = meta.deleted,
            linkPreview = meta.linkPreview,
        )
    }

    private fun put(k: String, v: String) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO kv(k,v) VALUES(?,?)", arrayOf(k, v))
    }

    private fun get(k: String): String? {
        val c = readableDatabase.rawQuery("SELECT v FROM kv WHERE k = ?", arrayOf(k))
        c.use { return if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun encrypt(text: String): ByteArray = encryptBytes(text.toByteArray())

    private fun decrypt(blob: ByteArray): String = String(decryptBytes(blob))

    private fun encryptBytes(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, payloadKey)
        return pack(cipher.iv, cipher.doFinal(plain))
    }

    private fun decryptBytes(blob: ByteArray): ByteArray {
        val (iv, ct) = unpack(blob)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, payloadKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    private fun payloadKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(PAYLOAD_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(PAYLOAD_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    companion object {
        const val VERSION = 6
        private const val PAYLOAD_ALIAS = "rope-local-payload"

        /** AES-GCM IV length used by Android Keystore. */
        const val IV_LEN = 12

        /** GCM tag is 16 bytes; ciphertext cannot be shorter. */
        const val TAG_LEN = 16

        /** At-rest layout: 1-byte IV length, 12-byte IV, ciphertext+tag. */
        fun pack(iv: ByteArray, ct: ByteArray): ByteArray {
            require(iv.size == IV_LEN)
            require(ct.size >= TAG_LEN)
            return byteArrayOf(IV_LEN.toByte()) + iv + ct
        }

        fun unpack(blob: ByteArray): Pair<ByteArray, ByteArray> {
            if (blob.isEmpty()) error("empty wrap")
            val ivLen = blob[0].toInt() and 0xff
            if (ivLen != IV_LEN || blob.size < 1 + IV_LEN + TAG_LEN) error("bad wrap")
            val iv = blob.copyOfRange(1, 1 + ivLen)
            val ct = blob.copyOfRange(1 + ivLen, blob.size)
            return iv to ct
        }
    }
}
