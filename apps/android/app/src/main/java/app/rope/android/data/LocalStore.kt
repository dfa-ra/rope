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

class LocalStore(context: Context) : SQLiteOpenHelper(context, "rope-local.db", null, 3) {
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
              sender_name TEXT NOT NULL DEFAULT ''
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
              members TEXT NOT NULL
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
        put("profile", o.toString())
    }

    fun profile(): ServerProfile? {
        val raw = get("profile") ?: return null
        val o = JSONObject(raw)
        return ServerProfile(
            host = o.getString("host"),
            port = o.getInt("port"),
            serverId = o.getString("serverId"),
            fingerprint = o.getString("fingerprint"),
            useTls = o.getBoolean("useTls"),
            role = o.getString("role"),
            memberId = o.getString("memberId"),
            deviceId = o.getString("deviceId"),
            displayName = o.optString("displayName"),
        )
    }

    fun insertMessage(msg: ChatMessage) {
        writableDatabase.execSQL(
            """
            INSERT OR REPLACE INTO messages(
              id, peer_id, outgoing, body_enc, status, ts, envelope,
              kind, extra, group_id, local_path, sender_id, sender_name
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(),
            arrayOf(
                msg.id,
                msg.peerDeviceId,
                if (msg.outgoing) 1 else 0,
                encrypt(msg.text),
                msg.status.name,
                msg.timestampMs,
                msg.envelope,
                msg.kind.name,
                msg.extra,
                msg.groupId,
                msg.localPath,
                msg.senderId,
                msg.senderName,
            ),
        )
    }

    fun updateStatus(id: String, status: MessageStatus) {
        writableDatabase.execSQL("UPDATE messages SET status = ? WHERE id = ?", arrayOf(status.name, id))
    }

    fun updateLocalPath(id: String, path: String) {
        writableDatabase.execSQL("UPDATE messages SET local_path = ? WHERE id = ?", arrayOf(path, id))
    }

    fun messages(peerId: String): List<ChatMessage> {
        val c = readableDatabase.rawQuery(
            """
            SELECT id, peer_id, outgoing, body_enc, status, ts, envelope,
                   kind, extra, group_id, local_path, sender_id, sender_name
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
                   kind, extra, group_id, local_path, sender_id, sender_name
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
        writableDatabase.beginTransaction()
        try {
            writableDatabase.execSQL("DELETE FROM groups")
            for (g in groups) {
                upsertGroup(g)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun upsertGroup(g: RopeGroup) {
        val members = JSONArray().apply { g.members.forEach { put(it) } }.toString()
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO groups(id, name, epoch, members) VALUES(?,?,?,?)",
            arrayOf(g.groupId, g.name, g.epoch, members),
        )
    }

    fun groups(): List<RopeGroup> {
        val c = readableDatabase.rawQuery("SELECT id, name, epoch, members FROM groups", null)
        val out = mutableListOf<RopeGroup>()
        c.use {
            while (it.moveToNext()) {
                val arr = JSONArray(it.getString(3))
                val members = buildList { for (i in 0 until arr.length()) add(arr.getString(i)) }
                out += RopeGroup(it.getString(0), it.getString(1), it.getInt(2), members)
            }
        }
        return out
    }

    fun group(id: String): RopeGroup? = groups().find { it.groupId == id }

    fun saveGithubToken(token: String) {
        if (token.isBlank()) return
        put("github_token", token)
    }

    fun githubToken(): String? = get("github_token")

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
        if (backup.githubToken.isNotBlank()) put("github_token", backup.githubToken)
        if (backup.sshJson.isNotBlank()) put("ssh_target", backup.sshJson)
    }

    fun profileJson(): String? = get("profile")

    fun sshJson(): String? = get("ssh_target")

    fun newId(): String = UUID.randomUUID().toString()

    private fun row(it: android.database.Cursor): ChatMessage {
        val kind = runCatching { MessageKind.valueOf(it.getString(7)) }.getOrDefault(MessageKind.TEXT)
        return ChatMessage(
            id = it.getString(0),
            peerDeviceId = it.getString(1),
            outgoing = it.getInt(2) == 1,
            text = decrypt(it.getBlob(3)),
            status = MessageStatus.valueOf(it.getString(4)),
            timestampMs = it.getLong(5),
            envelope = if (it.isNull(6)) null else it.getBlob(6),
            kind = kind,
            extra = it.getString(8).orEmpty(),
            groupId = if (it.isNull(9)) null else it.getString(9),
            localPath = if (it.isNull(10)) null else it.getString(10),
            senderId = it.getString(11).orEmpty(),
            senderName = it.getString(12).orEmpty(),
        )
    }

    private fun put(k: String, v: String) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO kv(k,v) VALUES(?,?)", arrayOf(k, v))
    }

    private fun get(k: String): String? {
        val c = readableDatabase.rawQuery("SELECT v FROM kv WHERE k = ?", arrayOf(k))
        c.use { return if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun encrypt(text: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, payloadKey)
        val iv = cipher.iv
        val ct = cipher.doFinal(text.toByteArray())
        return byteArrayOf(iv.size.toByte()) + iv + ct
    }

    private fun decrypt(blob: ByteArray): String {
        val ivLen = blob[0].toInt() and 0xff
        val iv = blob.copyOfRange(1, 1 + ivLen)
        val ct = blob.copyOfRange(1 + ivLen, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, payloadKey, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct))
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
        private const val PAYLOAD_ALIAS = "rope-local-payload"
    }
}
