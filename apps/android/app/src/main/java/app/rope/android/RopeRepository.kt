package app.rope.android

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import app.rope.android.data.AdminSnapshot
import app.rope.android.data.CallInfo
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMedia
import app.rope.android.data.CallPhase
import app.rope.android.data.CallSignal
import app.rope.android.data.IceServerSpec
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
import app.rope.android.data.ReactionPayload
import app.rope.android.data.ChatControl
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatPrefs
import app.rope.android.data.RoleRules
import app.rope.android.data.TextBody
import app.rope.android.data.TypingRules
import app.rope.android.data.MessageStatus
import app.rope.android.data.RopeGroup
import app.rope.android.data.ServerProfile
import app.rope.android.data.SshTarget
import app.rope.android.data.ThemeMode
import app.rope.android.data.ChatRouting
import app.rope.android.data.JsonIds
import app.rope.android.data.IceServers
import app.rope.android.media.ImageCodec
import app.rope.android.media.VoicePlayer
import app.rope.android.media.VoiceRecorder
import app.rope.android.media.WebRtcSession
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
import kotlinx.coroutines.withContext
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
    val backStack: List<Screen> = listOf(Screen.Start),
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
    val voiceProgressId: String? = null,
    val voicePositionMs: Long = 0,
    val voiceDurationMs: Long = 0,
    val call: CallInfo? = null,
    val groupNameDraft: String = "",
    val pickedMembers: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.DARK,
    val appUpdateAvailable: Boolean = false,
    val latestAppVersion: String = "",
    val replyTo: ChatMessage? = null,
    val editTarget: ChatMessage? = null,
    val forwarding: ChatMessage? = null,
    val chatQuery: String = "",
    val messageQuery: String = "",
    val typingName: String? = null,
    val viewingImage: ChatMessage? = null,
    val scrollToMessageId: String? = null,
    val notice: String? = null,
    val pinnedMessageId: String? = null,
    val sessionReady: Boolean = false,
)

