package app.rope.android

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import app.rope.android.data.AdminSnapshot
import app.rope.android.data.CallInfo
import app.rope.android.data.CallPhase
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.EnvelopeTypes
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.IdentityVault
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.RopeGroup
import app.rope.android.data.ServerProfile
import app.rope.android.data.SshTarget
import app.rope.android.media.VoicePlayer
import app.rope.android.media.VoiceRecorder
import app.rope.android.net.ServerApi
import app.rope.android.notify.RopeNotifier
import app.rope.android.protocol.InviteCodec
import app.rope.android.protocol.InviteLink as ParsedInvite
import app.rope.android.provision.ProvisionForm
import app.rope.android.provision.ServerTarget
import app.rope.android.provision.SshProvisioner
import app.rope.android.update.DeviceBackup
import app.rope.android.update.PublicDownloads
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
import uniffi.rope_core.decryptObject
import uniffi.rope_core.encryptObject
import uniffi.rope_core.knownEnvelopeType
import uniffi.rope_core.parseInviteUrl
import uniffi.rope_core.publicIdentityFromBlob
import uniffi.rope_core.verifyInvite
import java.io.File
import java.util.UUID

data class UiState(
    val screen: Screen = Screen.Start,
    val profile: ServerProfile? = null,
    val devices: List<DirectoryDevice> = emptyList(),
    val groups: List<RopeGroup> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val peer: DirectoryDevice? = null,
    val group: RopeGroup? = null,
    val inviteUrl: String? = null,
    val pendingInvite: String? = null,
    val statusText: String = "",
    val admin: AdminSnapshot? = null,
    val updateText: String = "",
    val pendingApkPath: String? = null,
    val installTick: Int = 0,
    val error: String? = null,
    val busy: Boolean = false,
    val offline: Boolean = false,
    val draftHost: String = "",
    val draftText: String = "",
    val onlineIds: Set<String> = emptySet(),
    val recording: Boolean = false,
    val recordMs: Long = 0,
    val playingVoiceId: String? = null,
    val call: CallInfo? = null,
    val groupNameDraft: String = "",
    val pickedMembers: Set<String> = emptySet(),
)

enum class Screen { Start, Provision, Join, Chats, Chat, Invite, Status, Settings, NewGroup, GroupInfo }

