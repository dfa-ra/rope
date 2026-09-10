package app.rope.android.net

import app.rope.android.data.DirectoryDevice
import app.rope.android.data.RopeGroup
import app.rope.android.data.ServerProfile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import uniffi.rope_core.DeviceIdentity
import java.util.concurrent.TimeUnit

class ServerApi(
    private val profile: ServerProfile,
    private val identity: DeviceIdentity,
) {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val octet = "application/octet-stream".toMediaType()
    private val client: OkHttpClient = if (profile.useTls) {
        PinnedClient.pinned(profile.fingerprint)
    } else {
        PinnedClient.http()
    }

    val baseHttp: String
        get() = (if (profile.useTls) "https" else "http") + "://${profile.host}:${profile.port}"

    val baseWs: String
        get() = (if (profile.useTls) "wss" else "ws") + "://${profile.host}:${profile.port}"

    fun info(): JSONObject = authed("GET", "/v1/info", ByteArray(0))

    fun directory(): List<DirectoryDevice> {
        val obj = authed("GET", "/v1/directory", ByteArray(0))
        val members = mutableMapOf<String, String>()
        val roles = mutableMapOf<String, String>()
        val mems = obj.optJSONArray("members") ?: JSONArray()
        for (i in 0 until mems.length()) {
            val m = mems.getJSONObject(i)
            val mid = m.getString("member_id")
            members[mid] = m.optString("display_name")
            roles[mid] = m.optString("role").ifBlank { "member" }
        }
        val devices = obj.getJSONArray("devices")
        val out = mutableListOf<DirectoryDevice>()
        for (i in 0 until devices.length()) {
            val d = devices.getJSONObject(i)
            val blob = android.util.Base64.decode(d.getString("public_identity"), android.util.Base64.DEFAULT)
            val mid = d.getString("member_id")
            out += DirectoryDevice(
                deviceId = d.getString("device_id"),
                memberId = mid,
                displayName = members[mid].orEmpty().ifBlank { d.getString("device_id").take(8) },
                publicIdentity = blob,
                lastSeen = d.optString("last_seen"),
                online = d.optBoolean("online"),
                role = roles[mid].orEmpty().ifBlank { "member" },
            )
        }
        return out
    }

    fun createInvite(ttlSeconds: Int = 3600): Pair<String, String> {
        val body = JSONObject().put("ttl_seconds", ttlSeconds).toString().toByteArray()
        val obj = authed("POST", "/v1/invites", body)
        return obj.getString("token") to obj.getString("expires_at")
    }

    fun status(): JSONObject = authed("GET", "/v1/admin/status", ByteArray(0))

    fun revokeMember(memberId: String): JSONObject {
        val body = JSONObject().put("member_id", memberId).toString().toByteArray()
        // 404 = unknown or already revoked (idempotent). Kotlin never implements crypto.
        return authed("POST", "/v1/admin/revoke-member", body, allowNotFound = true)
    }

    fun uploadObject(ciphertext: ByteArray, sha256: String): JSONObject {
        val path = "/v1/objects"
        val header = sendAuthHeader("POST", path, ciphertext)
        val req = Request.Builder()
            .url(baseHttp + path)
            .header("Authorization", header)
            .header("X-Rope-SHA256", sha256)
            .post(ciphertext.toRequestBody(octet))
            .build()
        client.newBuilder()
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
            .newCall(req)
            .execute()
            .use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("upload ${resp.code}: $text")
                return JSONObject(text)
            }
    }

    fun downloadObject(objectId: String): Pair<ByteArray, String> {
        val path = "/v1/objects/$objectId"
        val header = sendAuthHeader("GET", path, ByteArray(0))
        val req = Request.Builder()
            .url(baseHttp + path)
            .header("Authorization", header)
            .get()
            .build()
        client.newBuilder()
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
            .newCall(req)
            .execute()
            .use { resp ->
                val bytes = resp.body?.bytes() ?: ByteArray(0)
                if (!resp.isSuccessful) error("download ${resp.code}")
                return bytes to resp.header("X-Rope-SHA256").orEmpty()
            }
    }

    fun createGroup(name: String): RopeGroup {
        val body = JSONObject().put("name", name).toString().toByteArray()
        return parseGroup(authed("POST", "/v1/groups", body))
    }

    fun renameGroup(groupId: String, name: String): RopeGroup {
        val body = JSONObject().put("name", name).toString().toByteArray()
        return parseGroup(authed("PATCH", "/v1/groups/$groupId", body))
    }

    fun listGroups(): List<RopeGroup> {
        val obj = authed("GET", "/v1/groups", ByteArray(0))
        val arr = obj.optJSONArray("groups") ?: JSONArray()
        val out = mutableListOf<RopeGroup>()
        for (i in 0 until arr.length()) {
            out += parseGroup(arr.getJSONObject(i))
        }
        return out
    }

    fun addGroupMember(groupId: String, deviceId: String): RopeGroup {
        val body = JSONObject().put("device_id", deviceId).toString().toByteArray()
        return parseGroup(authed("POST", "/v1/groups/$groupId/members", body))
    }

    fun removeGroupMember(groupId: String, deviceId: String): RopeGroup {
        val body = JSONObject().put("device_id", deviceId).toString().toByteArray()
        return parseGroup(authed("POST", "/v1/groups/$groupId/remove", body))
    }

    fun bootstrap(host: String, port: Int, useTls: Boolean, fingerprint: String, token: String, displayName: String): JSONObject {
        val body = JSONObject()
            .put("token", token)
            .put("display_name", displayName)
            .put("public_identity", android.util.Base64.encodeToString(identity.publicIdentity().blob, android.util.Base64.NO_WRAP))
            .put("device_id", identity.deviceId())
            .toString()
        val c = if (useTls) PinnedClient.pinned(fingerprint) else PinnedClient.http()
        val scheme = if (useTls) "https" else "http"
        val req = Request.Builder()
            .url("$scheme://$host:$port/v1/bootstrap")
            .post(body.toRequestBody(json))
            .build()
        c.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("bootstrap ${resp.code}: $text")
            return JSONObject(text)
        }
    }

    fun openSocket(listener: WebSocketListener): WebSocket {
        val ts = uniffi.rope_core.unixTimestamp()
        val sig = identity.wsSignature(ts)
        val id = identity.deviceId()
        val tsStr = ts.toString()
        val url = "$baseWs/v1/ws?device_id=$id&ts=$tsStr&sig=$sig"
        val wsClient = client.newBuilder().pingInterval(20, TimeUnit.SECONDS).build()
        val req = Request.Builder()
            .url(url)
            .header(WsAuth.HEADER, WsAuth.value(id, tsStr, sig))
            .build()
        return wsClient.newWebSocket(req, listener)
    }

    fun sendAuthHeader(method: String, path: String, body: ByteArray): String =
        identity.authHeader(method, path, uniffi.rope_core.unixTimestamp(), body)

    private fun parseGroup(obj: JSONObject): RopeGroup {
        val members = obj.optJSONArray("members") ?: JSONArray()
        val ids = buildList { for (i in 0 until members.length()) add(members.getString(i)) }
        return RopeGroup(
            groupId = obj.getString("group_id"),
            name = obj.getString("name"),
            epoch = obj.optInt("epoch"),
            members = ids,
            createdBy = obj.optString("created_by").ifBlank { obj.optString("createdBy") },
        )
    }

    private fun authed(
        method: String,
        path: String,
        body: ByteArray,
        allowNotFound: Boolean = false,
    ): JSONObject {
        val header = sendAuthHeader(method, path, body)
        val builder = Request.Builder()
            .url(baseHttp + path)
            .header("Authorization", header)
        if (method == "GET") builder.get() else builder.method(method, body.toRequestBody(json))
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (allowNotFound && resp.code == 404) {
                return JSONObject().put("ok", true).put("already_revoked", true)
            }
            if (!resp.isSuccessful) error("${resp.code}: $text")
            return JSONObject(text)
        }
    }

    companion object {
        fun fetchInfo(host: String, port: Int, useTls: Boolean, fingerprint: String): JSONObject {
            val c = if (useTls) PinnedClient.pinned(fingerprint) else PinnedClient.http()
            val scheme = if (useTls) "https" else "http"
            val req = Request.Builder().url("$scheme://$host:$port/v1/info").get().build()
            c.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("info ${resp.code}: $text")
                return JSONObject(text)
            }
        }
    }
}