enum class Screen { Start, Provision, Join, Home, Chats, Chat, Groups, Calls, People, Invite, Status, Settings, NewGroup, GroupInfo }

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
    private var voiceProgressJob: Job? = null
    private var reconnectAttempt = 0
    private var tone: ToneGenerator? = null
    private val mediaAttempts = mutableSetOf<String>()
    private var lastTypingSentAt = 0L
    private val typingUntil = mutableMapOf<String, Pair<String, Long>>()
    private var typingJob: Job? = null
    private var rtc: WebRtcSession? = null
    private val queuedSignals = mutableListOf<CallSignal>()
    private var connectWatch: Job? = null
    private var iceRestarted = false
    private val rtcLock = Any()

    fun start(pendingLink: String?) {
        scope.launch {
            try {
                val night = app.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                _state.value = _state.value.copy(theme = store.themeMode(night))
                store.rehomeMisroutedMedia()
                if (!vault.exists()) tryRestoreBackup()
                identity = if (vault.exists()) DeviceIdentity.fromBytes(vault.load()) else DeviceIdentity.generate().also {
                    vault.save(it.toBytes())
                }
                store.profile()?.let { attached(it) }
                checkAppUpdate(openStatus = false)
                if (!pendingLink.isNullOrBlank() && store.profile() == null) {
                    prepareJoin(pendingLink)
                }
            } catch (e: Exception) {
                error(e)
            } finally {
                _state.value = _state.value.copy(sessionReady = true)
            }
        }
    }

    fun prepareJoin(url: String) {
        _state.value = applyNav(Screen.Join, NavMode.Push).copy(
            pendingInvite = url,
            error = null,
        )
    }

    fun go(screen: Screen, tab: Boolean = false) {
        if (screen != Screen.Chat) persistOpenDraft()
        _state.value = applyNav(screen, if (tab) NavMode.SwitchTab else NavMode.Push).copy(error = null)
        if (NavRules.refreshesLists(screen)) refreshConversations()
        if (screen == Screen.NewGroup) {
            _state.value = _state.value.copy(groupNameDraft = "", pickedMembers = emptySet())
        }
        if (screen == Screen.Status) loadStatusSnapshot()
    }

    /** @return true if the back event was consumed; false if the Activity should finish. */
    fun goBack(): Boolean {
        val s = _state.value
        return when (BackStack.decide(s)) {
            BackLayer.CloseImage -> {
                closeImage()
                true
            }
            BackLayer.DismissCall -> {
                if (s.call?.phase == CallPhase.RINGING_IN) rejectCall() else hangup()
                true
            }
            BackLayer.CancelRecording -> {
                finishVoice(false)
                true
            }
            BackLayer.ClearMessageQuery -> {
                setMessageQuery("")
                true
            }
            BackLayer.ClearChatQuery -> {
                setChatQuery("")
                true
            }
            BackLayer.CancelForward -> {
                cancelForward()
                true
            }
            BackLayer.CancelComposer -> {
                cancelComposerExtra()
                true
            }
            BackLayer.Pop -> {
                persistOpenDraft()
                val next = BackStack.pop(BackStack.currentStack(s.backStack, s.screen))
                _state.value = s.copy(
                    screen = next.last(),
                    backStack = next,
                    error = null,
                    viewingImage = null,
                    messageQuery = "",
                )
                if (NavRules.refreshesLists(next.last())) refreshConversations()
                true
            }
            BackLayer.CloseEmoji,
            BackLayer.CloseSearch,
            BackLayer.CloseDialog,
            BackLayer.Exit,
            -> false
        }
    }

    private fun applyNav(screen: Screen, mode: NavMode): UiState {
        val base = _state.value
        val stack = BackStack.apply(BackStack.currentStack(base.backStack, base.screen), screen, mode)
        return base.copy(screen = stack.last(), backStack = stack, viewingImage = null)
    }

    fun setDraft(text: String) {
        _state.value = _state.value.copy(draftText = text)
        persistOpenDraft()
        maybeSendTyping(text)
    }

    fun setChatQuery(query: String) {
        _state.value = _state.value.copy(chatQuery = query)
    }

    fun setMessageQuery(query: String) {
        _state.value = _state.value.copy(messageQuery = query)
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
                    _state.value = applyNav(Screen.Status, NavMode.Push).copy(
                        statusText = st?.toString(2) ?: "ядро сервера обновлено",
                        admin = st?.let { AdminSnapshot.from(it) } ?: _state.value.admin,
                        updateText = "Ядро на VPS обновлено, чаты и owner на месте.",
                        busy = false,
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
        persistOpenDraft()
        enterChat(device.deviceId, device, null)
    }

    fun openGroup(group: RopeGroup) {
        persistOpenDraft()
        enterChat(ChatIds.group(group.groupId), null, group)
    }

    fun openConversation(c: Conversation) {
        if (_state.value.forwarding != null) {
            completeForward(c)
            return
        }
        when {
            c.group != null -> openGroup(c.group)
            c.peer != null -> openChat(c.peer)
            !c.isGroup -> openChat(
                DirectoryDevice(c.id, "", c.title, ByteArray(0), "", c.online),
            )
            else -> _state.value = _state.value.copy(
                error = "Этой группы нет. Голосовые и фото вернулись в личный чат.",
            )
        }
    }

    fun toggleTheme() {
        val next = if (_state.value.theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        store.saveTheme(next)
        _state.value = _state.value.copy(theme = next)
    }

    fun sendDraft() {
        val text = _state.value.draftText
        if (text.isBlank() || _state.value.recording) return
        val edit = _state.value.editTarget
        if (edit != null) {
            _state.value = _state.value.copy(draftText = "", editTarget = null, replyTo = null)
            persistOpenDraft()
            applyEdit(edit, text)
            return
        }
        val reply = _state.value.replyTo
        _state.value = _state.value.copy(draftText = "", replyTo = null)
        persistOpenDraft()
        val group = _state.value.group
        if (group != null) {
            sendGroupText(group, text, reply)
            return
        }
        val peer = _state.value.peer ?: return
        scope.launch {
            val id = identity ?: return@launch
            try {
                val packed = TextBody.encode(text, reply?.id, reply?.preview().orEmpty(), replyName(reply))
                val env = id.encryptMessage(publicIdentityFromBlob(peer.publicIdentity), packed)
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
                    replyToId = reply?.id,
                    replyPreview = reply?.preview().orEmpty(),
                    replyName = replyName(reply),
                )
                store.insertMessage(local)
                refreshMessages(peer.deviceId)
                pushEnvelope(env.bytes)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun startReply(msg: ChatMessage) {
        if (msg.deleted) return
        _state.value = _state.value.copy(replyTo = msg, editTarget = null)
    }

    fun startEdit(msg: ChatMessage) {
        if (!msg.outgoing || msg.deleted) return
        if (msg.kind != MessageKind.TEXT && msg.kind != MessageKind.GROUP_TEXT) return
        _state.value = _state.value.copy(editTarget = msg, replyTo = null, draftText = msg.text)
    }

    fun cancelComposerExtra() {
        _state.value = _state.value.copy(replyTo = null, editTarget = null)
    }

    fun deleteMessage(msg: ChatMessage) {
        if (!msg.outgoing || msg.deleted) return
        store.markDeleted(msg.id)
        refreshOpenChat()
        sendControl(EnvelopeTypes.RECEIPT, ChatControl(ChatControl.DELETE, msg.id).toJson().toByteArray())
    }

    fun startForward(msg: ChatMessage) {
        if (msg.deleted) return
        val stack = BackStack.listForForward(BackStack.currentStack(_state.value.backStack, _state.value.screen))
        _state.value = _state.value.copy(
            forwarding = msg,
            screen = stack.last(),
            backStack = stack,
            replyTo = null,
            editTarget = null,
            viewingImage = null,
        )
    }

    fun cancelForward() {
        _state.value = _state.value.copy(forwarding = null)
    }

    fun togglePinChat(id: String) {
        val cur = store.chatPrefs(id)
        store.saveChatPrefs(id, cur.copy(pinned = !cur.pinned))
        refreshConversations()
    }

    fun toggleMuteChat(id: String) {
        val cur = store.chatPrefs(id)
        store.saveChatPrefs(id, cur.copy(muted = !cur.muted))
        refreshConversations()
    }

    fun copyMessage(msg: ChatMessage) {
        if (!msg.text.isNotBlank() || msg.deleted) return
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("rope", msg.text))
        _state.value = _state.value.copy(notice = "Скопировано")
    }

    fun togglePinMessage(msg: ChatMessage) {
        if (msg.deleted) return
        val chatId = openChatId() ?: msg.peerDeviceId
        val cur = store.chatPrefs(chatId)
        val nextId = if (cur.pinnedMessageId == msg.id) null else msg.id
        store.saveChatPrefs(chatId, cur.copy(pinnedMessageId = nextId))
        _state.value = _state.value.copy(pinnedMessageId = nextId)
        sendControl(
            EnvelopeTypes.RECEIPT,
            ChatControl(
                ChatControl.PIN,
                msg.id,
                op = if (nextId == null) ReactionPayload.CLEAR else ReactionPayload.SET,
            ).toJson().toByteArray(),
        )
    }

    fun jumpToMessage(id: String?) {
        if (id.isNullOrBlank()) return
        _state.value = _state.value.copy(scrollToMessageId = id)
    }

    fun consumeScrollTo() {
        _state.value = _state.value.copy(scrollToMessageId = null)
    }

    fun openImage(msg: ChatMessage) {
        if (msg.kind != MessageKind.IMAGE || msg.deleted) return
        _state.value = _state.value.copy(viewingImage = msg)
    }

    fun closeImage() {
        _state.value = _state.value.copy(viewingImage = null)
    }

    fun dismissNotice() {
        _state.value = _state.value.copy(notice = null)
    }

    fun completeForward(c: Conversation) {
        val src = _state.value.forwarding ?: return
        _state.value = _state.value.copy(forwarding = null)
        scope.launch {
            try {
                when {
                    c.group != null -> {
                        forwardToGroup(c.group, src)
                        openGroup(c.group)
                    }
                    c.peer != null -> {
                        forwardToPeer(c.peer, src)
                        openChat(c.peer)
                    }
                    else -> _state.value = _state.value.copy(error = "некуда переслать")
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun sendAttachment(uri: Uri, forcedMime: String? = null) {
        scope.launch {
            try {
                val cr = app.contentResolver
                var mime = forcedMime ?: cr.getType(uri) ?: "application/octet-stream"
                val name = attachmentName(uri, mime)
                var bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: error("не удалось прочитать файл")
                if (bytes.size > 25 * 1024 * 1024) {
                    _state.value = _state.value.copy(error = "Файл больше 25 МБ")
                    return@launch
                }
                if (mime.startsWith("image/") || looksLikeImage(name, mime)) {
                    val normalized = ImageCodec.normalizeForSend(bytes, if (mime.startsWith("image/")) mime else "image/jpeg")
                    bytes = normalized.first
                    mime = normalized.second
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
        val path = msg.localPath
        if (path.isNullOrBlank()) {
            retryMedia(msg)
            return
        }
        voicePlayer.toggle(msg.id, path)
        publishVoiceProgress()
        voiceProgressJob?.cancel()
        if (voicePlayer.playingId != null) {
            voiceProgressJob = scope.launch {
                while (voicePlayer.playingId != null) {
                    delay(80)
                    publishVoiceProgress()
                }
                publishVoiceProgress()
            }
        }
    }

    private fun publishVoiceProgress() {
        _state.value = _state.value.copy(
            playingVoiceId = voicePlayer.playingId,
            voiceProgressId = voicePlayer.activeId,
            voicePositionMs = voicePlayer.positionMs(),
            voiceDurationMs = voicePlayer.durationMs(),
        )
    }

    fun retryMedia(msg: ChatMessage) {
        ensureMedia(msg, force = true)
    }

    fun react(message: ChatMessage, emoji: String) {
        val id = identity ?: return
        val mine = id.deviceId()
        val name = _state.value.profile?.displayName.orEmpty()
        val already = message.reactions.any { it.deviceId == mine && it.emoji == emoji }
        val op = if (already) ReactionPayload.CLEAR else ReactionPayload.SET
        store.applyReaction(message.id, emoji, mine, name, already)
        refreshOpenChat()
        val payload = ReactionPayload(message.id, emoji, op)
        sendControl(EnvelopeTypes.RECEIPT, payload.toJson().toByteArray())
    }

    fun ensureMedia(msg: ChatMessage, force: Boolean = false) {
        if (msg.extra.isBlank()) return
        if (msg.kind != MessageKind.IMAGE && msg.kind != MessageKind.VOICE && msg.kind != MessageKind.FILE) return
        val path = msg.localPath
        if (!force && path != null && File(path).isFile && File(path).length() > 8) return
        if (!force && !mediaAttempts.add(msg.id)) return
        mediaAttempts.add(msg.id)
        scope.launch {
            downloadMedia(msg.id, MediaPayload.parse(msg.extra))
        }
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
            media = "WebRTC · соединяем",
            link = CallLinkState.RINGING,
        )
        _state.value = _state.value.copy(call = call)
        recordCall(peer.deviceId, "Исходящий звонок", outgoing = true)
        sendCall(call.callId, peer.deviceId, "ring", "")
        sendCallEnvelope(peer, call.callId, "ring")
        prefetchIce()
        startTone(true)
        audioMode(true)
    }

    fun acceptCall() {
        val call = _state.value.call ?: return
        _state.value = _state.value.copy(
            call = call.copy(
                phase = CallPhase.ACTIVE,
                media = CallMedia.label("CHECKING"),
                link = CallLinkState.CONNECTING,
            ),
        )
        sendCall(call.callId, call.peerDeviceId, "accept", "")
        _state.value.devices.find { it.deviceId == call.peerDeviceId }?.let {
            sendCallEnvelope(it, call.callId, "accept")
        }
        stopTone()
        audioMode(true)
        notifier.clearCall()
        scope.launch { startRtc(asCaller = false) }
    }

    fun rejectCall() {
        val call = _state.value.call ?: return
        sendCall(call.callId, call.peerDeviceId, "reject", "")
        _state.value.devices.find { it.deviceId == call.peerDeviceId }?.let {
            sendCallEnvelope(it, call.callId, "reject")
        }
        endCall()
    }

    fun hangup() {
        val call = _state.value.call ?: return
        sendCall(call.callId, call.peerDeviceId, "hangup", "")
        _state.value.devices.find { it.deviceId == call.peerDeviceId }?.let {
            sendCallEnvelope(it, call.callId, "hangup")
        }
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
                _state.value = applyNav(Screen.Invite, NavMode.Push).copy(inviteUrl = url, busy = false)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun refreshStatus() {
        if (_state.value.screen != Screen.Status) {
            _state.value = applyNav(Screen.Status, NavMode.Push).copy(error = null)
        }
        loadStatusSnapshot()
    }

    private fun loadStatusSnapshot() {
        scope.launch {
            val latest = runCatching { AppUpdater().latestApk(store.githubToken()) }.getOrNull()
            val newer = latest != null && AppRelease.isNewer(latest.version, BuildConfig.VERSION_NAME)
            val updateHint = when {
                latest == null -> "Не удалось проверить GitHub. Можно нажать «Обновить приложение» ещё раз."
                !newer -> "Уже стоит ${BuildConfig.VERSION_NAME}"
                else -> "Доступно приложение ${latest.version}. Поставится поверх, без удаления."
            }
            val st = runCatching { api?.status() }.getOrNull()
            val owner = RoleRules.isOwner(_state.value.profile?.role)
            _state.value = _state.value.copy(
                error = null,
                statusText = when {
                    st != null -> st.toString(2)
                    owner -> "ядро сейчас недоступно"
                    else -> "Вы гость. Приложение обновляется здесь, без прав owner."
                },
                admin = st?.let { AdminSnapshot.from(it) },
                updateText = updateHint,
                appUpdateAvailable = newer,
                latestAppVersion = latest?.version.orEmpty(),
            )
        }
    }

    fun upgradeCore(password: String, keyPem: String) {
        val profile = store.profile()
        if (profile == null) {
            _state.value = _state.value.copy(error = "сначала подключитесь к серверу")
            return
        }
        if (!RoleRules.canUpgradeCore(profile.role)) {
            _state.value = _state.value.copy(error = "Ядро на VPS обновляет только owner")
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
                    _state.value = applyNav(Screen.Status, NavMode.Push).copy(
                        busy = false,
                        updateText = "Уже стоит $local",
                        appUpdateAvailable = false,
                    )
                    return@launch
                }
                val dest = File(app.cacheDir, "updates/${latest.assetName}")
                dest.parentFile?.mkdirs()
                AppUpdater().download(latest, dest, store.githubToken())
                writeUpdateArtifacts(dest, latest.assetName)
                _state.value = applyNav(Screen.Status, NavMode.Push).copy(
                    busy = false,
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
        val group = _state.value.group
        val peer = _state.value.peer
        if (group == null && peer == null) {
            throw IllegalStateException("откройте чат, чтобы отправить вложение")
        }
        busy(true)
        try {
            val enc = encryptObject(bytes)
            val uploaded = api.uploadObject(enc.ciphertext, enc.sha256)
            val objectId = uploaded.getString("object_id")
            val inKnownGroup = group != null && _state.value.groups.any { it.groupId == group.groupId }
            val payload = MediaPayload(
                kind = kind,
                objectId = objectId,
                sha256 = enc.sha256,
                keyB64 = Base64.encodeToString(enc.key, Base64.NO_WRAP),
                mime = mime,
                name = name,
                size = bytes.size.toLong(),
                durationMs = durationMs,
                groupId = if (inKnownGroup) group?.groupId else null,
            )
            val cache = persistPlain(objectId, name, mime, bytes)
            if (inKnownGroup && group != null) {
                sendGroupPayload(group, EnvelopeTypes.MEDIA, payload.toJson().toByteArray(), payload.preview(), payload.messageKind(), payload.toJson(), cache)
                return
            }
            val dest = peer ?: throw IllegalStateException("откройте личный чат")
            val env = id.encryptTyped(publicIdentityFromBlob(dest.publicIdentity), EnvelopeTypes.MEDIA, payload.toJson().toByteArray())
            val local = ChatMessage(
                id = env.messageId,
                peerDeviceId = dest.deviceId,
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
            refreshMessages(dest.deviceId)
            pushEnvelope(env.bytes)
        } finally {
            _state.value = _state.value.copy(busy = false)
        }
    }

    private fun sendGroupText(group: RopeGroup, text: String, reply: ChatMessage? = null) {
        val body = GroupTextPayload(
            group.groupId,
            text,
            group.epoch,
            reply?.id,
            reply?.preview().orEmpty(),
            replyName(reply),
        ).toJson().toByteArray()
        sendGroupPayload(
            group,
            EnvelopeTypes.GROUP_TEXT,
            body,
            text,
            MessageKind.GROUP_TEXT,
            "",
            null,
            reply,
        )
    }

    private fun sendGroupPayload(
        group: RopeGroup,
        type: UByte,
        body: ByteArray,
        preview: String,
        kind: MessageKind,
        extra: String,
        localFile: File?,
        reply: ChatMessage? = null,
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
                    replyToId = reply?.id,
                    replyPreview = reply?.preview().orEmpty(),
                    replyName = replyName(reply),
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

    private fun persistPlain(objectId: String, name: String, mime: String, bytes: ByteArray): File {
        return ImageCodec.persist(File(app.filesDir, "media"), objectId, mime, name, bytes)
    }

    private fun prefetchMedia(messages: List<ChatMessage>) {
        messages.forEach { ensureMedia(it) }
    }

    private fun attachmentName(uri: Uri, mime: String): String {
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getString(0).orEmpty()
                if (n.isNotBlank()) return File(n).name
            }
        }
        val last = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        return if (last.contains('.')) File(last).name else "photo.${ImageCodec.extensionFor(mime, last)}"
    }

    private fun looksLikeImage(name: String, mime: String): Boolean {
        val n = name.lowercase()
        return mime.startsWith("image/") || n.endsWith(".jpg") || n.endsWith(".jpeg") ||
            n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".heic") || n.endsWith(".gif")
    }

    private fun sendControl(type: UByte, body: ByteArray) {
        val id = identity ?: return
        val group = _state.value.group
        if (group != null) {
            scope.launch {
                val devices = currentDevices()
                val envelopes = JSONArray()
                for (memberId in group.members.filter { it != id.deviceId() }) {
                    val peer = devices.find { it.deviceId == memberId } ?: continue
                    val env = runCatching {
                        id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), type, body)
                    }.getOrNull() ?: continue
                    envelopes.put(Base64.encodeToString(env.bytes, Base64.NO_WRAP))
                }
                if (envelopes.length() > 0) {
                    socket?.send(
                        JSONObject()
                            .put("type", "group_send")
                            .put("group_id", group.groupId)
                            .put("envelopes", envelopes)
                            .toString(),
                    )
                }
            }
            return
        }
        val peer = _state.value.peer ?: return
        if (peer.publicIdentity.isEmpty()) return
        scope.launch {
            try {
                val env = id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), type, body)
                pushEnvelope(env.bytes)
            } catch (_: Exception) {
            }
        }
    }

    private fun attached(profile: ServerProfile) {
        store.saveProfile(profile)
        api = ServerApi(profile, identity!!)
        val withIce = refreshIceServers(profile)
        store.saveProfile(withIce)
        api = ServerApi(withIce, identity!!)
        _state.value = applyNav(NavRules.signedInRoot, NavMode.Reset).copy(
            profile = withIce,
            busy = false,
            error = null,
            pendingInvite = null,
        )
        refreshDirectory()
        connectSocket(withIce)
        checkAppUpdate(openStatus = false)
    }

    private fun refreshIceServers(profile: ServerProfile): ServerProfile {
        val info = runCatching { api?.info() }.getOrNull() ?: return profile
        val ice = info.optJSONArray("ice_servers")?.toString().orEmpty()
        return profile.copy(iceServersJson = ice)
    }

    private fun prefetchIce() {
        scope.launch {
            val cur = store.profile() ?: return@launch
            val updated = refreshIceServers(cur)
            store.saveProfile(updated)
            _state.value = _state.value.copy(profile = updated)
        }
    }

    private fun checkAppUpdate(openStatus: Boolean) {
        scope.launch {
            val latest = runCatching { AppUpdater().latestApk(store.githubToken()) }.getOrNull() ?: return@launch
            val newer = AppRelease.isNewer(latest.version, BuildConfig.VERSION_NAME)
            val nav = if (openStatus) applyNav(Screen.Status, NavMode.Push) else _state.value
            _state.value = nav.copy(
                appUpdateAvailable = newer,
                latestAppVersion = latest.version,
                updateText = if (newer) {
                    "Доступно приложение ${latest.version}. Обновление не требует прав owner."
                } else {
                    _state.value.updateText
                },
            )
        }
    }

    private fun replyName(msg: ChatMessage?): String {
        if (msg == null) return ""
        return msg.senderName.ifBlank {
            if (msg.outgoing) _state.value.profile?.displayName.orEmpty() else "сообщение"
        }
    }

    private fun openChatId(): String? {
        _state.value.group?.let { return ChatIds.group(it.groupId) }
        return _state.value.peer?.deviceId
    }

    private fun enterChat(chatId: String, peer: DirectoryDevice?, group: RopeGroup?) {
        val prefs = store.chatPrefs(chatId)
        store.saveChatPrefs(chatId, prefs.copy(unread = 0, lastReadMs = System.currentTimeMillis()))
        _state.value = applyNav(Screen.Chat, NavMode.Push).copy(
            peer = peer,
            group = group,
            messages = store.messages(chatId),
            draftText = prefs.draft,
            replyTo = null,
            editTarget = null,
            messageQuery = "",
            pinnedMessageId = prefs.pinnedMessageId,
        )
        publishTyping()
        prefetchMedia(_state.value.messages)
        refreshConversations()
    }

    private fun persistOpenDraft() {
        val id = openChatId() ?: return
        val cur = store.chatPrefs(id)
        if (cur.draft == _state.value.draftText) return
        store.saveChatPrefs(id, cur.copy(draft = _state.value.draftText))
    }

    private fun maybeSendTyping(text: String) {
        val now = System.currentTimeMillis()
        if (!TypingRules.shouldSend(lastTypingSentAt, now, text)) return
        lastTypingSentAt = now
        val target = openChatId() ?: return
        sendControl(EnvelopeTypes.RECEIPT, ChatControl(ChatControl.TYPING, target).toJson().toByteArray())
    }

    private fun noteTyping(chatId: String, name: String) {
        typingUntil[chatId] = name.ifBlank { "печатает" } to System.currentTimeMillis() + TypingRules.TTL_MS
        publishTyping()
        if (typingJob?.isActive == true) return
        typingJob = scope.launch {
            while (typingUntil.isNotEmpty()) {
                delay(800)
                val now = System.currentTimeMillis()
                typingUntil.entries.removeAll { !TypingRules.isActive(it.value.second, now) }
                publishTyping()
            }
        }
    }

    private fun publishTyping() {
        val open = openChatId()
        val name = open?.let { typingUntil[it]?.first }
        if (_state.value.typingName != name) {
            _state.value = _state.value.copy(typingName = name)
        }
    }

    private fun applyEdit(msg: ChatMessage, text: String) {
        store.editMessage(msg.id, text)
        refreshOpenChat()
        sendControl(
            EnvelopeTypes.RECEIPT,
            ChatControl(ChatControl.EDIT, msg.id, text = text).toJson().toByteArray(),
        )
    }

    private fun forwardToPeer(peer: DirectoryDevice, src: ChatMessage) {
        val id = identity ?: return
        if (src.kind == MessageKind.TEXT || src.kind == MessageKind.GROUP_TEXT) {
            val packed = TextBody.encode(src.text, src.id, src.preview(), "Переслано · ${replyName(src)}")
            val env = id.encryptMessage(publicIdentityFromBlob(peer.publicIdentity), packed)
            store.insertMessage(
                ChatMessage(
                    id = env.messageId,
                    peerDeviceId = peer.deviceId,
                    outgoing = true,
                    text = src.text,
                    status = MessageStatus.CREATED,
                    timestampMs = env.timestampMs.toLong(),
                    envelope = env.bytes,
                    kind = MessageKind.TEXT,
                    senderId = id.deviceId(),
                    senderName = _state.value.profile?.displayName.orEmpty(),
                    replyToId = src.id,
                    replyPreview = src.preview(),
                    replyName = "Переслано · ${replyName(src)}",
                ),
            )
            refreshMessages(peer.deviceId)
            pushEnvelope(env.bytes)
            return
        }
        if (src.extra.isBlank()) {
            _state.value = _state.value.copy(error = "это вложение уже нельзя переслать")
            return
        }
        val env = id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), EnvelopeTypes.MEDIA, src.extra.toByteArray())
        store.insertMessage(
            src.copy(
                id = env.messageId,
                peerDeviceId = peer.deviceId,
                outgoing = true,
                status = MessageStatus.CREATED,
                timestampMs = env.timestampMs.toLong(),
                envelope = env.bytes,
                senderId = id.deviceId(),
                senderName = _state.value.profile?.displayName.orEmpty(),
                replyToId = src.id,
                replyPreview = src.preview(),
                replyName = "Переслано · ${replyName(src)}",
                reactions = emptyList(),
            ),
        )
        refreshMessages(peer.deviceId)
        pushEnvelope(env.bytes)
    }

    private fun forwardToGroup(group: RopeGroup, src: ChatMessage) {
        if (src.kind == MessageKind.TEXT || src.kind == MessageKind.GROUP_TEXT) {
            sendGroupText(group, src.text, src.copy(senderName = "Переслано · ${replyName(src)}"))
            return
        }
        if (src.extra.isBlank()) {
            _state.value = _state.value.copy(error = "это вложение уже нельзя переслать")
            return
        }
        sendGroupPayload(
            group,
            EnvelopeTypes.MEDIA,
            src.extra.toByteArray(),
            src.preview(),
            src.kind,
            src.extra,
            src.localPath?.let { File(it) },
            src,
        )
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
                store.rehomeMisroutedMedia()
                val peer = _state.value.peer?.let { cur -> devices.find { it.deviceId == cur.deviceId } ?: cur }
                val group = _state.value.group?.let { cur -> groups.find { it.groupId == cur.groupId } ?: cur }
                _state.value = _state.value.copy(devices = devices, groups = groups, peer = peer, group = group)
                refreshConversations()
                refreshOpenChat()
            } catch (e: Exception) {
                _state.value = _state.value.copy(offline = true, error = e.message)
            }
        }
    }

    private fun refreshConversations() {
        val devices = _state.value.devices
        val groups = _state.value.groups
        val lastBy = store.conversations().associate { it.first to it.second }
        val prefs = store.allChatPrefs()
        val dms = devices.map { d ->
            val last = lastBy[d.deviceId]
            val p = prefs[d.deviceId] ?: ChatPrefs()
            Conversation(
                id = d.deviceId,
                title = d.displayName.ifBlank { d.deviceId.take(8) },
                subtitle = last?.preview() ?: if (d.online) "в сети" else "не в сети",
                isGroup = false,
                online = d.online,
                last = last,
                peer = d,
                pinned = p.pinned,
                muted = p.muted,
                unread = p.unread,
            )
        }
        val gs = groups.map { g ->
            val id = ChatIds.group(g.groupId)
            val last = lastBy[id]
            val p = prefs[id] ?: ChatPrefs()
            Conversation(
                id = id,
                title = g.name,
                subtitle = last?.preview() ?: "${g.members.size} участников",
                isGroup = true,
                online = g.members.any { it in _state.value.onlineIds && it != identity?.deviceId() },
                last = last,
                group = g,
                pinned = p.pinned,
                muted = p.muted,
                unread = p.unread,
            )
        }
        val leftover = lastBy.keys
            .filter { id ->
                ChatRouting.showLeftoverThread(id) &&
                    dms.none { it.id == id } &&
                    gs.none { it.id == id }
            }
            .map { id ->
                val p = prefs[id] ?: ChatPrefs()
                Conversation(
                    id = id,
                    title = id.take(8),
                    subtitle = lastBy[id]?.preview().orEmpty(),
                    isGroup = false,
                    online = id in _state.value.onlineIds,
                    last = lastBy[id],
                    peer = devices.find { it.deviceId == id },
                    pinned = p.pinned,
                    muted = p.muted,
                    unread = p.unread,
                )
            }
        _state.value = _state.value.copy(
            conversations = (dms + gs + leftover).sortedWith { a, b -> ChatListRules.compare(a, b) },
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
                val (body, replyId, replyPair) = TextBody.decode(plain.text)
                val msg = ChatMessage(
                    id = plain.messageId,
                    peerDeviceId = sender.deviceId,
                    outgoing = false,
                    text = body,
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = plain.timestampMs.toLong(),
                    kind = MessageKind.TEXT,
                    senderId = sender.deviceId,
                    senderName = sender.displayName,
                    replyToId = replyId,
                    replyPreview = replyPair.first,
                    replyName = replyPair.second,
                )
                store.insertMessage(msg)
                ack(plain.messageId)
                notifyIfHidden(sender.displayName, body, sender.deviceId)
            }
            EnvelopeTypes.GROUP_TEXT -> {
                val typed = id.decryptTyped(publicIdentityFromBlob(sender.publicIdentity), env)
                val payload = GroupTextPayload.parse(String(typed.body))
                val gid = JsonIds.optional(payload.groupId)
                if (gid == null) {
                    ack(typed.messageId)
                    return
                }
                val chatId = ChatIds.group(gid)
                val msg = ChatMessage(
                    id = typed.messageId,
                    peerDeviceId = chatId,
                    outgoing = false,
                    text = payload.text,
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = typed.timestampMs.toLong(),
                    kind = MessageKind.GROUP_TEXT,
                    groupId = gid,
                    senderId = sender.deviceId,
                    senderName = sender.displayName,
                    replyToId = payload.replyTo,
                    replyPreview = payload.replyPreview,
                    replyName = payload.replyName,
                )
                store.insertMessage(msg)
                ack(typed.messageId)
                notifyIfHidden(sender.displayName, payload.text, chatId)
            }
            EnvelopeTypes.MEDIA -> {
                val typed = id.decryptTyped(publicIdentityFromBlob(sender.publicIdentity), env)
                val payload = MediaPayload.parse(String(typed.body))
                val known = _state.value.groups.map { it.groupId }.toSet()
                val chatId = ChatRouting.mediaChatId(payload.groupId, sender.deviceId, known)
                val routedGroup = JsonIds.optional(payload.groupId)?.takeIf { it in known }
                val msg = ChatMessage(
                    id = typed.messageId,
                    peerDeviceId = chatId,
                    outgoing = false,
                    text = payload.preview(),
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = typed.timestampMs.toLong(),
                    kind = payload.messageKind(),
                    extra = payload.toJson(),
                    groupId = routedGroup,
                    senderId = sender.deviceId,
                    senderName = sender.displayName,
                )
                store.insertMessage(msg)
                ack(typed.messageId)
                notifyIfHidden(sender.displayName, payload.preview(), chatId)
                scope.launch { downloadMedia(msg.id, payload) }
            }
            EnvelopeTypes.RECEIPT -> {
                val typed = id.decryptTyped(publicIdentityFromBlob(sender.publicIdentity), env)
                val raw = String(typed.body)
                val control = ChatControl.parse(raw)
                val reaction = ReactionPayload.parse(raw)
                when {
                    control?.kind == ChatControl.EDIT -> store.editMessage(control.targetId, control.text)
                    control?.kind == ChatControl.DELETE -> store.markDeleted(control.targetId)
                    control?.kind == ChatControl.TYPING -> {
                        val chatId = if (ChatIds.isGroup(control.targetId)) control.targetId else sender.deviceId
                        noteTyping(chatId, sender.displayName)
                    }
                    control?.kind == ChatControl.PIN -> {
                        val chatId = store.message(control.targetId)?.peerDeviceId
                            ?: if (ChatIds.isGroup(control.targetId)) control.targetId else sender.deviceId
                        val cur = store.chatPrefs(chatId)
                        val next = if (control.op == ReactionPayload.CLEAR) null else control.targetId
                        store.saveChatPrefs(chatId, cur.copy(pinnedMessageId = next))
                        if (openChatId() == chatId) {
                            _state.value = _state.value.copy(pinnedMessageId = next)
                        }
                    }
                    reaction != null -> store.applyReaction(
                        reaction.targetId,
                        reaction.emoji,
                        sender.deviceId,
                        sender.displayName,
                        reaction.op == ReactionPayload.CLEAR,
                    )
                }
                refreshOpenChat()
                ack(typed.messageId)
            }
            EnvelopeTypes.CALL -> {
                val typed = id.decryptTyped(publicIdentityFromBlob(sender.publicIdentity), env)
                val body = JSONObject(String(typed.body))
                handleCallEvent(
                    JSONObject()
                        .put("from", sender.deviceId)
                        .put("event", body.optString("event"))
                        .put("call_id", body.optString("call_id"))
                        .put("payload", body.optString("payload")),
                )
                ack(typed.messageId)
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
            val dest = persistPlain(payload.objectId, payload.name, payload.mime, plain)
            store.updateLocalPath(messageId, dest.absolutePath)
            _state.value = _state.value.copy(updateText = "")
            refreshOpenChat()
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "не скачалось вложение: ${e.message}")
        }
    }

    private fun handleCallEvent(obj: JSONObject) {
        val from = JsonIds.optional(obj.optString("from")).orEmpty()
        val event = JsonIds.optional(obj.optString("event")).orEmpty()
        val callId = JsonIds.optional(obj.optString("call_id")).orEmpty()
        if (from.isBlank() || callId.isBlank()) return
        val name = _state.value.devices.find { it.deviceId == from }?.displayName ?: from.take(8)
        val current = _state.value.call
        when (event) {
            "ring" -> {
                if (current?.callId == callId) return
                if (current != null && current.outgoing && current.peerDeviceId == from &&
                    current.phase == CallPhase.RINGING_OUT
                ) {
                    val me = identity?.deviceId().orEmpty()
                    val iOffer = CallLink.weCreateOffer(me, from)
                    _state.value = _state.value.copy(
                        call = current.copy(
                            phase = CallPhase.ACTIVE,
                            media = CallMedia.label("CHECKING"),
                            link = CallLinkState.CONNECTING,
                        ),
                    )
                    stopTone()
                    scope.launch { startRtc(asCaller = iOffer) }
                    return
                }
                if (current != null) return
                _state.value = _state.value.copy(
                    call = CallInfo(
                        callId, from, name, outgoing = false, phase = CallPhase.RINGING_IN,
                        payload = obj.optString("payload"),
                        media = "WebRTC · соединяем",
                        link = CallLinkState.RINGING,
                    ),
                )
                recordCall(from, "Входящий звонок", outgoing = false)
                notifier.incomingCall(name)
                prefetchIce()
                startTone(false)
                audioMode(true)
            }
            "accept" -> {
                val cur = current ?: return
                if (cur.callId != callId) return
                _state.value = _state.value.copy(
                    call = cur.copy(
                        phase = CallPhase.ACTIVE,
                        media = CallMedia.label("CHECKING"),
                        link = CallLinkState.CONNECTING,
                    ),
                )
                if (cur.outgoing) scope.launch { startRtc(asCaller = true) }
                stopTone()
            }
            "reject", "hangup" -> {
                if (current == null || current.callId == callId || current.peerDeviceId == from) endCall()
            }
            in CallSignal.EVENTS -> {
                if (current != null && current.callId != callId) return
                val sig = CallSignal.parsePayload(obj.opt("payload")) ?: return
                val session = rtc
                if (session == null) queuedSignals += sig else session.handleRemote(sig)
            }
        }
    }

    private suspend fun startRtc(asCaller: Boolean) {
        val ice = awaitIce()
        val hasTurn = !CallLink.missingTurn(ice)
        val cur = _state.value.call ?: return
        val media = if (hasTurn) cur.media.ifBlank { CallMedia.label("CHECKING") } else CallLink.missingTurnDetail()
        _state.value = _state.value.copy(call = cur.copy(hasTurn = hasTurn, media = media, link = CallLinkState.CONNECTING))
        attachRtc(asCaller, ice)
        watchConnecting()
    }

    private suspend fun awaitIce(): List<IceServerSpec> {
        val cur = store.profile() ?: _state.value.profile
        val updated = withContext(Dispatchers.IO) {
            cur?.let { refreshIceServers(it) } ?: cur
        }
        if (updated != null) {
            store.saveProfile(updated)
            _state.value = _state.value.copy(profile = updated)
        }
        val parsed = IceServers.parse(updated?.iceServersJson)
        return IceServers.resolve(parsed, updated?.host)
    }

    private fun attachRtc(asCaller: Boolean, ice: List<IceServerSpec>) {
        synchronized(rtcLock) {
            if (rtc != null) {
                if (asCaller) return
                queuedSignals.toList().also { queuedSignals.clear() }.forEach { rtc?.handleRemote(it) }
                return
            }
            val profile = store.profile() ?: _state.value.profile
            val session = try {
                WebRtcSession(
                    app,
                    iceServers = ice,
                    pinnedFingerprint = profile?.fingerprint.orEmpty(),
                    hintHost = profile?.host,
                    onLocalSignal = { sig ->
                        val call = _state.value.call ?: return@WebRtcSession
                        val peer = _state.value.devices.find { it.deviceId == call.peerDeviceId }
                            ?: _state.value.peer
                            ?: return@WebRtcSession
                        sendCall(call.callId, peer.deviceId, sig.kind, sig.toJson())
                        sendCallEnvelope(peer, call.callId, sig.kind, sig.toJson())
                    },
                    onIce = { name, viaRelay ->
                        applyIceState(name, viaRelay)
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "WebRTC: ${e.message}")
                return
            }
            rtc = session
            if (asCaller) session.createOffer() else session.prepareCallee()
            queuedSignals.toList().also { queuedSignals.clear() }.forEach { session.handleRemote(it) }
        }
    }

    private fun applyIceState(name: String, viaRelay: Boolean) {
        val cur = _state.value.call ?: return
        val (link, label) = CallLink.applyIce(name, viaRelay, cur.hasTurn)
        _state.value = _state.value.copy(call = cur.copy(media = label, link = link))
        if (link == CallLinkState.CONNECTED) {
            connectWatch?.cancel()
            connectWatch = null
        }
        if (link == CallLinkState.FAILED && !iceRestarted && cur.hasTurn) {
            iceRestarted = true
            rtc?.restartIce()
            watchConnecting()
        }
    }

    private fun watchConnecting() {
        connectWatch?.cancel()
        val startedAt = System.currentTimeMillis()
        connectWatch = scope.launch {
            delay(CallLink.CONNECT_TIMEOUT_MS)
            val call = _state.value.call ?: return@launch
            if (call.phase != CallPhase.ACTIVE) return@launch
            if (!CallLink.timedOut(System.currentTimeMillis() - startedAt, call.link)) return@launch
            if (!iceRestarted && call.hasTurn) {
                iceRestarted = true
                rtc?.restartIce()
                delay(CallLink.CONNECT_TIMEOUT_MS)
                val again = _state.value.call ?: return@launch
                if (again.link == CallLinkState.CONNECTED || again.phase != CallPhase.ACTIVE) return@launch
                failConnecting(CallLink.timeoutDetail(again.hasTurn))
                return@launch
            }
            failConnecting(CallLink.timeoutDetail(call.hasTurn))
        }
    }

    private fun failConnecting(detail: String) {
        val cur = _state.value.call ?: return
        if (cur.link == CallLinkState.CONNECTED) return
        _state.value = _state.value.copy(
            call = cur.copy(link = CallLinkState.FAILED, media = detail),
            error = detail,
        )
    }

    private fun sendCallEnvelope(peer: DirectoryDevice, callId: String, event: String, payload: String = "") {
        val id = identity ?: return
        if (peer.publicIdentity.isEmpty()) return
        scope.launch {
            try {
                val body = JSONObject()
                    .put("call_id", callId)
                    .put("event", event)
                    .put("payload", payload)
                    .toString()
                    .toByteArray()
                val env = id.encryptTyped(publicIdentityFromBlob(peer.publicIdentity), EnvelopeTypes.CALL, body)
                pushEnvelope(env.bytes)
            } catch (_: Exception) {
            }
        }
    }

    private fun recordCall(peerId: String, text: String, outgoing: Boolean) {
        val id = identity ?: return
        store.insertMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                peerDeviceId = peerId,
                outgoing = outgoing,
                text = text,
                status = MessageStatus.DELIVERED_TO_DEVICE,
                timestampMs = System.currentTimeMillis(),
                kind = MessageKind.CALL,
                senderId = if (outgoing) id.deviceId() else peerId,
                senderName = if (outgoing) _state.value.profile?.displayName.orEmpty()
                else _state.value.devices.find { it.deviceId == peerId }?.displayName.orEmpty(),
            ),
        )
        refreshMessages(peerId)
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
        connectWatch?.cancel()
        connectWatch = null
        iceRestarted = false
        stopTone()
        audioMode(false)
        notifier.clearCall()
        try {
            rtc?.close()
        } catch (_: Exception) {
        }
        rtc = null
        queuedSignals.clear()
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
        val open = _state.value.screen == Screen.Chat && openChatId() == chatId
        if (open) return
        val cur = store.chatPrefs(chatId)
        store.saveChatPrefs(chatId, cur.copy(unread = cur.unread + 1))
        refreshConversations()
        if (!cur.muted) notifier.message(title, body)
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