class RopeRepository(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = LocalStore(app)
    private val vault = IdentityVault(app)
    private val notifier = RopeNotifier(app)
    private val voiceRecorder = VoiceRecorder(app)
    private val voicePlayer = VoicePlayer()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var identity: DeviceIdentity? = null
    private var api: ServerApi? = null
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var recordJob: Job? = null
    private var reconnectAttempt = 0
    private var tone: ToneGenerator? = null

    fun start(pendingLink: String?) {
        scope.launch {
            try {
                if (!vault.exists()) tryRestoreBackup()
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
        if (screen == Screen.Chats) refreshConversations()
        if (screen == Screen.NewGroup) {
            _state.value = _state.value.copy(groupNameDraft = "", pickedMembers = emptySet())
        }
    }

    fun setDraft(text: String) {
        _state.value = _state.value.copy(draftText = text)
    }

    fun setGroupName(name: String) {
        _state.value = _state.value.copy(groupNameDraft = name)
    }

    fun toggleMember(deviceId: String) {
        val cur = _state.value.pickedMembers.toMutableSet()
        if (!cur.add(deviceId)) cur.remove(deviceId)
        _state.value = _state.value.copy(pickedMembers = cur)
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
                store.saveSshTarget(SshTarget(form.host, form.sshPort, form.user, form.listenPort))
                val result = SshProvisioner(app).install(form)
                if (form.upgrade) {
                    val st = runCatching { api?.status() }.getOrNull()
                    _state.value = _state.value.copy(
                        statusText = st?.toString(2) ?: "ядро сервера обновлено",
                        admin = st?.let { AdminSnapshot.from(it) } ?: _state.value.admin,
                        updateText = "Ядро на VPS обновлено, чаты и owner на месте.",
                        busy = false,
                        screen = Screen.Status,
                        error = null,
                    )
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
            group = null,
            messages = store.messages(device.deviceId),
        )
    }

    fun openGroup(group: RopeGroup) {
        _state.value = _state.value.copy(
            screen = Screen.Chat,
            peer = null,
            group = group,
            messages = store.messages(ChatIds.group(group.groupId)),
        )
    }

    fun openConversation(c: Conversation) {
        if (c.isGroup && c.group != null) openGroup(c.group) else c.peer?.let { openChat(it) }
    }

    fun sendDraft() {
        val text = _state.value.draftText
        if (text.isBlank() || _state.value.recording) return
        _state.value = _state.value.copy(draftText = "")
        val group = _state.value.group
        if (group != null) {
            sendGroupText(group, text)
            return
        }
        val peer = _state.value.peer ?: return
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
                    kind = MessageKind.TEXT,
                    senderId = id.deviceId(),
                    senderName = _state.value.profile?.displayName.orEmpty(),
                )
                store.insertMessage(local)
                refreshMessages(peer.deviceId)
                pushEnvelope(env.bytes)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun sendAttachment(uri: Uri, forcedMime: String? = null) {
        scope.launch {
            try {
                val cr = app.contentResolver
                val mime = forcedMime ?: cr.getType(uri) ?: "application/octet-stream"
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
                val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: error("не удалось прочитать файл")
                if (bytes.size > 25 * 1024 * 1024) {
                    _state.value = _state.value.copy(error = "Файл больше 25 МБ")
                    return@launch
                }
                val kind = when {
                    mime.startsWith("image/") -> "image"
                    mime.startsWith("audio/") -> "voice"
                    else -> "file"
                }
                sendMediaBytes(bytes, mime, name, kind, 0)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun startVoice() {
        if (_state.value.recording) return
        try {
            voiceRecorder.start()
            _state.value = _state.value.copy(recording = true, recordMs = 0, error = null)
            recordJob?.cancel()
            recordJob = scope.launch {
                while (_state.value.recording) {
                    delay(200)
                    val next = _state.value.recordMs + 200
                    _state.value = _state.value.copy(recordMs = next)
                    if (next >= VoiceRecorder.MAX_MS) {
                        finishVoice(send = true)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            error(e)
        }
    }

    fun finishVoice(send: Boolean) {
        val take = try {
            voiceRecorder.stop()
        } catch (_: Exception) {
            null
        }
        recordJob?.cancel()
        _state.value = _state.value.copy(recording = false, recordMs = 0)
        if (!send || take == null || take.durationMs < VoiceRecorder.MIN_MS) {
            take?.file?.delete()
            return
        }
        scope.launch {
            try {
                val bytes = take.file.readBytes()
                sendMediaBytes(bytes, "audio/mp4", take.file.name, "voice", take.durationMs)
            } catch (e: Exception) {
                error(e)
            } finally {
                take.file.delete()
            }
        }
    }

    fun toggleVoice(msg: ChatMessage) {
        val path = msg.localPath ?: return
        voicePlayer.toggle(msg.id, path)
        _state.value = _state.value.copy(playingVoiceId = voicePlayer.playingId)
    }

    fun createGroup() {
        val name = _state.value.groupNameDraft.trim()
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Название группы")
            return
        }
        scope.launch {
            busy(true)
            try {
                var g = api?.createGroup(name) ?: error("нет сети")
                for (id in _state.value.pickedMembers) {
                    g = api?.addGroupMember(g.groupId, id) ?: g
                }
                store.upsertGroup(g)
                _state.value = _state.value.copy(busy = false, error = null)
                refreshDirectory()
                openGroup(g)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun addMemberToOpenGroup(deviceId: String) {
        val g = _state.value.group ?: return
        scope.launch {
            try {
                val updated = api?.addGroupMember(g.groupId, deviceId) ?: return@launch
                store.upsertGroup(updated)
                _state.value = _state.value.copy(group = updated)
                refreshDirectory()
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun removeMemberFromOpenGroup(deviceId: String) {
        val g = _state.value.group ?: return
        scope.launch {
            try {
                val updated = api?.removeGroupMember(g.groupId, deviceId) ?: return@launch
                store.upsertGroup(updated)
                _state.value = _state.value.copy(group = updated)
                refreshDirectory()
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun startCall() {
        val peer = _state.value.peer ?: return
        val call = CallInfo(
            callId = UUID.randomUUID().toString(),
            peerDeviceId = peer.deviceId,
            peerName = peer.displayName,
            outgoing = true,
            phase = CallPhase.RINGING_OUT,
        )
        _state.value = _state.value.copy(call = call)
        sendCall(call.callId, peer.deviceId, "ring", "")
        startTone(true)
        audioMode(true)
    }

    fun acceptCall() {
        val call = _state.value.call ?: return
        _state.value = _state.value.copy(call = call.copy(phase = CallPhase.ACTIVE))
        sendCall(call.callId, call.peerDeviceId, "accept", "")
        stopTone()
        audioMode(true)
        notifier.clearCall()
    }

    fun rejectCall() {
        val call = _state.value.call ?: return
        sendCall(call.callId, call.peerDeviceId, "reject", "")
        endCall()
    }

    fun hangup() {
        val call = _state.value.call ?: return
        sendCall(call.callId, call.peerDeviceId, "hangup", "")
        endCall()
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
                val latest = runCatching { AppUpdater().latestApk(store.githubToken()) }.getOrNull()
                val updateHint = when {
                    latest == null -> _state.value.updateText
                    !AppRelease.isNewer(latest.version, BuildConfig.VERSION_NAME) ->
                        "Уже стоит ${BuildConfig.VERSION_NAME}"
                    else -> "Доступно приложение ${latest.version}. Поставится поверх, без удаления."
                }
                _state.value = _state.value.copy(
                    screen = Screen.Status,
                    statusText = st?.toString(2) ?: "нет прав владельца или офлайн",
                    admin = st?.let { AdminSnapshot.from(it) },
                    updateText = updateHint,
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun upgradeCore(password: String, keyPem: String) {
        val profile = store.profile()
        if (profile == null) {
            _state.value = _state.value.copy(error = "сначала подключитесь к серверу")
            return
        }
        if (password.isBlank() && keyPem.isBlank()) {
            _state.value = _state.value.copy(error = "Введите SSH-пароль или ключ")
            return
        }
        val ssh = store.sshTarget() ?: SshTarget(profile.host, 22, "root", profile.port)
        provision(
            ProvisionForm(
                host = ssh.host.ifBlank { profile.host },
                sshPort = ssh.sshPort,
                user = ssh.user.ifBlank { "root" },
                password = password,
                keyPem = keyPem,
                listenPort = ssh.listenPort.takeIf { it > 0 } ?: profile.port,
                target = ServerTarget.AUTO,
                binaryUrl = "",
                displayName = profile.displayName,
                githubToken = store.githubToken().orEmpty(),
                upgrade = true,
            ),
        )
    }

    fun updateApp() {
        val existing = _state.value.pendingApkPath
        if (!existing.isNullOrBlank() && File(existing).isFile) {
            _state.value = _state.value.copy(
                installTick = _state.value.installTick + 1,
                error = null,
                updateText = "Повтор установки. Подтвердите в системном окне.",
            )
            return
        }
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
                writeUpdateArtifacts(dest, latest.assetName)
                _state.value = _state.value.copy(
                    busy = false,
                    screen = Screen.Status,
                    pendingApkPath = dest.absolutePath,
                    installTick = _state.value.installTick + 1,
                    error = null,
                    updateText = "Ставим ${latest.version} поверх. Подтвердите установку в системном окне.",
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun consumePendingApk() {
        _state.value = _state.value.copy(pendingApkPath = null)
    }

    fun onApkInstalled() {
        _state.value = _state.value.copy(
            pendingApkPath = null,
            updateText = "приложение обновлено",
            error = null,
            busy = false,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun onApkInstallFailed(message: String, status: Int) {
        _state.value = _state.value.copy(
            busy = false,
            error = message,
            updateText = message,
        )
    }

    fun restoreFromFile(bytes: ByteArray) {
        scope.launch {
            try {
                applyDeviceBackup(DeviceBackup.parse(bytes))
                identity = DeviceIdentity.fromBytes(vault.load())
                val profile = store.profile()
                if (profile != null) {
                    attached(profile)
                } else {
                    _state.value = _state.value.copy(error = "восстановили ключ, но профиля сервера нет")
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun resume() {
        store.profile()?.let { connectSocket(it) }
    }

    private fun sendMediaBytes(bytes: ByteArray, mime: String, name: String, kind: String, durationMs: Long) {
        val id = identity ?: return
        val api = api ?: throw IllegalStateException("нет сети")
        val enc = encryptObject(bytes)
        val uploaded = api.uploadObject(enc.ciphertext, enc.sha256)
        val objectId = uploaded.getString("object_id")
        val group = _state.value.group
        val payload = MediaPayload(
            kind = kind,
            objectId = objectId,
            sha256 = enc.sha256,
            keyB64 = Base64.encodeToString(enc.key, Base64.NO_WRAP),
            mime = mime,
            name = name,
            size = bytes.size.toLong(),
            durationMs = durationMs,
            groupId = group?.groupId,
        )
        val cache = persistPlain(objectId, name, bytes)
        if (group != null) {
            sendGroupPayload(group, EnvelopeTypes.MEDIA, payload.toJson().toByteArray(), payload.preview(), payload.messageKind(), payload.toJson(), cache)
            return
        }
        val peer = _state.value.peer ?: return
        val env = id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), EnvelopeTypes.MEDIA, payload.toJson().toByteArray())
        val local = ChatMessage(
            id = env.messageId,
            peerDeviceId = peer.deviceId,
            outgoing = true,
            text = payload.preview(),
            status = MessageStatus.CREATED,
            timestampMs = env.timestampMs.toLong(),
            envelope = env.bytes,
            kind = payload.messageKind(),
            extra = payload.toJson(),
            localPath = cache.absolutePath,
            senderId = id.deviceId(),
            senderName = _state.value.profile?.displayName.orEmpty(),
        )
        store.insertMessage(local)
        refreshMessages(peer.deviceId)
        pushEnvelope(env.bytes)
    }

    private fun sendGroupText(group: RopeGroup, text: String) {
        val body = GroupTextPayload(group.groupId, text, group.epoch).toJson().toByteArray()
        sendGroupPayload(group, EnvelopeTypes.GROUP_TEXT, body, text, MessageKind.GROUP_TEXT, "", null)
    }

    private fun sendGroupPayload(
        group: RopeGroup,
        type: UByte,
        body: ByteArray,
        preview: String,
        kind: MessageKind,
        extra: String,
        localFile: File?,
    ) {
        scope.launch {
            val id = identity ?: return@launch
            try {
                val members = group.members.filter { it != id.deviceId() }
                if (members.isEmpty()) {
                    _state.value = _state.value.copy(error = "В группе пока никого нет")
                    return@launch
                }
                val devices = currentDevices()
                val envelopes = JSONArray()
                var firstId = ""
                var ts = System.currentTimeMillis()
                var firstBytes: ByteArray? = null
                for (memberId in members) {
                    val peer = devices.find { it.deviceId == memberId } ?: continue
                    val env = id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), type, body)
                    if (firstId.isBlank()) {
                        firstId = env.messageId
                        ts = env.timestampMs.toLong()
                        firstBytes = env.bytes
                    }
                    envelopes.put(Base64.encodeToString(env.bytes, Base64.NO_WRAP))
                }
                if (envelopes.length() == 0) {
                    _state.value = _state.value.copy(error = "Нет ключей участников")
                    return@launch
                }
                val chatId = ChatIds.group(group.groupId)
                val local = ChatMessage(
                    id = firstId.ifBlank { UUID.randomUUID().toString() },
                    peerDeviceId = chatId,
                    outgoing = true,
                    text = preview,
                    status = MessageStatus.CREATED,
                    timestampMs = ts,
                    envelope = firstBytes,
                    kind = kind,
                    extra = extra,
                    groupId = group.groupId,
                    localPath = localFile?.absolutePath,
                    senderId = id.deviceId(),
                    senderName = _state.value.profile?.displayName.orEmpty(),
                )
                store.insertMessage(local)
                refreshMessages(chatId)
                val sent = socket?.send(
                    JSONObject()
                        .put("type", "group_send")
                        .put("group_id", group.groupId)
                        .put("envelopes", envelopes)
                        .toString(),
                ) == true
                if (!sent) {
                    _state.value = _state.value.copy(offline = true, error = "нет сети — отправится при подключении")
                    scheduleReconnect()
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    private fun persistPlain(objectId: String, name: String, bytes: ByteArray): File {
        val dir = File(app.cacheDir, "media")
        dir.mkdirs()
        val ext = name.substringAfterLast('.', MimeTypeMap.getSingleton().getExtensionFromMimeType("application/octet-stream") ?: "bin")
        val dest = File(dir, "$objectId.$ext")
        dest.writeBytes(bytes)
        return dest
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
                val groups = try {
                    api?.listGroups().orEmpty()
                } catch (_: Exception) {
                    store.groups()
                }
                store.saveGroups(groups)
                val peer = _state.value.peer?.let { cur -> devices.find { it.deviceId == cur.deviceId } ?: cur }
                val group = _state.value.group?.let { cur -> groups.find { it.groupId == cur.groupId } ?: cur }
                _state.value = _state.value.copy(devices = devices, groups = groups, peer = peer, group = group)
                refreshConversations()
            } catch (e: Exception) {
                _state.value = _state.value.copy(offline = true, error = e.message)
            }
        }
    }

    private fun refreshConversations() {
        val devices = _state.value.devices
        val groups = _state.value.groups
        val lastBy = store.conversations().associate { it.first to it.second }
        val dms = devices.map { d ->
            val last = lastBy[d.deviceId]
            Conversation(
                id = d.deviceId,
                title = d.displayName.ifBlank { d.deviceId.take(8) },
                subtitle = last?.text ?: if (d.online) "в сети" else "не в сети",
                isGroup = false,
                online = d.online,
                last = last,
                peer = d,
            )
        }
        val gs = groups.map { g ->
            val last = lastBy[ChatIds.group(g.groupId)]
            Conversation(
                id = ChatIds.group(g.groupId),
                title = g.name,
                subtitle = last?.text ?: "${g.members.size} участников",
                isGroup = true,
                online = g.members.any { it in _state.value.onlineIds && it != identity?.deviceId() },
                last = last,
                group = g,
            )
        }
        val leftover = lastBy.keys
            .filter { id -> dms.none { it.id == id } && gs.none { it.id == id } }
            .map { id ->
                Conversation(
                    id = id,
                    title = if (ChatIds.isGroup(id)) "Группа" else id.take(8),
                    subtitle = lastBy[id]?.text.orEmpty(),
                    isGroup = ChatIds.isGroup(id),
                    online = id in _state.value.onlineIds,
                    last = lastBy[id],
                )
            }
        _state.value = _state.value.copy(
            conversations = (dms + gs + leftover).sortedByDescending { it.last?.timestampMs ?: 0L },
        )
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
                refreshOpenChat()
            }
            "delivered" -> {
                val mid = obj.optString("message_id")
                store.updateStatus(mid, MessageStatus.DELIVERED_TO_DEVICE)
                refreshOpenChat()
            }
            "deliver" -> handleDeliver(obj)
            "call" -> handleCallEvent(obj)
            "error" -> {
                _state.value = _state.value.copy(error = obj.optString("message"))
            }
        }
    }

    private fun handleDeliver(obj: JSONObject) {
        val env = Base64.decode(obj.getString("envelope"), Base64.DEFAULT)
        val id = identity ?: return
        val meta = uniffi.rope_core.parseEnvelope(env)
        val sender = findSender(meta.senderId) ?: return
        if (!knownEnvelopeType(meta.msgType)) {
            val unknown = ChatMessage(
                id = meta.messageId,
                peerDeviceId = sender.deviceId,
                outgoing = false,
                text = "Неизвестный тип сообщения. Обновите Rope.",
                status = MessageStatus.DELIVERED_TO_DEVICE,
                timestampMs = meta.timestampMs.toLong(),
                kind = MessageKind.UNKNOWN,
                senderId = sender.deviceId,
                senderName = sender.displayName,
            )
            store.insertMessage(unknown)
            ack(meta.messageId)
            refreshOpenChat()
            return
        }
        when (meta.msgType) {
            EnvelopeTypes.TEXT -> {
                val plain = id.decryptMessage(publicIdentityFromBlob(sender.publicIdentity), env)
                val msg = ChatMessage(
                    id = plain.messageId,
                    peerDeviceId = sender.deviceId,
                    outgoing = false,
                    text = plain.text,
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = plain.timestampMs.toLong(),
                    kind = MessageKind.TEXT,
                    senderId = sender.deviceId,
                    senderName = sender.displayName,
                )
                store.insertMessage(msg)
                ack(plain.messageId)
                notifyIfHidden(sender.displayName, plain.text, sender.deviceId)
            }
            EnvelopeTypes.GROUP_TEXT -> {
                val typed = id.decryptTyped(publicIdentityFromBlob(sender.publicIdentity), env)
                val payload = GroupTextPayload.parse(String(typed.body))
                val chatId = ChatIds.group(payload.groupId)
                val msg = ChatMessage(
                    id = typed.messageId,
                    peerDeviceId = chatId,
                    outgoing = false,
                    text = payload.text,
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = typed.timestampMs.toLong(),
                    kind = MessageKind.GROUP_TEXT,
                    groupId = payload.groupId,
                    senderId = sender.deviceId,
                    senderName = sender.displayName,
                )
                store.insertMessage(msg)
                ack(typed.messageId)
                notifyIfHidden(sender.displayName, payload.text, chatId)
            }
            EnvelopeTypes.MEDIA -> {
                val typed = id.decryptTyped(publicIdentityFromBlob(sender.publicIdentity), env)
                val payload = MediaPayload.parse(String(typed.body))
                val chatId = payload.groupId?.let { ChatIds.group(it) } ?: sender.deviceId
                val msg = ChatMessage(
                    id = typed.messageId,
                    peerDeviceId = chatId,
                    outgoing = false,
                    text = payload.preview(),
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = typed.timestampMs.toLong(),
                    kind = payload.messageKind(),
                    extra = payload.toJson(),
                    groupId = payload.groupId,
                    senderId = sender.deviceId,
                    senderName = sender.displayName,
                )
                store.insertMessage(msg)
                ack(typed.messageId)
                notifyIfHidden(sender.displayName, payload.preview(), chatId)
                scope.launch { downloadMedia(msg.id, payload) }
            }
            else -> {
                ack(meta.messageId)
            }
        }
        refreshOpenChat()
        refreshDirectory()
    }

    private fun downloadMedia(messageId: String, payload: MediaPayload) {
        try {
            val api = api ?: return
            val (blob, headerHash) = api.downloadObject(payload.objectId)
            val expected = payload.sha256.ifBlank { headerHash }
            val key = Base64.decode(payload.keyB64, Base64.DEFAULT)
            val plain = decryptObject(key, blob, expected)
            val dest = persistPlain(payload.objectId, payload.name, plain)
            store.updateLocalPath(messageId, dest.absolutePath)
            refreshOpenChat()
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "не скачалось вложение: ${e.message}")
        }
    }

    private fun handleCallEvent(obj: JSONObject) {
        val from = obj.optString("from")
        val event = obj.optString("event")
        val callId = obj.optString("call_id")
        val name = _state.value.devices.find { it.deviceId == from }?.displayName ?: from.take(8)
        when (event) {
            "ring" -> {
                _state.value = _state.value.copy(
                    call = CallInfo(callId, from, name, outgoing = false, phase = CallPhase.RINGING_IN, payload = obj.optString("payload")),
                )
                notifier.incomingCall(name)
                startTone(false)
                audioMode(true)
            }
            "accept" -> {
                val cur = _state.value.call ?: return
                _state.value = _state.value.copy(call = cur.copy(phase = CallPhase.ACTIVE))
                stopTone()
            }
            "reject", "hangup" -> endCall()
        }
    }

    private fun sendCall(callId: String, to: String, event: String, payload: String) {
        val sent = socket?.send(
            JSONObject()
                .put("type", "call")
                .put("call_id", callId)
                .put("to", to)
                .put("event", event)
                .put("payload", payload)
                .toString(),
        ) == true
        if (!sent && event == "ring") {
            _state.value = _state.value.copy(error = "собеседник не в сети")
            endCall()
        }
    }

    private fun endCall() {
        stopTone()
        audioMode(false)
        notifier.clearCall()
        _state.value = _state.value.copy(call = null)
    }

    private fun startTone(outgoing: Boolean) {
        stopTone()
        tone = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80).also {
            it.startTone(if (outgoing) ToneGenerator.TONE_SUP_RINGTONE else ToneGenerator.TONE_SUP_RINGTONE, 30_000)
        }
    }

    private fun stopTone() {
        try {
            tone?.stopTone()
            tone?.release()
        } catch (_: Exception) {
        }
        tone = null
    }

    private fun audioMode(on: Boolean) {
        val am = app.getSystemService(AudioManager::class.java) ?: return
        am.mode = if (on) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
        am.isSpeakerphoneOn = false
    }

    private fun notifyIfHidden(title: String, body: String, chatId: String) {
        val open = _state.value.screen == Screen.Chat && (
            _state.value.peer?.deviceId == chatId ||
                (_state.value.group != null && ChatIds.group(_state.value.group!!.groupId) == chatId)
            )
        if (!open) notifier.message(title, body)
    }

    private fun ack(messageId: String) {
        socket?.send(JSONObject().put("type", "ack").put("message_id", messageId).toString())
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
        refreshConversations()
    }

    private fun flushOutbox() {
        val id = identity ?: return
        val devices = currentDevices()
        for (msg in store.pendingOutgoing()) {
            if (ChatIds.isGroup(msg.peerDeviceId)) {
                val gid = msg.groupId ?: ChatIds.rawGroupId(msg.peerDeviceId)
                val group = store.group(gid) ?: continue
                val body = when (msg.kind) {
                    MessageKind.GROUP_TEXT, MessageKind.TEXT -> GroupTextPayload(gid, msg.text, group.epoch).toJson().toByteArray()
                    else -> msg.extra.toByteArray()
                }
                val type = if (msg.kind == MessageKind.GROUP_TEXT || msg.kind == MessageKind.TEXT) EnvelopeTypes.GROUP_TEXT else EnvelopeTypes.MEDIA
                val envelopes = JSONArray()
                for (memberId in group.members.filter { it != id.deviceId() }) {
                    val peer = devices.find { it.deviceId == memberId } ?: continue
                    val env = runCatching { id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), type, body) }.getOrNull() ?: continue
                    envelopes.put(Base64.encodeToString(env.bytes, Base64.NO_WRAP))
                }
                socket?.send(
                    JSONObject().put("type", "group_send").put("group_id", gid).put("envelopes", envelopes).toString(),
                )
            } else {
                val bytes = msg.envelope ?: encryptAgain(id, devices, msg) ?: continue
                try {
                    socket?.send(sendPayload(bytes))
                } catch (_: Exception) {
                }
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
            if (msg.kind == MessageKind.TEXT) {
                id.encryptMessage(publicIdentityFromBlob(peer.publicIdentity), msg.text).bytes
            } else {
                id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), EnvelopeTypes.MEDIA, msg.extra.toByteArray()).bytes
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun pushEnvelope(envelope: ByteArray) {
        val sent = socket?.send(sendPayload(envelope)) == true
        if (!sent) {
            _state.value = _state.value.copy(offline = true, error = "нет сети — отправится при подключении")
            scheduleReconnect()
        }
    }

    private fun sendPayload(envelope: ByteArray): String =
        JSONObject()
            .put("type", "send")
            .put("envelope", Base64.encodeToString(envelope, Base64.NO_WRAP))
            .toString()

    private fun refreshMessages(peerId: String) {
        val openId = when {
            _state.value.group != null -> ChatIds.group(_state.value.group!!.groupId)
            else -> _state.value.peer?.deviceId
        }
        if (openId == peerId) {
            _state.value = _state.value.copy(messages = store.messages(peerId))
        }
        refreshConversations()
    }

    private fun refreshOpenChat() {
        val openId = when {
            _state.value.group != null -> ChatIds.group(_state.value.group!!.groupId)
            else -> _state.value.peer?.deviceId
        } ?: return
        _state.value = _state.value.copy(messages = store.messages(openId))
        refreshConversations()
    }

    private fun currentDevices(): List<DirectoryDevice> = try {
        api?.directory().orEmpty()
    } catch (_: Exception) {
        _state.value.devices
    }

    private fun writeUpdateArtifacts(apk: File, assetName: String) {
        val id = identity ?: return
        val backup = DeviceBackup(
            identity = id.toBytes(),
            profileJson = store.profileJson().orEmpty(),
            githubToken = store.githubToken().orEmpty(),
            sshJson = store.sshJson().orEmpty(),
        )
        runCatching {
            PublicDownloads.write(app, DeviceBackup.FILE_NAME, "application/octet-stream", backup.toBytes())
        }
        runCatching {
            PublicDownloads.write(app, assetName, "application/vnd.android.package-archive", apk.readBytes())
        }
    }

    private fun tryRestoreBackup() {
        val raw = PublicDownloads.read(app, DeviceBackup.FILE_NAME) ?: return
        applyDeviceBackup(DeviceBackup.parse(raw))
    }

    private fun applyDeviceBackup(backup: DeviceBackup) {
        vault.save(backup.identity)
        store.applyBackup(backup)
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
