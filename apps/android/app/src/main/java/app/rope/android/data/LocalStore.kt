package app.rope.android.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore

class LocalStore(context: Context) : SQLiteOpenHelper(context, "rope-local.db", null, 2) {
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
              envelope BLOB
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE messages ADD COLUMN envelope BLOB")
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
            "INSERT OR REPLACE INTO messages(id, peer_id, outgoing, body_enc, status, ts, envelope) VALUES(?,?,?,?,?,?,?)",
            arrayOf(
                msg.id,
                msg.peerDeviceId,
                if (msg.outgoing) 1 else 0,
                encrypt(msg.text),
                msg.status.name,
                msg.timestampMs,
                msg.envelope,
            ),
        )
    }

    fun updateStatus(id: String, status: MessageStatus) {
        writableDatabase.execSQL("UPDATE messages SET status = ? WHERE id = ?", arrayOf(status.name, id))
    }

    fun messages(peerId: String): List<ChatMessage> {
        val c = readableDatabase.rawQuery(
            "SELECT id, peer_id, outgoing, body_enc, status, ts, envelope FROM messages WHERE peer_id = ? ORDER BY ts ASC",
            arrayOf(peerId),
        )
        val out = mutableListOf<ChatMessage>()
        c.use {
            while (it.moveToNext()) {
                out += ChatMessage(
                    id = it.getString(0),
                    peerDeviceId = it.getString(1),
                    outgoing = it.getInt(2) == 1,
                    text = decrypt(it.getBlob(3)),
                    status = MessageStatus.valueOf(it.getString(4)),
                    timestampMs = it.getLong(5),
                    envelope = if (it.isNull(6)) null else it.getBlob(6),
                )
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
            "SELECT id, peer_id, outgoing, body_enc, status, ts, envelope FROM messages WHERE outgoing = 1 AND status = ?",
            arrayOf(MessageStatus.CREATED.name),
        )
        val out = mutableListOf<ChatMessage>()
        c.use {
            while (it.moveToNext()) {
                out += ChatMessage(
                    id = it.getString(0),
                    peerDeviceId = it.getString(1),
                    outgoing = true,
                    text = decrypt(it.getBlob(3)),
                    status = MessageStatus.valueOf(it.getString(4)),
                    timestampMs = it.getLong(5),
                    envelope = if (it.isNull(6)) null else it.getBlob(6),
                )
            }
        }
        return out
    }

    fun saveGithubToken(token: String) {
        if (token.isBlank()) return
        put("github_token", token)
    }

    fun githubToken(): String? = get("github_token")

    fun newId(): String = UUID.randomUUID().toString()

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
