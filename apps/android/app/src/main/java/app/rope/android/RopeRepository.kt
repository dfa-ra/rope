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
import app.rope.android.provision.ProvisionForm
import app.rope.android.provision.SshProvisioner
import app.rope.android.update.AppRelease
import app.rope.android.update.AppUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import uniffi.rope_core.DeviceIdentity
import uniffi.rope_core.buildInviteUrl
import uniffi.rope_core.parseInviteUrl
import uniffi.rope_core.publicIdentityFromBlob
import uniffi.rope_core.verifyInvite
import java.io.File

data class UiState(
    val screen: Screen = Screen.Start,
    val profile: ServerProfile? = null,
    val devices: List<DirectoryDevice> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val peer: DirectoryDevice? = null,
    val inviteUrl: String? = null,
    val pendingInvite: String? = null,
    val statusText: String = "",
    val updateText: String = "",
    val pendingApkPath: String? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val offline: Boolean = false,
    val draftHost: String = "",
    val draftText: String = "",
    val onlineIds: Set<String> = emptySet(),
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
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    fun start(pendingLink: String?) {
        scope.launch {
            try {
                identity = if (vault.exists()) DeviceIdentity.fromBytes(vault.load()) else DeviceIdentity.generate().also {
                    vault.save(it.toBytes())
                }
                store.profile()?.let { attached(it) }
                if (!pendingLink.isNullOrBlank() && store.profile() == null) {
                    prepareJoin(pendingLink)
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun prepareJoin(url: String) {
        _state.value = _state.value.copy(
            screen = Screen.Join,
            pendingInvite = url,
            error = null,
        )
    }

    fun go(screen: Screen) {
        _state.value = _state.value.copy(screen = screen, error = null)
    }

    fun setDraft(text: String) {
        _state.value = _state.value.copy(draftText = text)
    }

    fun provision(form: ProvisionForm) {
        scope.launch {
            if (!form.upgrade && !LoginRules.isValid(form.displayName)) {
                _state.value = _state.value.copy(error = "Придумайте логин: 2–24 символа, буквы/цифры/_ . -")
                return@launch
            }
            busy(true)
            try {
                store.saveGithubToken(form.githubToken)
                val result = SshProvisioner(app).install(form)
                if (form.upgrade) {
                    _state.value = _state.value.copy(statusText = "ядро сервера обновлено", busy = false, screen = Screen.Status)
                    return@launch
                }
                val id = identity ?: error("identity missing")
                val login = LoginRules.normalize(form.displayName)
                val boot = ServerApi(dummyProfile(result.host, result.port, result.fingerprint, true), id)
                    .bootstrap(result.host, result.port, true, result.fingerprint, result.setupToken, login)
                val profile = ServerProfile(
                    host = result.host,
                    port = result.port,
                    serverId = result.serverId,
                    fingerprint = result.fingerprint,
                    useTls = true,
                    role = boot.getString("role"),
                    memberId = boot.getString("member_id"),
                    deviceId = boot.getString("device_id"),
                    displayName = login,
                )
                attached(profile)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun join(url: String, displayName: String) {
        scope.launch {
            if (!LoginRules.isValid(displayName)) {
                _state.value = _state.value.copy(error = "Придумайте логин: 2–24 символа, буквы/цифры/_ . -")
                return@launch
            }
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
                val login = LoginRules.normalize(displayName)
                val boot = ServerApi(dummyProfile(parsed.host, parsed.port, parsed.fingerprint, true), id)
                    .bootstrap(parsed.host, parsed.port, true, parsed.fingerprint, parsed.token, login)
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
                        displayName = login,
                    ),
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun joinDevHttp(host: String, port: Int, token: String, displayName: String) {
        scope.launch {
            if (!LoginRules.isValid(displayName)) {
                _state.value = _state.value.copy(error = "Придумайте логин: 2–24 символа, буквы/цифры/_ . -")
                return@launch
            }
            busy(true)
            try {
                val fp = uniffi.rope_core.devHttpFingerprint()
                val info = ServerApi.fetchInfo(host, port, useTls = false, fingerprint = fp)
                val id = identity ?: error("identity missing")
                val login = LoginRules.normalize(displayName)
                val boot = ServerApi(dummyProfile(host, port, fp, false), id)
                    .bootstrap(host, port, false, fp, token, login)
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
                        displayName = login,
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
                    envelope = env.bytes,
                )
                store.insertMessage(local)
                refreshMessages(peer.deviceId)
                val sent = socket?.send(sendPayload(env.bytes)) == true
                if (!sent) {
                    _state.value = _state.value.copy(offline = true, error = "нет сети — отправится при подключении")
                    scheduleReconnect()
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
                    statusText = st?.toString(2) ?: "нет прав владельца или офлайн",
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun updateApp() {
        scope.launch {
            busy(true)
            try {
                val latest = AppUpdater().latestApk(store.githubToken())
                val local = BuildConfig.VERSION_NAME
                if (!AppRelease.isNewer(latest.version, local)) {
                    _state.value = _state.value.copy(
                        busy = false,
                        screen = Screen.Status,
                        updateText = "Уже стоит $local",
                    )
                    return@launch
                }
                val dest = File(app.cacheDir, "updates/${latest.assetName}")
                dest.parentFile?.mkdirs()
                AppUpdater().download(latest, dest, store.githubToken())
                _state.value = _state.value.copy(
                    busy = false,
                    screen = Screen.Status,
                    pendingApkPath = dest.absolutePath,
                    updateText = "Скачано ${latest.version}. Подтвердите установку — удалять приложение не нужно.",
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun consumePendingApk() {
        _state.value = _state.value.copy(pendingApkPath = null)
    }

    fun resume() {
        store.profile()?.let { connectSocket(it) }
    }

    private fun attached(profile: ServerProfile) {
        store.saveProfile(profile)
        api = ServerApi(profile, identity!!)
        _state.value = _state.value.copy(
            profile = profile,
            screen = Screen.Chats,
            busy = false,
            error = null,
            pendingInvite = null,
        )
        refreshDirectory()
        connectSocket(profile)
    }

    private fun refreshDirectory() {
        scope.launch {
            try {
                val online = _state.value.onlineIds
                val devices = api?.directory().orEmpty()
                    .filter { it.deviceId != identity?.deviceId() }
                    .map { it.copy(online = it.online || it.deviceId in online) }
                val peer = _state.value.peer?.let { cur -> devices.find { it.deviceId == cur.deviceId } ?: cur }
                _state.value = _state.value.copy(devices = devices, peer = peer)
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
                reconnectAttempt = 0
                _state.value = _state.value.copy(offline = false, error = null)
                refreshDirectory()
                flushOutbox()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWs(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (webSocket != socket) return
                _state.value = _state.value.copy(offline = true)
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket != socket) return
                _state.value = _state.value.copy(offline = true)
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (store.profile() == null) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val wait = (1_000L * (1 shl reconnectAttempt.coerceAtMost(5))).coerceAtMost(30_000L)
            reconnectAttempt += 1
            delay(wait)
            store.profile()?.let { connectSocket(it) }
        }
    }

    private fun handleWs(text: String) {
        val obj = JSONObject(text)
        when (obj.optString("type")) {
            "presence" -> applyPresence(obj.optJSONArray("devices"))
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
                val sender = findSender(meta.senderId) ?: return
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

    private fun findSender(senderId: String): DirectoryDevice? {
        _state.value.devices.find { it.deviceId == senderId }?.let { return it }
        return try {
            val devices = api?.directory().orEmpty()
            val online = _state.value.onlineIds
            val mapped = devices.map { it.copy(online = it.online || it.deviceId in online) }
            _state.value = _state.value.copy(
                devices = mapped.filter { it.deviceId != identity?.deviceId() },
            )
            mapped.find { it.deviceId == senderId }
        } catch (_: Exception) {
            null
        }
    }

    private fun applyPresence(arr: JSONArray?) {
        val ids = mutableSetOf<String>()
        if (arr != null) {
            for (i in 0 until arr.length()) ids += arr.getString(i)
        }
        val devices = _state.value.devices.map { it.copy(online = it.deviceId in ids) }
        val peer = _state.value.peer?.let { it.copy(online = it.deviceId in ids) }
        _state.value = _state.value.copy(onlineIds = ids, devices = devices, peer = peer)
    }

    private fun flushOutbox() {
        val id = identity ?: return
        val devices = try {
            api?.directory().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        for (msg in store.pendingOutgoing()) {
            val bytes = msg.envelope ?: encryptAgain(id, devices, msg) ?: continue
            try {
                socket?.send(sendPayload(bytes))
            } catch (_: Exception) {
            }
        }
    }

    private fun encryptAgain(
        id: DeviceIdentity,
        devices: List<DirectoryDevice>,
        msg: ChatMessage,
    ): ByteArray? {
        val peer = devices.find { it.deviceId == msg.peerDeviceId } ?: return null
        return try {
            id.encryptMessage(publicIdentityFromBlob(peer.publicIdentity), msg.text).bytes
        } catch (_: Exception) {
            null
        }
    }

    private fun sendPayload(envelope: ByteArray): String =
        JSONObject()
            .put("type", "send")
            .put("envelope", Base64.encodeToString(envelope, Base64.NO_WRAP))
            .toString()

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
