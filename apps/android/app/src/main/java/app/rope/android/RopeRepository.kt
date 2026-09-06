package app.rope.android

import android.app.Application
import android.util.Base64
import app.rope.android.data.ChatMessage
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.IdentityVault
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import app.rope.android.data.ServerProfile
import app.rope.android.net.ServerApi
import app.rope.android.protocol.InviteCodec
import app.rope.android.protocol.InviteLink as ParsedInvite
import app.rope.android.provision.SshProvisioner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import uniffi.rope_core.DeviceIdentity
import uniffi.rope_core.buildInviteUrl
import uniffi.rope_core.parseInviteUrl
import uniffi.rope_core.publicIdentityFromBlob
import uniffi.rope_core.verifyInvite

data class UiState(
    val screen: Screen = Screen.Start,
    val profile: ServerProfile? = null,
    val devices: List<DirectoryDevice> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val peer: DirectoryDevice? = null,
    val inviteUrl: String? = null,
    val statusText: String = "",
    val error: String? = null,
    val busy: Boolean = false,
    val offline: Boolean = false,
    val draftHost: String = "",
    val draftText: String = "",
)

enum class Screen { Start, Provision, Join, Chats, Chat, Invite, Status, Settings }

class RopeRepository(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = LocalStore(app)
    private val vault = IdentityVault(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var identity: DeviceIdentity? = null
    private var api: ServerApi? = null
    private var socket: WebSocket? = null

    fun start(pendingLink: String?) {
        scope.launch {
            try {
                identity = if (vault.exists()) DeviceIdentity.fromBytes(vault.load()) else DeviceIdentity.generate().also {
                    vault.save(it.toBytes())
                }
                store.profile()?.let { attached(it) }
                if (!pendingLink.isNullOrBlank()) {
                    join(pendingLink, "guest")
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun go(screen: Screen) {
        _state.value = _state.value.copy(screen = screen, error = null)
    }

    fun setDraft(text: String) {
        _state.value = _state.value.copy(draftText = text)
    }

    fun provision(
        host: String,
        sshPort: Int,
        user: String,
        password: String,
        keyPem: String,
        listenPort: Int,
        binaryUrl: String,
        displayName: String,
        upgrade: Boolean,
    ) {
        scope.launch {
            busy(true)
            try {
                val result = SshProvisioner(app).install(
                    sshHost = host,
                    sshPort = sshPort,
                    sshUser = user,
                    sshPassword = password.ifBlank { null },
                    sshKeyPem = keyPem.ifBlank { null },
                    listenPort = listenPort,
                    binaryUrl = binaryUrl,
                    upgrade = upgrade,
                )
                if (upgrade) {
                    _state.value = _state.value.copy(statusText = "server core updated", busy = false, screen = Screen.Status)
                    return@launch
                }
                val id = identity ?: error("identity missing")
                val boot = ServerApi(dummyProfile(result.host, result.port, result.fingerprint, true), id)
                    .bootstrap(result.host, result.port, true, result.fingerprint, result.setupToken, displayName)
                val profile = ServerProfile(
                    host = result.host,
                    port = result.port,
                    serverId = result.serverId,
                    fingerprint = result.fingerprint,
                    useTls = true,
                    role = boot.getString("role"),
                    memberId = boot.getString("member_id"),
                    deviceId = boot.getString("device_id"),
                )
                attached(profile)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun join(url: String, displayName: String) {
        scope.launch {
            busy(true)
            try {
                val parsed = try {
                    val rust = parseInviteUrl(url)
                    ParsedInvite(
                        rust.version.toInt(), rust.host, rust.port.toInt(), rust.serverId, rust.fingerprint, rust.token, rust.displayName,
                    )
                } catch (_: Exception) {
                    InviteCodec.parse(url)
                }
                val info = ServerApi.fetchInfo(parsed.host, parsed.port, useTls = true, fingerprint = parsed.fingerprint)
                try {
                    verifyInvite(
                        uniffi.rope_core.InviteLink(
                            parsed.version.toUShort(),
                            parsed.host,
                            parsed.port.toUShort(),
                            parsed.serverId,
                            parsed.fingerprint,
                            parsed.token,
                            parsed.displayName,
                        ),
                        info.getString("server_id"),
                        info.getString("fingerprint"),
                    )
                } catch (e: Exception) {
                    error(IllegalStateException("Fingerprint or server id mismatch. Stopped.", e))
                    return@launch
                }
                val id = identity ?: error("identity missing")
                val boot = ServerApi(dummyProfile(parsed.host, parsed.port, parsed.fingerprint, true), id)
                    .bootstrap(parsed.host, parsed.port, true, parsed.fingerprint, parsed.token, displayName)
                attached(
                    ServerProfile(
                        host = parsed.host,
                        port = parsed.port,
                        serverId = parsed.serverId,
                        fingerprint = parsed.fingerprint,
                        useTls = true,
                        role = boot.getString("role"),
                        memberId = boot.getString("member_id"),
                        deviceId = boot.getString("device_id"),
                    ),
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun joinDevHttp(host: String, port: Int, token: String, displayName: String) {
        scope.launch {
            busy(true)
            try {
                val fp = uniffi.rope_core.devHttpFingerprint()
                val info = ServerApi.fetchInfo(host, port, useTls = false, fingerprint = fp)
                val id = identity ?: error("identity missing")
                val boot = ServerApi(dummyProfile(host, port, fp, false), id)
                    .bootstrap(host, port, false, fp, token, displayName)
                attached(
                    ServerProfile(
                        host = host,
                        port = port,
                        serverId = info.getString("server_id"),
                        fingerprint = fp,
                        useTls = false,
                        role = boot.getString("role"),
                        memberId = boot.getString("member_id"),
                        deviceId = boot.getString("device_id"),
                    ),
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun openChat(device: DirectoryDevice) {
        _state.value = _state.value.copy(
            screen = Screen.Chat,
            peer = device,
            messages = store.messages(device.deviceId),
        )
    }

    fun sendDraft() {
        val text = _state.value.draftText
        val peer = _state.value.peer ?: return
        if (text.isBlank()) return
        _state.value = _state.value.copy(draftText = "")
        scope.launch {
            val id = identity ?: return@launch
            try {
                val env = id.encryptMessage(publicIdentityFromBlob(peer.publicIdentity), text)
                val local = ChatMessage(
                    id = env.messageId,
                    peerDeviceId = peer.deviceId,
                    outgoing = true,
                    text = text,
                    status = MessageStatus.CREATED,
                    timestampMs = env.timestampMs.toLong(),
                )
                store.insertMessage(local)
                refreshMessages(peer.deviceId)
                val payload = JSONObject()
                    .put("type", "send")
                    .put("envelope", Base64.encodeToString(env.bytes, Base64.NO_WRAP))
                    .toString()
                if (socket?.send(payload) != true) {
                    _state.value = _state.value.copy(offline = true, error = "offline — will retry")
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun createInvite() {
        scope.launch {
            busy(true)
            try {
                val profile = store.profile() ?: error("no server")
                val (token, _) = api?.createInvite() ?: error("not connected")
                val url = buildInviteUrl(
                    uniffi.rope_core.InviteLink(
                        1u,
                        profile.host,
                        profile.port.toUShort(),
                        profile.serverId,
                        profile.fingerprint,
                        token,
                        null,
                    ),
                )
                _state.value = _state.value.copy(inviteUrl = url, screen = Screen.Invite, busy = false)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun refreshStatus() {
        scope.launch {
            try {
                val st = api?.status()
                _state.value = _state.value.copy(
                    screen = Screen.Status,
                    statusText = st?.toString(2) ?: "not owner or offline",
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun resume() {
        store.profile()?.let { connectSocket(it) }
    }

    private fun attached(profile: ServerProfile) {
        store.saveProfile(profile)
        api = ServerApi(profile, identity!!)
        _state.value = _state.value.copy(profile = profile, screen = Screen.Chats, busy = false, error = null)
        refreshDirectory()
        connectSocket(profile)
    }

    private fun refreshDirectory() {
        scope.launch {
            try {
                val devices = api?.directory().orEmpty().filter { it.deviceId != identity?.deviceId() }
                _state.value = _state.value.copy(devices = devices)
            } catch (e: Exception) {
                _state.value = _state.value.copy(offline = true, error = e.message)
            }
        }
    }

    private fun connectSocket(profile: ServerProfile) {
        socket?.cancel()
        val id = identity ?: return
        val currentApi = ServerApi(profile, id)
        api = currentApi
        socket = currentApi.openSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.value = _state.value.copy(offline = false)
                flushOutbox()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWs(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = _state.value.copy(offline = true)
            }
        })
    }

    private fun handleWs(text: String) {
        val obj = JSONObject(text)
        when (obj.optString("type")) {
            "queued" -> {
                val mid = obj.optString("message_id")
                store.updateStatus(mid, MessageStatus.SENT_TO_SERVER)
                _state.value.peer?.let { refreshMessages(it.deviceId) }
            }
            "delivered" -> {
                val mid = obj.optString("message_id")
                store.updateStatus(mid, MessageStatus.DELIVERED_TO_DEVICE)
                _state.value.peer?.let { refreshMessages(it.deviceId) }
            }
            "deliver" -> {
                val env = Base64.decode(obj.getString("envelope"), Base64.DEFAULT)
                val id = identity ?: return
                val meta = uniffi.rope_core.parseEnvelope(env)
                val sender = _state.value.devices.find { it.deviceId == meta.senderId }
                    ?: api?.directory()?.find { it.deviceId == meta.senderId }
                    ?: return
                val plain = id.decryptMessage(publicIdentityFromBlob(sender.publicIdentity), env)
                val msg = ChatMessage(
                    id = plain.messageId,
                    peerDeviceId = sender.deviceId,
                    outgoing = false,
                    text = plain.text,
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = plain.timestampMs.toLong(),
                )
                store.insertMessage(msg)
                socket?.send(JSONObject().put("type", "ack").put("message_id", plain.messageId).toString())
                if (_state.value.peer?.deviceId == sender.deviceId) {
                    refreshMessages(sender.deviceId)
                }
                refreshDirectory()
            }
            "error" -> {
                _state.value = _state.value.copy(error = obj.optString("message"))
            }
        }
    }

    private fun flushOutbox() {
        val id = identity ?: return
        val devices = try {
            api?.directory().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        for (msg in store.pendingOutgoing()) {
            val peer = devices.find { it.deviceId == msg.peerDeviceId } ?: continue
            try {
                val env = id.encryptMessage(publicIdentityFromBlob(peer.publicIdentity), msg.text)
                socket?.send(
                    JSONObject()
                        .put("type", "send")
                        .put("envelope", Base64.encodeToString(env.bytes, Base64.NO_WRAP))
                        .toString(),
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun refreshMessages(peerId: String) {
        _state.value = _state.value.copy(messages = store.messages(peerId))
    }

    private fun dummyProfile(host: String, port: Int, fp: String, tls: Boolean) = ServerProfile(
        host, port, "", fp, tls, "", "", identity?.deviceId().orEmpty(),
    )

    private fun busy(v: Boolean) {
        _state.value = _state.value.copy(busy = v, error = null)
    }

    private fun error(e: Exception) {
        _state.value = _state.value.copy(busy = false, error = e.message ?: e.toString())
    }
}

