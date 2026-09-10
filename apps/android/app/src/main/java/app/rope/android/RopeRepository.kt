package app.rope.android

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import android.view.SurfaceHolder
import android.webkit.MimeTypeMap
import app.rope.android.data.AdminSnapshot
import app.rope.android.data.AlbumRules
import app.rope.android.data.CallInfo
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMedia
import app.rope.android.data.CallPhase
import app.rope.android.data.CallEffect
import app.rope.android.data.CallMachine
import app.rope.android.data.CallSignal
import app.rope.android.data.CallToneRules
import app.rope.android.data.IceServerSpec
import app.rope.android.data.ChatControlRules
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.EnvelopeTypes
import app.rope.android.data.ForwardRules
import app.rope.android.data.GroupChatUx
import app.rope.android.data.GroupTextPayload
import app.rope.android.data.IdentityVault
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MediaSendRules
import app.rope.android.data.MessageKind
import app.rope.android.data.ReactionPayload
import app.rope.android.data.ChatControl
import app.rope.android.data.ChatListPreviewRules
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatPrefs
import app.rope.android.data.ArchiveRules
import app.rope.android.data.RevokeRules
import app.rope.android.data.RoleRules
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.QuoteSpan
import app.rope.android.data.QuoteSpanRules
import app.rope.android.data.TextBody
import app.rope.android.data.TypingRules
import app.rope.android.data.UnreadSeparatorRules
import app.rope.android.data.VideoCallRules
import app.rope.android.data.VideoRules
import app.rope.android.data.MessageStatus
import app.rope.android.data.RopeGroup
import app.rope.android.data.ServerProfile
import app.rope.android.data.SshTarget
import app.rope.android.data.ThemeMode
import app.rope.android.data.ChatRouting
import app.rope.android.data.JsonIds
import app.rope.android.data.LinkPreviewRules
import app.rope.android.data.NotifyRules
import app.rope.android.data.NotifySoundRules
import app.rope.android.data.PackedLinkPreview
import app.rope.android.data.PeerIds
import app.rope.android.data.IceServers
import app.rope.android.data.UserFacing
import app.rope.android.data.VideoNoteRules
import app.rope.android.data.VoicePlayback
import app.rope.android.media.CallAudio
import app.rope.android.media.ImageCodec
import app.rope.android.media.VideoCodec
import app.rope.android.media.VideoNoteRecorder
import app.rope.android.media.VoicePlayer
import app.rope.android.media.VoiceRecorder
import app.rope.android.media.VoiceWaveform
import app.rope.android.media.WebRtcSession
import app.rope.android.media.WssAudioSession
import org.webrtc.VideoSink
import app.rope.android.net.LinkUnfurl
import app.rope.android.net.ServerApi
import app.rope.android.notify.RopeConnectionService
import app.rope.android.notify.RopeNotifier
import app.rope.android.protocol.InviteCodec
import app.rope.android.protocol.InviteLink as ParsedInvite
import app.rope.android.provision.ProvisionForm
import app.rope.android.provision.ServerTarget
import app.rope.android.provision.SshProvisioner
import app.rope.android.update.DeviceBackup
import app.rope.android.update.PublicBackupRules
import app.rope.android.update.PublicDownloads
import app.rope.android.update.AppRelease
import app.rope.android.update.AppUpdater
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
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
    val recordingVideoNote: Boolean = false,
    val recordMs: Long = 0,
    val playingVoiceId: String? = null,
    val voiceProgressId: String? = null,
    val voicePositionMs: Long = 0,
    val voiceDurationMs: Long = 0,
    val voiceSpeed: Float = VoicePlayback.SPEED_1X,
    val call: CallInfo? = null,
    val callMicMuted: Boolean = false,
    val callSpeakerOn: Boolean = false,
    val callCamMuted: Boolean = false,
    val callLocalMirrored: Boolean = true,
    val callNotice: String? = null,
    val callRtcReady: Boolean = false,
    val groupNameDraft: String = "",
    val pickedMembers: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.DARK,
    val notificationsMuted: Boolean = false,
    val notifySound: String = NotifySoundRules.DEFAULT,
    val linkPreviewsEnabled: Boolean = true,
    val composerPreview: PackedLinkPreview? = null,
    val composerPreviewDismissedUrl: String? = null,
    val appUpdateAvailable: Boolean = false,
    val latestAppVersion: String = "",
    val replyTo: ChatMessage? = null,
    val replySpan: QuoteSpan? = null,
    val editTarget: ChatMessage? = null,
    val forwarding: ChatMessage? = null,
    val chatQuery: String = "",
    val messageQuery: String = "",
    val typingName: String? = null,
    val viewingImage: ChatMessage? = null,
    val scrollToMessageId: String? = null,
    val notice: String? = null,
    val pinnedMessageId: String? = null,
    val unreadAnchorId: String? = null,
    val sessionReady: Boolean = false,
    val pendingAttachments: List<Uri> = emptyList(),
)

enum class Screen { Start, Provision, Join, Home, Chats, Chat, Groups, Calls, People, Invite, Status, Settings, NewGroup, GroupInfo, PeerProfile, Archive }

private data class ReplyPack(
    val id: String? = null,
    val preview: String = "",
    val name: String = "",
    val quoteText: String = "",
    val quoteStart: Int = -1,
    val quoteEnd: Int = -1,
)

class RopeRepository(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = LocalStore(app)
    private val vault = IdentityVault(app)
    private val notifier = RopeNotifier(app)
    private val voiceRecorder = VoiceRecorder(app)
    private val voicePlayer = VoicePlayer()
    private val videoNoteRecorder = VideoNoteRecorder(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var identity: DeviceIdentity? = null
    private var api: ServerApi? = null
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var recordJob: Job? = null
    private var unfurlJob: Job? = null
    @Volatile private var unfurlResult: PackedLinkPreview? = null
    private var voiceProgressJob: Job? = null
    private var reconnectAttempt = 0
    private var tone: ToneGenerator? = null
    private val mediaAttempts = mutableSetOf<String>()
    private var lastTypingSentAt = 0L
    private val typingUntil = mutableMapOf<String, MutableMap<String, Pair<String, Long>>>()
    private var typingJob: Job? = null
    private var rtc: WebRtcSession? = null
    private var wssAudio: WssAudioSession? = null
    private val wssFrameBusy = AtomicBoolean(false)
    private val sessionStarted = AtomicBoolean(false)
    private val callMachine = CallMachine()
    private var callPeerName: String = ""
    private var connectWatch: Job? = null
    private var ringWatch: Job? = null
    private var oneWayWatch: Job? = null
    private var rtcAsCaller = false
    private val rtcLock = Any()
    private var boundRemote: VideoSink? = null
    private var boundLocal: VideoSink? = null
    private var iceCachedAtMs: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var toneOutgoing: Boolean? = null

    fun start(pendingLink: String?) {
        if (!sessionStarted.compareAndSet(false, true)) {
            if (!pendingLink.isNullOrBlank() && store.profile() == null) {
                prepareJoin(pendingLink)
            }
            return
        }
        scope.launch {
            try {
                val night = app.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                _state.value = _state.value.copy(
                    theme = store.themeMode(night),
                    notificationsMuted = store.notificationsMuted(),
                    notifySound = store.notifySound(),
                    linkPreviewsEnabled = store.linkPreviewsEnabled(),
                )
                store.rehomeMisroutedMedia()
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
        if (screen == Screen.Invite && !RoleRules.canInvite(_state.value.profile?.role)) {
            return
        }
        val from = _state.value.screen
        if (screen != Screen.Chat) persistOpenDraft()
        var next = applyNav(screen, if (tab) NavMode.SwitchTab else NavMode.Push).copy(error = null)
        if (NavRules.clearsChatQuery(from, screen)) {
            next = next.copy(chatQuery = "")
        }
        _state.value = next
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
                if (s.recordingVideoNote) finishVideoNote(false) else finishVoice(false)
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
            BackLayer.CancelPendingMedia -> {
                cancelPendingMedia()
                true
            }
            BackLayer.Pop -> {
                persistOpenDraft()
                val next = BackStack.pop(BackStack.currentStack(s.backStack, s.screen))
                val dest = next.last()
                _state.value = s.copy(
                    screen = dest,
                    backStack = next,
                    error = null,
                    viewingImage = null,
                    messageQuery = "",
                    chatQuery = NavRules.chatQueryAfterPop(s.screen, dest, s.chatQuery),
                    unreadAnchorId = if (next.last() == Screen.Chat || next.last() == Screen.PeerProfile) {
                        s.unreadAnchorId
                    } else {
                        null
                    },
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
        val keepUnread = stack.last() == Screen.Chat || stack.last() == Screen.PeerProfile
        return base.copy(
            screen = stack.last(),
            backStack = stack,
            viewingImage = null,
            unreadAnchorId = if (keepUnread) base.unreadAnchorId else null,
        )
    }

    fun setDraft(text: String) {
        _state.value = _state.value.copy(draftText = text)
        persistOpenDraft()
        maybeSendTyping(text)
        scheduleUnfurl(text)
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
                notice(UserFacing.LOGIN)
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
                notice(UserFacing.LOGIN)
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
        if (!JoinDebugRules.showHttpJoin(BuildConfig.DEBUG)) return
        scope.launch {
            if (!LoginRules.isValid(displayName)) {
                notice(UserFacing.LOGIN)
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

    fun openSaved() {
        persistOpenDraft()
        enterChat(SavedMessagesRules.ID, SavedMessagesRules.stubPeer(), null)
    }

    fun openConversation(c: Conversation) {
        if (_state.value.forwarding != null) {
            completeForward(c)
            return
        }
        when {
            SavedMessagesRules.isSaved(c.id) -> openSaved()
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
        setTheme(next)
    }

    fun setTheme(mode: ThemeMode) {
        if (_state.value.theme == mode) return
        store.saveTheme(mode)
        _state.value = _state.value.copy(theme = mode)
    }

    fun toggleNotificationsMuted() {
        val next = !_state.value.notificationsMuted
        store.saveNotificationsMuted(next)
        _state.value = _state.value.copy(notificationsMuted = next)
    }

    fun setNotifySound(id: String) {
        val next = NotifySoundRules.normalize(id)
        if (next == _state.value.notifySound) return
        store.saveNotifySound(next)
        _state.value = _state.value.copy(notifySound = next)
    }

    fun toggleLinkPreviews() {
        val next = !_state.value.linkPreviewsEnabled
        store.saveLinkPreviews(next)
        _state.value = _state.value.copy(linkPreviewsEnabled = next)
        if (!next) {
            unfurlJob?.cancel()
            unfurlResult = null
            _state.value = _state.value.copy(composerPreview = null)
        } else {
            scheduleUnfurl(_state.value.draftText)
        }
    }

    fun dismissComposerPreview() {
        val url = _state.value.composerPreview?.url
            ?: LinkPreviewRules.firstHttps(_state.value.draftText)
        unfurlJob?.cancel()
        unfurlResult = null
        _state.value = _state.value.copy(
            composerPreview = null,
            composerPreviewDismissedUrl = url,
        )
    }

    fun copyText(value: String) {
        if (value.isBlank()) return
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("rope", value))
        _state.value = _state.value.copy(notice = "Скопировано")
    }

    fun sendDraft() {
        val edit = _state.value.editTarget
        if (edit != null && MediaSendRules.preferEditOverPending(true) && !_state.value.recording) {
            val text = _state.value.draftText
            if (text.isBlank()) return
            _state.value = _state.value.copy(draftText = "", editTarget = null, replyTo = null, replySpan = null, composerPreview = null, composerPreviewDismissedUrl = null)
            persistOpenDraft()
            applyEdit(edit, text)
            return
        }
        val pending = _state.value.pendingAttachments
        if (pending.isNotEmpty() && !_state.value.recording) {
            val caption = _state.value.draftText
            val pack = replyPack(_state.value.replyTo)
            val destPeer = _state.value.peer
            val destGroup = _state.value.group
            _state.value = _state.value.copy(
                draftText = "",
                pendingAttachments = emptyList(),
                replyTo = null,
                replySpan = null,
                composerPreview = null,
                composerPreviewDismissedUrl = null,
            )
            persistOpenDraft()
            sendAttachments(pending, caption = caption, pack = pack, destPeer = destPeer, destGroup = destGroup)
            return
        }
        val text = _state.value.draftText
        if (text.isBlank() || _state.value.recording) return
        val reply = _state.value.replyTo
        val pack = replyPack(reply)
        val attached = _state.value.composerPreview
        val dismissed = _state.value.composerPreviewDismissedUrl
        val pendingJob = unfurlJob
        _state.value = _state.value.copy(
            draftText = "",
            replyTo = null,
            replySpan = null,
            composerPreview = null,
            composerPreviewDismissedUrl = null,
        )
        persistOpenDraft()
        val group = _state.value.group
        val peer = _state.value.peer
        val saved = SavedMessagesRules.isSaved(openChatId()) || SavedMessagesRules.isSaved(peer?.deviceId)
        scope.launch {
            val preview = resolvePreviewForSend(text, attached, dismissed, pendingJob, upload = !saved)
            val lp = if (saved) preview?.withoutImage()?.copy(localPath = preview.localPath) else preview
            when {
                group != null -> sendGroupText(group, text, reply, pack, linkPreview = lp)
                saved -> saveLocalText(text, reply, pack = pack, linkPreview = lp)
                else -> {
                    val dest = peer ?: return@launch
                    sendPeerText(dest, text, pack, lp)
                }
            }
        }
    }

    fun startReply(msg: ChatMessage, span: QuoteSpan? = null) {
        if (msg.deleted) return
        _state.value = _state.value.copy(
            replyTo = msg,
            editTarget = null,
            replySpan = QuoteSpanRules.packed(msg.preview(), span)?.span(),
        )
    }

    fun setReplySpan(span: QuoteSpan?) {
        val reply = _state.value.replyTo ?: return
        _state.value = _state.value.copy(
            replySpan = QuoteSpanRules.packed(reply.preview(), span)?.span(),
        )
    }

    fun startEdit(msg: ChatMessage) {
        if (!msg.outgoing || msg.deleted) return
        if (msg.kind != MessageKind.TEXT && msg.kind != MessageKind.GROUP_TEXT) return
        _state.value = _state.value.copy(
            editTarget = msg,
            replyTo = null,
            replySpan = null,
            draftText = msg.text,
            pendingAttachments = emptyList(),
        )
    }

    fun cancelComposerExtra() {
        _state.value = _state.value.copy(
            replyTo = null,
            replySpan = null,
            editTarget = null,
        )
    }

    fun cancelPendingMedia() {
        _state.value = _state.value.copy(pendingAttachments = emptyList())
    }

    fun deleteMessage(msg: ChatMessage) {
        if (!msg.outgoing || msg.deleted) return
        store.markDeleted(msg.id)
        refreshOpenChat()
        if (SavedMessagesRules.skipNetwork(openChatId() ?: msg.peerDeviceId)) return
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
            replySpan = null,
            editTarget = null,
            viewingImage = null,
        )
    }

    fun cancelForward() {
        _state.value = _state.value.copy(forwarding = null)
    }

    fun togglePinChat(id: String) {
        val cur = store.chatPrefs(id)
        if (!ArchiveRules.canPin(cur)) return
        store.saveChatPrefs(id, cur.copy(pinned = !cur.pinned))
        refreshConversations()
    }

    fun archiveChat(id: String) {
        if (!ArchiveRules.canArchive(id)) return
        val cur = store.chatPrefs(id)
        if (cur.archived) return
        store.saveChatPrefs(id, ArchiveRules.archivePrefs(cur))
        refreshConversations()
    }

    fun unarchiveChat(id: String) {
        val cur = store.chatPrefs(id)
        if (!cur.archived) return
        store.saveChatPrefs(id, ArchiveRules.unarchivePrefs(cur))
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
        if ((msg.kind != MessageKind.IMAGE && msg.kind != MessageKind.VIDEO) || msg.deleted) return
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
                    SavedMessagesRules.isSaved(c.id) -> {
                        forwardToSaved(src)
                        openSaved()
                    }
                    c.group != null -> {
                        forwardToGroup(c.group, src)
                        openGroup(c.group)
                    }
                    c.peer != null -> {
                        forwardToPeer(c.peer, src)
                        openChat(c.peer)
                    }
                    else -> notice("некуда переслать")
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun sendAttachment(uri: Uri, forcedMime: String? = null) {
        if (forcedMime == null && looksLikeVisual(uri, null)) {
            stageAttachments(listOf(uri))
            return
        }
        sendAttachments(listOf(uri), forcedMime)
    }

    fun stageAttachments(uris: List<Uri>) {
        if (_state.value.editTarget != null) return
        val merged = (_state.value.pendingAttachments + uris).distinct().take(AlbumRules.MAX_PHOTOS)
        if (merged.isEmpty()) return
        _state.value = _state.value.copy(pendingAttachments = merged)
    }

    private fun sendAttachments(
        uris: List<Uri>,
        forcedMime: String? = null,
        caption: String? = null,
        pack: ReplyPack = ReplyPack(),
        destPeer: DirectoryDevice? = _state.value.peer,
        destGroup: RopeGroup? = _state.value.group,
    ) {
        val resolved = if (pack.id != null) pack else replyPack(_state.value.replyTo)
        if (pack.id == null && resolved.id != null) {
            _state.value = _state.value.copy(replyTo = null, replySpan = null)
        }
        scope.launch {
            try {
                val prepared = uris.take(AlbumRules.MAX_PHOTOS).mapNotNull { uri ->
                    prepareOutgoingMedia(uri, forcedMime)
                }
                if (prepared.isEmpty()) return@launch
                val images = prepared.filter { VideoRules.albumEligible(it.kind) }
                val rest = prepared.filter { !VideoRules.albumEligible(it.kind) }
                val slots = AlbumRules.slots(images.size)
                val cap = MediaSendRules.normalize(caption)
                images.zip(slots).forEach { (item, slot) ->
                    sendMediaBytes(
                        item.bytes,
                        item.mime,
                        item.name,
                        item.kind,
                        item.durationMs,
                        albumId = slot.albumId,
                        albumIndex = slot.index,
                        albumCount = slot.count,
                        caption = MediaSendRules.onFirstOnly(slot.index, cap),
                        pack = resolved,
                        destPeer = destPeer,
                        destGroup = destGroup,
                    )
                }
                rest.forEach { item ->
                    sendMediaBytes(
                        item.bytes,
                        item.mime,
                        item.name,
                        item.kind,
                        item.durationMs,
                        pack = resolved,
                        destPeer = destPeer,
                        destGroup = destGroup,
                    )
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    private data class OutgoingMedia(
        val bytes: ByteArray,
        val mime: String,
        val name: String,
        val kind: String,
        val durationMs: Long = 0,
    )

    private fun prepareOutgoingMedia(uri: Uri, forcedMime: String?): OutgoingMedia? {
        val cr = app.contentResolver
        var mime = forcedMime ?: cr.getType(uri) ?: "application/octet-stream"
        val name = attachmentName(uri, mime)
        if (VideoRules.looksLikeVideo(name, mime)) {
            val compressed = VideoCodec.normalizeForSend(
                app,
                uri,
                mime,
                name,
                File(app.cacheDir, "video-out"),
            )
            if (compressed == null) {
                notice(UserFacing.FILE_TOO_BIG)
                return null
            }
            return OutgoingMedia(
                compressed.bytes,
                compressed.mime,
                compressed.name.ifBlank { name },
                "video",
                compressed.durationMs,
            )
        }
        var bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: error("не удалось прочитать файл")
        if (bytes.size > VideoRules.MAX_OBJECT_BYTES) {
            notice(UserFacing.FILE_TOO_BIG)
            return null
        }
        if (mime.startsWith("image/") || looksLikeImage(name, mime)) {
            val normalized = ImageCodec.normalizeForSend(bytes, if (mime.startsWith("image/")) mime else "image/jpeg")
            bytes = normalized.first
            mime = normalized.second
        }
        val kind = VideoRules.kind(mime, name)
        return OutgoingMedia(bytes, mime, name, kind)
    }

    fun startVoice() {
        if (_state.value.recording || _state.value.recordingVideoNote) return
        try {
            voiceRecorder.start()
            unfurlJob?.cancel()
            unfurlResult = null
            _state.value = _state.value.copy(recording = true, recordMs = 0, error = null)
            recordJob?.cancel()
            recordJob = scope.launch {
                while (_state.value.recording) {
                    delay(80)
                    voiceRecorder.amplitude()
                    val next = _state.value.recordMs + 80
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
        val pack = replyPack(_state.value.replyTo)
        val destPeer = _state.value.peer
        val destGroup = _state.value.group
        _state.value = _state.value.copy(replyTo = null, replySpan = null)
        scope.launch {
            try {
                val bytes = take.file.readBytes()
                val wave = take.waveform.ifEmpty { VoiceWaveform.extract(take.file.absolutePath) }
                sendMediaBytes(
                    bytes,
                    "audio/mp4",
                    take.file.name,
                    "voice",
                    take.durationMs,
                    pack = pack,
                    destPeer = destPeer,
                    destGroup = destGroup,
                    waveform = wave,
                )
            } catch (e: Exception) {
                error(e)
            } finally {
                take.file.delete()
            }
        }
    }

    fun startVideoNote() {
        if (_state.value.recording || _state.value.recordingVideoNote || _state.value.call != null) return
        unfurlJob?.cancel()
        unfurlResult = null
        _state.value = _state.value.copy(recordingVideoNote = true, recordMs = 0, error = null)
    }

    fun bindVideoNotePreview(holder: SurfaceHolder, displayRotationDeg: Int) {
        if (!_state.value.recordingVideoNote || videoNoteRecorder.recording) return
        try {
            videoNoteRecorder.start(holder, displayRotationDeg)
            recordJob?.cancel()
            recordJob = scope.launch {
                while (_state.value.recordingVideoNote && videoNoteRecorder.recording) {
                    delay(200)
                    val next = _state.value.recordMs + 200
                    _state.value = _state.value.copy(recordMs = next)
                    if (next >= VideoNoteRules.MAX_MS) {
                        finishVideoNote(send = true)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            videoNoteRecorder.cancel()
            _state.value = _state.value.copy(recordingVideoNote = false, recordMs = 0)
            error(e)
        }
    }

    fun unbindVideoNotePreview() {
        if (_state.value.recordingVideoNote && videoNoteRecorder.recording) {
            finishVideoNote(false)
        }
    }

    fun finishVideoNote(send: Boolean) {
        val take = try {
            videoNoteRecorder.stop()
        } catch (_: Exception) {
            null
        }
        recordJob?.cancel()
        _state.value = _state.value.copy(recordingVideoNote = false, recordMs = 0)
        if (!send || take == null || take.durationMs < VideoNoteRules.MIN_MS) {
            take?.file?.delete()
            return
        }
        val pack = replyPack(_state.value.replyTo)
        val destPeer = _state.value.peer
        val destGroup = _state.value.group
        _state.value = _state.value.copy(replyTo = null, replySpan = null)
        scope.launch {
            try {
                val bytes = take.file.readBytes()
                sendMediaBytes(
                    bytes,
                    "video/mp4",
                    take.file.name,
                    VideoNoteRules.KIND,
                    take.durationMs,
                    pack = pack,
                    destPeer = destPeer,
                    destGroup = destGroup,
                )
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

    fun seekVoice(msg: ChatMessage, positionMs: Long) {
        val path = msg.localPath
        if (path.isNullOrBlank()) {
            retryMedia(msg)
            return
        }
        voicePlayer.seek(msg.id, path, positionMs)
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

    fun cycleVoiceSpeed() {
        voicePlayer.cycleSpeed()
        publishVoiceProgress()
    }

    private fun publishVoiceProgress() {
        _state.value = _state.value.copy(
            playingVoiceId = voicePlayer.playingId,
            voiceProgressId = voicePlayer.activeId,
            voicePositionMs = voicePlayer.positionMs(),
            voiceDurationMs = voicePlayer.durationMs(),
            voiceSpeed = voicePlayer.speed,
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
        val lp = msg.linkPreview
        if (lp != null && lp.hasImage() && (force || lp.localPath == null || !File(lp.localPath).isFile)) {
            scope.launch { downloadLinkThumb(msg.id, lp) }
        }
        if (msg.extra.isBlank()) return
        if (msg.kind != MessageKind.IMAGE && msg.kind != MessageKind.VOICE && msg.kind != MessageKind.FILE && msg.kind != MessageKind.VIDEO && msg.kind != MessageKind.VIDEO_NOTE) return
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
            notice("Название группы")
            return
        }
        scope.launch {
            busy(true)
            try {
                var g = api?.createGroup(name) ?: error("нет сети")
                val me = identity?.deviceId().orEmpty()
                g = g.copy(createdBy = g.createdBy.ifBlank { me })
                for (id in _state.value.pickedMembers) {
                    g = api?.addGroupMember(g.groupId, id)?.copy(createdBy = g.createdBy) ?: g
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
        val me = _state.value.profile?.deviceId
        val organizer = GroupChatUx.organizerId(g)
        if (!RoleRules.canManageGroupMembers(me in g.members, me, organizer, _state.value.profile?.role)) return
        scope.launch {
            try {
                val updated = api?.addGroupMember(g.groupId, deviceId)?.copy(createdBy = g.createdBy) ?: return@launch
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
        val me = _state.value.profile?.deviceId
        val organizer = GroupChatUx.organizerId(g)
        if (!RoleRules.canManageGroupMembers(me in g.members, me, organizer, _state.value.profile?.role)) return
        scope.launch {
            try {
                val updated = api?.removeGroupMember(g.groupId, deviceId)?.copy(createdBy = g.createdBy) ?: return@launch
                store.upsertGroup(updated)
                _state.value = _state.value.copy(group = updated)
                refreshDirectory()
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun leaveOpenGroup() {
        val g = _state.value.group ?: return
        val me = _state.value.profile?.deviceId ?: return
        if (!RoleRules.canLeaveGroup(me in g.members)) return
        scope.launch {
            try {
                api?.removeGroupMember(g.groupId, me)
                store.deleteGroup(g.groupId)
                refreshDirectory()
                _state.value = applyNav(Screen.Groups, NavMode.SwitchTab).copy(
                    group = null,
                    messages = emptyList(),
                    replyTo = null,
                    replySpan = null,
                    editTarget = null,
                    notice = "Вы вышли из «${g.name}»",
                )
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun startCall() {
        startCall(video = false)
    }

    fun startVideoCall() {
        startCall(video = true)
    }

    private fun startCall(video: Boolean) {
        if (_state.value.group != null) return
        val hint = _state.value.peer ?: return
        if (ChatIds.isGroup(hint.deviceId) || ChatIds.isSaved(hint.deviceId)) return
        if (!VideoCallRules.showHeader(hint.deviceId, isGroup = false)) return
        val peer = resolveCallPeer(hint.deviceId, hint, fetch = true) ?: return
        callPeerName = peer.displayName.ifBlank { hint.displayName }
        applyCallEffects(
            callMachine.localStart(
                UUID.randomUUID().toString(),
                peer.deviceId,
                identity?.deviceId().orEmpty(),
                video = video,
            ),
        )
    }

    fun acceptCall() {
        val before = _state.value.call?.phase
        applyCallEffects(callMachine.localAccept())
        val st = _state.value
        if (before == CallPhase.RINGING_IN && st.call != null) {
            val next = VideoCallRules.noticeAfterAccept(st.callNotice)
            if (next != st.callNotice) {
                _state.value = st.copy(callNotice = next)
            }
        }
    }

    fun rejectCall() {
        applyCallEffects(callMachine.localReject())
    }

    fun hangup() {
        applyCallEffects(callMachine.localHangup())
    }

    fun toggleCallMute() {
        val next = !_state.value.callMicMuted
        _state.value = _state.value.copy(callMicMuted = next)
        rtc?.setMicEnabled(!next)
        wssAudio?.setMuted(next)
    }

    fun toggleCallSpeaker() {
        val next = !_state.value.callSpeakerOn
        _state.value = _state.value.copy(callSpeakerOn = next)
        CallAudio.setSpeaker(app, next)
    }

    fun toggleCallCamera() {
        val st = _state.value
        if (st.call == null) return
        val next = !st.callCamMuted
        _state.value = st.copy(
            callCamMuted = next,
            callNotice = if (!next) VideoCallRules.noticeAfterCameraUnmute(st.callNotice) else st.callNotice,
        )
        rtc?.setCameraEnabled(!next)
    }

    fun flipCallCamera() {
        if (!VideoCallRules.flipCameraWhileSending(_state.value.callCamMuted)) return
        rtc?.flipCamera()
    }

    fun cameraDenied() {
        val st = _state.value
        if (st.call == null) return
        _state.value = st.copy(
            callCamMuted = true,
            callNotice = VideoCallRules.cameraDenyFallbackNotice(),
        )
    }

    fun micDenied() {
        notice(VideoCallRules.micDeniedNotice())
    }

    fun callEglContext(): org.webrtc.EglBase.Context? = rtc?.eglContext()

    fun bindCallRemote(sink: VideoSink) {
        val prev = boundRemote
        if (prev !== null && prev !== sink) rtc?.detachRemoteSink(prev)
        boundRemote = sink
        rtc?.attachRemoteSink(sink)
    }

    fun bindCallLocal(sink: VideoSink) {
        val prev = boundLocal
        if (prev !== null && prev !== sink) rtc?.detachLocalSink(prev)
        boundLocal = sink
        rtc?.attachLocalSink(sink)
    }

    fun unbindCallRemote(sink: VideoSink) {
        if (boundRemote === sink) {
            rtc?.detachRemoteSink(sink)
            boundRemote = null
        }
    }

    fun unbindCallLocal(sink: VideoSink) {
        if (boundLocal === sink) {
            rtc?.detachLocalSink(sink)
            boundLocal = null
        }
    }

    fun revokeMember(memberId: String) {
        val st = _state.value
        val target = st.devices.find { PeerIds.same(it.memberId, memberId) } ?: return
        if (!RevokeRules.canRevokeTarget(st.profile?.role, st.profile?.memberId, st.profile?.deviceId, target)) {
            return
        }
        scope.launch {
            busy(true)
            try {
                api?.revokeMember(memberId) ?: error("нет сети")
                notice(RevokeRules.noticeRevoked(target.displayName.ifBlank { target.deviceId.take(8) }))
                refreshDirectory()
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun createInvite() {
        if (!RoleRules.canInvite(_state.value.profile?.role)) {
            return
        }
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
            val owner = RoleRules.canShowAdminCards(_state.value.profile?.role)
            _state.value = _state.value.copy(
                error = null,
                statusText = when {
                    st != null && owner -> st.toString(2)
                    owner -> "ядро сейчас недоступно"
                    else -> "Вы гость. Приложение обновляется здесь, без прав owner."
                },
                admin = if (owner) st?.let { AdminSnapshot.from(it) } else null,
                updateText = updateHint,
                appUpdateAvailable = newer,
                latestAppVersion = latest?.version.orEmpty(),
            )
        }
    }

    fun upgradeCore(password: String, keyPem: String) {
        val profile = store.profile()
        if (profile == null) {
            notice("сначала подключитесь к серверу")
            return
        }
        if (!RoleRules.canUpgradeCore(profile.role)) {
            notice("Ядро на VPS обновляет только owner")
            return
        }
        if (password.isBlank() && keyPem.isBlank()) {
            notice("Введите SSH-пароль или ключ")
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
                applyDeviceBackup(DeviceBackup.open(bytes, vault::unwrap))
                identity = DeviceIdentity.fromBytes(vault.load())
                val profile = store.profile()
                if (profile != null) {
                    attached(profile)
                } else {
                    notice("восстановили ключ, но профиля сервера нет")
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    fun resume() {
        store.profile()?.let { connectSocket(it) }
    }

    private fun sendMediaBytes(
        bytes: ByteArray,
        mime: String,
        name: String,
        kind: String,
        durationMs: Long,
        albumId: String? = null,
        albumIndex: Int = 0,
        albumCount: Int = 1,
        caption: String? = null,
        pack: ReplyPack = ReplyPack(),
        destPeer: DirectoryDevice? = _state.value.peer,
        destGroup: RopeGroup? = _state.value.group,
        waveform: List<Int> = emptyList(),
    ) {
        val id = identity ?: return
        val group = destGroup
        val peer = destPeer
        if (group == null && peer == null) {
            throw IllegalStateException("откройте чат, чтобы отправить вложение")
        }
        val saved = SavedMessagesRules.isSaved(peer?.deviceId) && group == null
        val enc = encryptObject(bytes)
        val grouped = VideoRules.albumEligible(kind)
        val cap = MediaSendRules.normalize(caption)
        fun payloadOf(objectId: String, groupId: String?): MediaPayload = MediaPayload(
            kind = kind,
            objectId = objectId,
            sha256 = enc.sha256,
            keyB64 = Base64.encodeToString(enc.key, Base64.NO_WRAP),
            mime = mime,
            name = name,
            size = bytes.size.toLong(),
            durationMs = durationMs,
            groupId = groupId,
            albumId = if (grouped) albumId else null,
            albumIndex = if (grouped) albumIndex else 0,
            albumCount = if (grouped) albumCount else 1,
            caption = cap,
            waveform = waveform,
        ).withReply(
            pack.id,
            pack.preview,
            pack.name,
            pack.quoteText,
            pack.quoteStart,
            pack.quoteEnd,
        )
        if (saved) {
            val objectId = SavedMessagesRules.localObjectId(UUID.randomUUID().toString())
            val payload = payloadOf(objectId, null)
            val cache = persistPlain(objectId, name, mime, bytes)
            insertLocalSaved(
                text = payload.preview(),
                kind = payload.messageKind(),
                extra = payload.toJson(),
                localFile = cache,
                pack = pack,
            )
            return
        }
        val api = api ?: throw IllegalStateException("нет сети")
        val uploaded = api.uploadObject(enc.ciphertext, enc.sha256)
        val objectId = uploaded.getString("object_id")
        val inKnownGroup = group != null && _state.value.groups.any { it.groupId == group.groupId }
        val payload = payloadOf(objectId, if (inKnownGroup) group?.groupId else null)
        val cache = persistPlain(objectId, name, mime, bytes)
        if (inKnownGroup && group != null) {
            sendGroupPayload(
                group,
                EnvelopeTypes.MEDIA,
                payload.toJson().toByteArray(),
                payload.preview(),
                payload.messageKind(),
                payload.toJson(),
                cache,
                pack = pack,
            )
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
            replyToId = pack.id,
            replyPreview = pack.preview,
            replyName = pack.name,
            quoteText = pack.quoteText,
            quoteStart = pack.quoteStart,
            quoteEnd = pack.quoteEnd,
        )
        store.insertMessage(local)
        refreshMessages(dest.deviceId)
        pushEnvelope(env.bytes)
    }

    private fun sendGroupText(
        group: RopeGroup,
        text: String,
        reply: ChatMessage? = null,
        pack: ReplyPack = replyPack(reply),
        forwardedFrom: String? = null,
        linkPreview: PackedLinkPreview? = null,
    ) {
        val attributed = JsonIds.optional(forwardedFrom)
        val body = GroupTextPayload(
            group.groupId,
            text,
            group.epoch,
            if (attributed == null) pack.id else null,
            if (attributed == null) pack.preview else "",
            if (attributed == null) pack.name else "",
            forwardedFrom = attributed,
            quoteText = if (attributed == null) pack.quoteText else "",
            quoteStart = if (attributed == null) pack.quoteStart else -1,
            quoteEnd = if (attributed == null) pack.quoteEnd else -1,
            linkPreview = linkPreview,
        ).toJson().toByteArray()
        sendGroupPayload(
            group,
            EnvelopeTypes.GROUP_TEXT,
            body,
            text,
            MessageKind.GROUP_TEXT,
            "",
            null,
            if (attributed == null) reply else null,
            attributed,
            if (attributed == null) pack else ReplyPack(),
            linkPreview,
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
        forwardedFrom: String? = null,
        pack: ReplyPack = ReplyPack(),
        linkPreview: PackedLinkPreview? = null,
    ) {
        scope.launch {
            val id = identity ?: return@launch
            try {
                val members = group.members.filter { it != id.deviceId() }
                if (members.isEmpty()) {
                    notice("В группе пока никого нет")
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
                    notice("Нет ключей участников")
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
                    replyToId = if (forwardedFrom == null) pack.id ?: reply?.id else null,
                    replyPreview = if (forwardedFrom == null) pack.preview.ifBlank { reply?.preview().orEmpty() } else "",
                    replyName = if (forwardedFrom == null) pack.name.ifBlank { replyName(reply) } else "",
                    quoteText = if (forwardedFrom == null) pack.quoteText else "",
                    quoteStart = if (forwardedFrom == null) pack.quoteStart else -1,
                    quoteEnd = if (forwardedFrom == null) pack.quoteEnd else -1,
                    forwardedFrom = forwardedFrom,
                    linkPreview = linkPreview,
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

    private fun scheduleUnfurl(text: String) {
        unfurlJob?.cancel()
        unfurlResult = null
        val url = LinkPreviewRules.firstHttps(text)
        val dismissed = _state.value.composerPreviewDismissedUrl
        if (url != dismissed && url != null) {
            _state.value = _state.value.copy(composerPreviewDismissedUrl = null)
        }
        if (!LinkPreviewRules.shouldFetch(
                _state.value.linkPreviewsEnabled,
                _state.value.recording || _state.value.recordingVideoNote,
                text,
            ) ||
            url == null ||
            url == dismissed
        ) {
            if (_state.value.composerPreview?.url != url) {
                _state.value = _state.value.copy(composerPreview = null)
            }
            return
        }
        if (_state.value.composerPreview?.url == url) return
        unfurlJob = scope.launch {
            val self = coroutineContext[Job]
            delay(LinkPreviewRules.UNFURL_DEBOUNCE_MS)
            if (_state.value.recording || _state.value.recordingVideoNote) return@launch
            if (!_state.value.linkPreviewsEnabled) return@launch
            if (!LinkPreviewRules.keepUnfurl(url, _state.value.draftText)) return@launch
            val withThumb = fetchPackedPreview(url)
            if (withThumb == null) {
                if (_state.value.composerPreview?.url == url) {
                    _state.value = _state.value.copy(composerPreview = null)
                }
                return@launch
            }
            if (!isActive) return@launch
            if (!LinkPreviewRules.keepUnfurl(url, _state.value.draftText)) return@launch
            if (_state.value.composerPreviewDismissedUrl == url) return@launch
            if (unfurlJob !== self) return@launch
            unfurlResult = withThumb
            if (LinkPreviewRules.writeComposerCard(url, _state.value.draftText)) {
                _state.value = _state.value.copy(composerPreview = withThumb)
            }
        }
    }

    private suspend fun fetchPackedPreview(url: String): PackedLinkPreview? {
        val page = runCatching { LinkUnfurl.fetch(url) }.getOrNull() ?: return null
        val packed = LinkPreviewRules.fromOg(url, page.title, page.description) ?: return null
        val jpeg = page.imageJpeg?.takeIf { it.isNotEmpty() } ?: return packed
        return packed.copy(
            localPath = persistPlain("lp-${UUID.randomUUID()}", "lp.jpg", "image/jpeg", jpeg).absolutePath,
        )
    }

    private suspend fun resolvePreviewForSend(
        text: String,
        attached: PackedLinkPreview?,
        dismissedUrl: String?,
        pendingJob: Job?,
        upload: Boolean,
    ): PackedLinkPreview? {
        val deadlineNs = System.nanoTime() + LinkPreviewRules.SEND_WAIT_MS * 1_000_000L
        val packed = LinkPreviewRules.resolveSendPreview(
            enabled = _state.value.linkPreviewsEnabled,
            sendText = text,
            dismissedUrl = dismissedUrl,
            attached = attached,
            jobResultAfterWait = {
                if (pendingJob != null && pendingJob.isActive) {
                    val left = remainingMs(deadlineNs)
                    if (left > 0L) withTimeoutOrNull(left) { pendingJob.join() }
                }
                unfurlResult
            },
            fetch = { url ->
                val left = remainingMs(deadlineNs)
                if (left <= 0L) null
                else withTimeoutOrNull(left) { fetchPackedPreview(url) }
            },
        )
        return attachThumb(packed, upload)
    }

    private fun remainingMs(deadlineNs: Long): Long =
        ((deadlineNs - System.nanoTime()) / 1_000_000L).coerceAtLeast(0L)

    private fun attachThumb(preview: PackedLinkPreview?, upload: Boolean): PackedLinkPreview? {
        if (preview == null) return null
        val jpeg = preview.localPath?.let { File(it).takeIf { f -> f.isFile && f.length() > 0 } }?.readBytes()
        if (jpeg == null) return preview.withoutImage().copy(localPath = preview.localPath)
        if (!upload) return preview.withoutImage().copy(localPath = preview.localPath)
        return try {
            val enc = encryptObject(jpeg)
            val uploaded = (api ?: return preview.withoutImage().copy(localPath = preview.localPath))
                .uploadObject(enc.ciphertext, enc.sha256)
            preview.copy(
                objectId = uploaded.getString("object_id"),
                sha256 = enc.sha256,
                keyB64 = Base64.encodeToString(enc.key, Base64.NO_WRAP),
                mime = "image/jpeg",
                name = "lp.jpg",
                size = enc.ciphertext.size.toLong(),
            )
        } catch (_: Exception) {
            preview.withoutImage().copy(localPath = preview.localPath)
        }
    }

    private suspend fun sendPeerText(
        peer: DirectoryDevice,
        text: String,
        pack: ReplyPack,
        preview: PackedLinkPreview?,
    ) {
        val id = identity ?: return
        try {
            val packed = TextBody.encode(
                text,
                pack.id,
                pack.preview,
                pack.name,
                quoteText = pack.quoteText,
                quoteStart = pack.quoteStart,
                quoteEnd = pack.quoteEnd,
                preview = preview,
            )
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
                replyToId = pack.id,
                replyPreview = pack.preview,
                replyName = pack.name,
                quoteText = pack.quoteText,
                quoteStart = pack.quoteStart,
                quoteEnd = pack.quoteEnd,
                linkPreview = preview,
            )
            store.insertMessage(local)
            refreshMessages(peer.deviceId)
            pushEnvelope(env.bytes)
        } catch (e: Exception) {
            error(e)
        }
    }

    private fun downloadLinkThumb(messageId: String, preview: PackedLinkPreview) {
        val objectId = preview.objectId ?: return
        if (preview.sha256.isBlank() || preview.keyB64.isBlank()) return
        if (SavedMessagesRules.isLocalObject(objectId)) return
        if (SavedMessagesRules.skipNetwork(store.message(messageId)?.peerDeviceId)) return
        try {
            val api = api ?: return
            val (blob, headerHash) = api.downloadObject(objectId)
            val expected = preview.sha256.ifBlank { headerHash }
            val key = Base64.decode(preview.keyB64, Base64.DEFAULT)
            val plain = decryptObject(key, blob, expected)
            val dest = persistPlain(objectId, preview.name.ifBlank { "lp.jpg" }, preview.mime.ifBlank { "image/jpeg" }, plain)
            store.updateLinkThumb(messageId, dest.absolutePath)
            refreshOpenChat()
        } catch (_: Exception) {
        }
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

    private fun looksLikeVisual(uri: Uri, forcedMime: String?): Boolean {
        val mime = forcedMime ?: app.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = attachmentName(uri, mime)
        return VideoRules.looksLikeVideo(name, mime) || looksLikeImage(name, mime)
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
        RopeConnectionService.start(app)
        checkAppUpdate(openStatus = false)
    }

    private fun refreshIceServers(profile: ServerProfile): ServerProfile {
        val info = runCatching { api?.info() }.getOrNull() ?: return profile
        val ice = IceServers.infoJson(info) ?: return profile
        iceCachedAtMs = System.currentTimeMillis()
        return profile.copy(iceServersJson = ice)
    }

    private fun prefetchIce() {
        scope.launch {
            val cur = store.profile() ?: return@launch
            val updated = refreshIceServers(cur)
            store.saveProfile(updated)
            _state.value = _state.value.copy(profile = updated)
            applyCallEffects(callMachine.onIceServers(updated.iceServersJson))
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

    private fun replyPack(reply: ChatMessage?, span: QuoteSpan? = _state.value.replySpan): ReplyPack {
        if (reply == null) return ReplyPack()
        val source = reply.preview()
        val packed = QuoteSpanRules.packed(source, span)
        return ReplyPack(
            id = reply.id,
            preview = packed?.text ?: source,
            name = replyName(reply),
            quoteText = packed?.text.orEmpty(),
            quoteStart = packed?.start ?: -1,
            quoteEnd = packed?.end ?: -1,
        )
    }

    private fun openChatId(): String? {
        _state.value.group?.let { return ChatIds.group(it.groupId) }
        return _state.value.peer?.deviceId
    }

    private fun enterChat(chatId: String, peer: DirectoryDevice?, group: RopeGroup?) {
        val prefs = store.chatPrefs(chatId)
        val messages = store.messages(chatId)
        val anchorId = UnreadSeparatorRules.firstUnreadId(messages, prefs.unread, prefs.lastReadMs)
        val pending = if (MediaSendRules.keepPendingOnEnter(openChatId(), chatId)) {
            _state.value.pendingAttachments
        } else {
            emptyList()
        }
        store.saveChatPrefs(chatId, prefs.copy(unread = 0, lastReadMs = System.currentTimeMillis()))
        _state.value = applyNav(Screen.Chat, NavMode.Push).copy(
            peer = peer,
            group = group,
            messages = messages,
            draftText = prefs.draft,
            replyTo = null,
            replySpan = null,
            editTarget = null,
            messageQuery = "",
            pinnedMessageId = prefs.pinnedMessageId,
            unreadAnchorId = anchorId,
            scrollToMessageId = anchorId,
            pendingAttachments = pending,
            composerPreview = null,
            composerPreviewDismissedUrl = null,
        )
        publishTyping()
        prefetchMedia(_state.value.messages)
        refreshConversations()
        scheduleUnfurl(prefs.draft)
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
        if (SavedMessagesRules.skipNetwork(target)) return
        sendControl(EnvelopeTypes.RECEIPT, ChatControl(ChatControl.TYPING, target).toJson().toByteArray())
    }

    private fun noteTyping(chatId: String, deviceId: String, name: String) {
        val slot = typingUntil.getOrPut(chatId) { mutableMapOf() }
        slot[deviceId.ifBlank { name }] = name.ifBlank { "кто-то" } to System.currentTimeMillis() + TypingRules.TTL_MS
        publishTyping()
        if (typingJob?.isActive == true) return
        typingJob = scope.launch {
            while (typingUntil.isNotEmpty()) {
                delay(800)
                val now = System.currentTimeMillis()
                typingUntil.values.forEach { people ->
                    people.entries.removeAll { !TypingRules.isActive(it.value.second, now) }
                }
                typingUntil.entries.removeAll { it.value.isEmpty() }
                publishTyping()
            }
        }
    }

    private fun publishTyping() {
        val open = openChatId()
        val now = System.currentTimeMillis()
        val names = open?.let { chat ->
            typingUntil[chat]
                ?.filterValues { TypingRules.isActive(it.second, now) }
                ?.values
                ?.map { it.first }
                ?.distinct()
        }.orEmpty()
        val line = GroupChatUx.typingLine(names, _state.value.group != null)
        val next = line.ifBlank { null }
        if (_state.value.typingName != next) {
            _state.value = _state.value.copy(typingName = next)
        }
    }

    private fun applyEdit(msg: ChatMessage, text: String) {
        store.editMessage(msg.id, text)
        refreshOpenChat()
        if (SavedMessagesRules.skipNetwork(openChatId() ?: msg.peerDeviceId)) return
        sendControl(
            EnvelopeTypes.RECEIPT,
            ChatControl(ChatControl.EDIT, msg.id, text = text).toJson().toByteArray(),
        )
    }

    private fun forwardToPeer(peer: DirectoryDevice, src: ChatMessage) {
        if (SavedMessagesRules.isSaved(peer.deviceId)) {
            forwardToSaved(src)
            return
        }
        val id = identity ?: return
        val from = ForwardRules.originName(src, _state.value.profile?.displayName.orEmpty())
        if (src.kind == MessageKind.TEXT || src.kind == MessageKind.GROUP_TEXT) {
            val packed = TextBody.encode(
                src.text, null, "", "",
                forwardedFrom = from,
                preview = src.linkPreview,
            )
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
                    forwardedFrom = from,
                    linkPreview = src.linkPreview,
                ),
            )
            refreshMessages(peer.deviceId)
            pushEnvelope(env.bytes)
            return
        }
        val payload = attributedMediaPayload(src, from, groupId = null) ?: return
        val env = id.encryptTyped(
            publicIdentityFromBlob(peer.publicIdentity),
            EnvelopeTypes.MEDIA,
            payload.toJson().toByteArray(),
        )
        val cache = src.localPath?.let { File(it) }?.takeIf { it.isFile }
        store.insertMessage(
            src.copy(
                id = env.messageId,
                peerDeviceId = peer.deviceId,
                outgoing = true,
                text = payload.preview(),
                status = MessageStatus.CREATED,
                timestampMs = env.timestampMs.toLong(),
                envelope = env.bytes,
                extra = payload.toJson(),
                groupId = null,
                localPath = cache?.absolutePath,
                senderId = id.deviceId(),
                senderName = _state.value.profile?.displayName.orEmpty(),
                replyToId = null,
                replyPreview = "",
                replyName = "",
                forwardedFrom = from,
                reactions = emptyList(),
            ),
        )
        refreshMessages(peer.deviceId)
        pushEnvelope(env.bytes)
    }

    private fun forwardToGroup(group: RopeGroup, src: ChatMessage) {
        val from = ForwardRules.originName(src, _state.value.profile?.displayName.orEmpty())
        if (src.kind == MessageKind.TEXT || src.kind == MessageKind.GROUP_TEXT) {
            sendGroupText(group, src.text, forwardedFrom = from, linkPreview = src.linkPreview)
            return
        }
        val payload = attributedMediaPayload(src, from, groupId = group.groupId) ?: return
        sendGroupPayload(
            group,
            EnvelopeTypes.MEDIA,
            payload.toJson().toByteArray(),
            payload.preview(),
            payload.messageKind(),
            payload.toJson(),
            src.localPath?.let { File(it) }?.takeIf { it.isFile },
            forwardedFrom = from,
        )
    }

    private fun forwardToSaved(src: ChatMessage) {
        val from = ForwardRules.originName(src, _state.value.profile?.displayName.orEmpty())
        if (src.kind == MessageKind.TEXT || src.kind == MessageKind.GROUP_TEXT) {
            saveLocalText(src.text, forwardedFrom = from, linkPreview = src.linkPreview)
            return
        }
        val payload = attributedMediaPayload(src, from, groupId = null, upload = false) ?: return
        val cache = src.localPath?.let { File(it) }?.takeIf { it.isFile }
        insertLocalSaved(
            text = payload.preview(),
            kind = payload.messageKind(),
            extra = payload.toJson(),
            localFile = cache,
            forwardedFrom = from,
        )
    }

    private fun saveLocalText(
        text: String,
        reply: ChatMessage? = null,
        forwardedFrom: String? = null,
        pack: ReplyPack = replyPack(reply),
        linkPreview: PackedLinkPreview? = null,
    ) {
        val attributed = JsonIds.optional(forwardedFrom)
        insertLocalSaved(
            text = text,
            kind = MessageKind.TEXT,
            extra = "",
            localFile = null,
            reply = if (attributed == null) reply else null,
            forwardedFrom = attributed,
            pack = if (attributed == null) pack else ReplyPack(),
            linkPreview = linkPreview,
        )
    }

    private fun insertLocalSaved(
        text: String,
        kind: MessageKind,
        extra: String,
        localFile: File?,
        reply: ChatMessage? = null,
        forwardedFrom: String? = null,
        pack: ReplyPack = ReplyPack(),
        linkPreview: PackedLinkPreview? = null,
    ) {
        val local = ChatMessage(
            id = store.newId(),
            peerDeviceId = SavedMessagesRules.ID,
            outgoing = true,
            text = text,
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = System.currentTimeMillis(),
            envelope = null,
            kind = kind,
            extra = extra,
            localPath = localFile?.absolutePath,
            senderId = identity?.deviceId().orEmpty(),
            senderName = _state.value.profile?.displayName.orEmpty(),
            replyToId = pack.id ?: reply?.id,
            replyPreview = pack.preview.ifBlank { reply?.preview().orEmpty() },
            replyName = pack.name.ifBlank { replyName(reply) },
            quoteText = pack.quoteText,
            quoteStart = pack.quoteStart,
            quoteEnd = pack.quoteEnd,
            forwardedFrom = forwardedFrom,
            linkPreview = linkPreview,
        )
        store.insertMessage(local)
        refreshMessages(SavedMessagesRules.ID)
    }

    private fun attributedMediaPayload(
        src: ChatMessage,
        forwardedFrom: String,
        groupId: String?,
        upload: Boolean = true,
    ): MediaPayload? {
        val file = src.localPath?.let { File(it) }?.takeIf { it.isFile }
        if (src.extra.isBlank() && file == null) {
            notice("это вложение уже нельзя переслать")
            return null
        }
        val base = if (src.extra.isNotBlank()) {
            MediaPayload.parse(src.extra)
        } else {
            MediaPayload(
                kind = when (src.kind) {
                    MessageKind.VOICE -> "voice"
                    MessageKind.IMAGE -> "image"
                    MessageKind.VIDEO -> "video"
                    MessageKind.VIDEO_NOTE -> VideoNoteRules.KIND
                    else -> "file"
                },
                objectId = "",
                sha256 = "",
                keyB64 = "",
                mime = "application/octet-stream",
                name = file?.name.orEmpty(),
                size = file?.length() ?: 0,
            )
        }
        if (file != null) {
            val bytes = file.readBytes()
            val enc = encryptObject(bytes)
            if (!upload) {
                val objectId = SavedMessagesRules.localObjectId(UUID.randomUUID().toString())
                persistPlain(objectId, base.name.ifBlank { file.name }, base.mime, bytes)
                return base.copy(
                    objectId = objectId,
                    sha256 = enc.sha256,
                    keyB64 = Base64.encodeToString(enc.key, Base64.NO_WRAP),
                    size = bytes.size.toLong(),
                    groupId = null,
                    forwardedFrom = forwardedFrom,
                ).withoutReply()
            }
            val uploaded = (api ?: throw IllegalStateException("нет сети")).uploadObject(enc.ciphertext, enc.sha256)
            persistPlain(uploaded.getString("object_id"), base.name.ifBlank { file.name }, base.mime, bytes)
            return base.copy(
                objectId = uploaded.getString("object_id"),
                sha256 = enc.sha256,
                keyB64 = Base64.encodeToString(enc.key, Base64.NO_WRAP),
                size = bytes.size.toLong(),
                groupId = groupId,
                forwardedFrom = forwardedFrom,
            ).withoutReply()
        }
        if (!upload) {
            notice("это вложение уже нельзя переслать")
            return null
        }
        return base.copy(groupId = groupId, forwardedFrom = forwardedFrom).withoutReply()
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
                val stored = store.groups()
                store.rehomeMisroutedMedia()
                val peer = _state.value.peer?.let { cur -> devices.find { it.deviceId == cur.deviceId } ?: cur }
                val group = _state.value.group?.let { cur ->
                    val found = stored.find { it.groupId == cur.groupId }
                    found?.copy(createdBy = found.createdBy.ifBlank { cur.createdBy }) ?: cur
                }
                _state.value = _state.value.copy(devices = devices, groups = stored, peer = peer, group = group)
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
                subtitle = ChatListPreviewRules.copy(
                    last = last,
                    draft = p.draft,
                    isGroup = false,
                    myDeviceId = identity?.deviceId().orEmpty(),
                    online = d.online,
                ).text,
                isGroup = false,
                online = d.online,
                last = last,
                peer = d,
                pinned = p.pinned,
                muted = p.muted,
                unread = p.unread,
                archived = p.archived,
            )
        }
        val gs = groups.map { g ->
            val id = ChatIds.group(g.groupId)
            val last = lastBy[id]
            val p = prefs[id] ?: ChatPrefs()
            Conversation(
                id = id,
                title = g.name,
                subtitle = ChatListPreviewRules.copy(
                    last = last,
                    draft = p.draft,
                    isGroup = true,
                    myDeviceId = identity?.deviceId().orEmpty(),
                    memberCount = g.members.size,
                ).text,
                isGroup = true,
                online = g.members.any { it in _state.value.onlineIds && it != identity?.deviceId() },
                last = last,
                group = g,
                pinned = p.pinned,
                muted = p.muted,
                unread = p.unread,
                archived = p.archived,
            )
        }
        val leftover = lastBy.keys
            .filter { id ->
                ChatRouting.showLeftoverThread(id) &&
                    dms.none { it.id == id } &&
                    gs.none { it.id == id } &&
                    !SavedMessagesRules.isSaved(id)
            }
            .map { id ->
                val p = prefs[id] ?: ChatPrefs()
                Conversation(
                    id = id,
                    title = id.take(8),
                    subtitle = ChatListPreviewRules.copy(
                        last = lastBy[id],
                        draft = p.draft,
                        isGroup = false,
                        myDeviceId = identity?.deviceId().orEmpty(),
                        online = id in _state.value.onlineIds,
                    ).text,
                    isGroup = false,
                    online = id in _state.value.onlineIds,
                    last = lastBy[id],
                    peer = devices.find { it.deviceId == id },
                    pinned = p.pinned,
                    muted = p.muted,
                    unread = p.unread,
                    archived = p.archived,
                )
            }
        val savedPrefs = SavedMessagesRules.defaultPrefs(prefs[SavedMessagesRules.ID])
        if (prefs[SavedMessagesRules.ID] == null) {
            store.saveChatPrefs(SavedMessagesRules.ID, savedPrefs)
        }
        val saved = SavedMessagesRules.conversation(
            lastBy[SavedMessagesRules.ID],
            savedPrefs,
            identity?.deviceId().orEmpty(),
        )
        _state.value = _state.value.copy(
            conversations = (listOf(saved) + dms + gs + leftover).sortedWith { a, b -> ChatListRules.compare(a, b) },
        )
    }

    private fun connectSocket(profile: ServerProfile) {
        RopeConnectionService.start(app)
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
        if (store.profile() == null) {
            RopeConnectionService.stop(app)
            return
        }
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
                if (mid.isNotBlank()) {
                    store.updateStatus(mid, MessageStatus.SENT_TO_SERVER)
                    refreshOpenChat()
                }
            }
            "delivered" -> {
                val mid = obj.optString("message_id")
                store.updateStatus(mid, MessageStatus.DELIVERED_TO_DEVICE)
                refreshOpenChat()
            }
            "deliver" -> handleDeliver(obj)
            "call" -> handleCallEvent(obj, sealed = false)
            "error" -> {
                val code = obj.optString("code")
                val msg = obj.optString("message")
                val call = _state.value.call
                val ringingOut = call != null &&
                    call.phase == CallPhase.RINGING_OUT &&
                    call.link != CallLinkState.FAILED
                if (ringingOut && (code == "not_found" || msg.contains("offline", ignoreCase = true))) {
                    applyCallEffects(callMachine.onRingSendFailed())
                } else if (call == null || call.phase != CallPhase.ACTIVE) {
                    notice(msg)
                }
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
                val packed = TextBody.decode(plain.text)
                val msg = ChatMessage(
                    id = plain.messageId,
                    peerDeviceId = sender.deviceId,
                    outgoing = false,
                    text = packed.text,
                    status = MessageStatus.DELIVERED_TO_DEVICE,
                    timestampMs = plain.timestampMs.toLong(),
                    kind = MessageKind.TEXT,
                    senderId = sender.deviceId,
                    senderName = sender.displayName,
                    replyToId = packed.replyTo,
                    replyPreview = packed.replyPreview,
                    replyName = packed.replyName,
                    quoteText = packed.quoteText,
                    quoteStart = packed.quoteStart,
                    quoteEnd = packed.quoteEnd,
                    forwardedFrom = packed.forwardedFrom,
                    linkPreview = packed.linkPreview,
                )
                store.insertMessage(msg)
                ack(plain.messageId)
                notifyIfHidden(sender.displayName, packed.text, sender.deviceId)
                packed.linkPreview?.let { ensureMedia(msg.copy(linkPreview = it)) }
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
                    quoteText = payload.quoteText,
                    quoteStart = payload.quoteStart,
                    quoteEnd = payload.quoteEnd,
                    forwardedFrom = payload.forwardedFrom,
                    linkPreview = payload.linkPreview,
                )
                store.insertMessage(msg)
                ack(typed.messageId)
                notifyIfHidden(sender.displayName, payload.text, chatId)
                payload.linkPreview?.let { ensureMedia(msg.copy(linkPreview = it)) }
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
                    replyToId = payload.replyTo,
                    replyPreview = payload.replyPreview,
                    replyName = payload.replyName,
                    quoteText = payload.quoteText,
                    quoteStart = payload.quoteStart,
                    quoteEnd = payload.quoteEnd,
                    forwardedFrom = payload.forwardedFrom,
                )
                store.insertMessage(msg)
                ack(typed.messageId)
                notifyIfHidden(sender.displayName, payload.preview(), chatId, payload.albumId)
                scope.launch { downloadMedia(msg.id, payload) }
            }
            EnvelopeTypes.RECEIPT -> {
                val typed = id.decryptTyped(publicIdentityFromBlob(sender.publicIdentity), env)
                val raw = String(typed.body)
                val control = ChatControl.parse(raw)
                val reaction = ReactionPayload.parse(raw)
                when {
                    control?.kind == ChatControl.EDIT -> {
                        val target = store.message(control.targetId)
                        if (ChatControlRules.allowEdit(sender.deviceId, target)) {
                            store.editMessage(control.targetId, control.text)
                        }
                    }
                    control?.kind == ChatControl.DELETE -> {
                        val target = store.message(control.targetId)
                        if (ChatControlRules.allowDelete(sender.deviceId, target)) {
                            store.markDeleted(control.targetId)
                        }
                    }
                    control?.kind == ChatControl.TYPING -> {
                        val known = _state.value.groups.map { it.groupId }
                        ChatControlRules.typingChatId(sender.deviceId, control.targetId, known)?.let { chatId ->
                            noteTyping(chatId, sender.deviceId, sender.displayName)
                        }
                    }
                    control?.kind == ChatControl.PIN -> {
                        val target = store.message(control.targetId)
                        val gid = JsonIds.optional(target?.groupId)
                            ?: target?.peerDeviceId?.takeIf { ChatIds.isGroup(it) }?.let { ChatIds.rawGroupId(it) }
                        val members = gid?.let { store.group(it)?.members }
                        if (ChatControlRules.allowPin(sender.deviceId, target, members) && target != null) {
                            val chatId = target.peerDeviceId
                            val cur = store.chatPrefs(chatId)
                            val next = if (control.op == ReactionPayload.CLEAR) null else control.targetId
                            store.saveChatPrefs(chatId, cur.copy(pinnedMessageId = next))
                            if (openChatId() == chatId) {
                                _state.value = _state.value.copy(pinnedMessageId = next)
                            }
                        }
                    }
                    reaction != null -> {
                        val target = store.message(reaction.targetId)
                        val gid = JsonIds.optional(target?.groupId)
                            ?: target?.peerDeviceId?.takeIf { ChatIds.isGroup(it) }?.let { ChatIds.rawGroupId(it) }
                        val members = gid?.let { store.group(it)?.members }
                        if (ChatControlRules.allowReact(sender.deviceId, target, members)) {
                            store.applyReaction(
                                reaction.targetId,
                                reaction.emoji,
                                sender.deviceId,
                                sender.displayName,
                                reaction.op == ReactionPayload.CLEAR,
                            )
                        }
                    }
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
                        .put("payload", if (body.has("payload")) body.opt("payload") else ""),
                    sealed = true,
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
        if (SavedMessagesRules.isLocalObject(payload.objectId)) return
        if (SavedMessagesRules.skipNetwork(store.message(messageId)?.peerDeviceId)) return
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
            notice("не скачалось вложение")
        }
    }

    private fun handleCallEvent(obj: JSONObject, sealed: Boolean = false) {
        val from = PeerIds.normalize(obj.optString("from"))
        val event = CallSignal.parseEvent(obj.optString("event")).orEmpty()
        val callId = JsonIds.optional(obj.optString("call_id")).orEmpty()
        if (from.isBlank() || callId.isBlank() || event.isBlank()) return
        if (!sealed && !CallSignal.trustsPlainWss(event)) return
        val resolved = resolveCallPeer(from, _state.value.peer)
        val name = resolved?.displayName
            ?: _state.value.devices.find { PeerIds.same(it.deviceId, from) }?.displayName
            ?: from.take(8)
        if (name.isNotBlank()) callPeerName = name
        applyCallEffects(
            callMachine.onWire(from, event, callId, obj.opt("payload"), identity?.deviceId().orEmpty()),
        )
    }

    private fun applyCallEffects(effects: List<CallEffect>) {
        publishCall()
        for (effect in effects) {
            when (effect) {
                is CallEffect.Send -> {
                    val sent = dispatchCall(effect.callId, effect.peerId, effect.event, effect.payload)
                    if (!sent && effect.event == CallSignal.RING) {
                        applyCallEffects(callMachine.onRingSendFailed())
                        return
                    }
                }
                is CallEffect.StartRtc -> {
                    stopTone()
                    if (!callMachine.state.wssMedia) {
                        audioMode(true)
                        if (effect.video) {
                            _state.value = _state.value.copy(callSpeakerOn = true)
                            CallAudio.setSpeaker(app, true)
                        }
                        scope.launch { startRtc(effect.asCaller, effect.video) }
                    }
                }
                is CallEffect.DeliverRemote -> {
                    if (callMachine.state.wssMedia) {
                        // serial fallback: ignore leftover SDP/ICE
                    } else {
                        val session = rtc
                        effect.signals.forEach { sig ->
                            if (sig.kind == CallSignal.ICE) {
                                callMachine.onLocalCandidate(CallMedia.isRelayCandidate(sig.candidate))
                            }
                            session?.handleRemote(sig)
                        }
                    }
                }
                CallEffect.StartWssMedia -> {
                    stopTone()
                    cancelOneWayWatch()
                    startWssMedia()
                }
                is CallEffect.DeliverAudio -> playWssAudio(effect.signals)
                CallEffect.RestartIce -> if (!callMachine.state.wssMedia) rtc?.restartIce()
                CallEffect.FallbackDirect -> if (!callMachine.state.wssMedia) rtc?.allowDirect()
                CallEffect.TearDown -> teardownCall()
                CallEffect.RingOut -> {
                    val s = callMachine.state
                    if (CallToneRules.shouldPlayRing(s.phase, s.link, s.mediaUp) &&
                        CallToneRules.shouldRingOutgoing(_state.value.notificationsMuted)
                    ) {
                        startTone(true)
                    }
                    audioMode(true)
                }
                CallEffect.RingIn -> {
                    val s = callMachine.state
                    val chatMuted = store.chatPrefs(s.peerDeviceId).muted
                    if (CallToneRules.shouldPlayRing(s.phase, s.link, s.mediaUp) &&
                        CallToneRules.shouldRingIncoming(_state.value.notificationsMuted, chatMuted)
                    ) {
                        startTone(false)
                    }
                    audioMode(true)
                }
                CallEffect.StopTone -> stopTone()
                CallEffect.ClearNotify -> notifier.clearCall()
                is CallEffect.Record -> recordCall(
                    effect.peerId,
                    VideoCallRules.recordLabel(callMachine.state.video, effect.outgoing),
                    effect.outgoing,
                )
                CallEffect.NotifyIncoming -> {
                    if (CallToneRules.shouldNotifyIncoming(_state.value.notificationsMuted)) {
                        notifier.incomingCall(callPeerName)
                    }
                }
                CallEffect.PrefetchIce -> prefetchIce()
                CallEffect.WatchConnect -> watchConnecting()
                CallEffect.WatchRing -> watchRing()
                CallEffect.CancelWatch -> {
                    connectWatch?.cancel()
                    connectWatch = null
                    ringWatch?.cancel()
                    ringWatch = null
                }
                is CallEffect.Notice -> notice(effect.message)
            }
            if (effect !is CallEffect.TearDown) publishCall()
        }
    }

    private fun publishCall() {
        val info = callMachine.snapshot(callPeerName)
        if (info != null) {
            _state.value = _state.value.copy(call = info)
        }
    }

    private fun dispatchCall(callId: String, peerId: String, event: String, payload: String): Boolean {
        val peer = resolveCallPeer(peerId, _state.value.peer)
        val to = PeerIds.wireId(peer, peerId)
        if (to.isBlank()) return false
        val wss = CallSignal.trustsPlainWss(event) && sendCall(callId, to, event, payload)
        if (!CallSignal.skipMailbox(event) && peer != null && peer.publicIdentity.isNotEmpty()) {
            sendCallEnvelope(peer, callId, event, payload)
            return true
        }
        return wss
    }

    private suspend fun startRtc(asCaller: Boolean, video: Boolean) {
        val ice = awaitIce()
        withContext(Dispatchers.Main) {
            if (!callMachine.state.live || !callMachine.state.rtcWanted) return@withContext
            if (callMachine.state.wssMedia) return@withContext
            val hasTurn = !CallLink.missingTurn(ice)
            applyCallEffects(callMachine.onHasTurn(hasTurn, CallLink.missingTurnDetail()))
            if (!callMachine.state.live) return@withContext
            attachRtc(asCaller, ice, video || callMachine.state.video)
            applyCallEffects(callMachine.onSessionAttached())
        }
    }

    private suspend fun awaitIce(): List<IceServerSpec> {
        var profile = store.profile() ?: _state.value.profile
        repeat(2) { attempt ->
            val updated = withContext(Dispatchers.IO) {
                profile?.let { refreshIceServers(it) } ?: profile
            }
            if (updated != null) {
                store.saveProfile(updated)
                _state.value = _state.value.copy(profile = updated)
                profile = updated
            }
            val parsed = IceServers.parse(profile?.iceServersJson)
            val resolved = IceServers.resolve(parsed, profile?.host)
            val hasTurn = !IceServers.missingTurn(resolved)
            val staleOrEmpty = IceServers.shouldRefresh(
                iceCachedAtMs,
                System.currentTimeMillis(),
                profile?.iceServersJson,
            )
            if (hasTurn && !staleOrEmpty) return resolved
            if (attempt == 1) return resolved
            delay(400)
        }
        return IceServers.resolve(emptyList(), profile?.host)
    }

    private fun startWssMedia() {
        audioMode(true)
        synchronized(rtcLock) {
            try {
                rtc?.close()
            } catch (_: Exception) {
            }
            rtc = null
            _state.value = _state.value.copy(callRtcReady = false)
            if (wssAudio != null) {
                if (_state.value.callMicMuted) wssAudio?.setMuted(true)
                return
            }
            CallAudio.apply(app, true)
            if (_state.value.callSpeakerOn) CallAudio.setSpeaker(app, true)
            wssAudio = WssAudioSession(app) { pcm ->
                if (!wssFrameBusy.compareAndSet(false, true)) return@WssAudioSession
                scope.launch {
                    try {
                        sendWssAudioFrame(pcm)
                    } finally {
                        wssFrameBusy.set(false)
                    }
                }
            }
            if (_state.value.callMicMuted) wssAudio?.setMuted(true)
        }
    }

    private fun sendWssAudioFrame(pcm: ByteArray) {
        if (pcm.isEmpty() || !callMachine.state.live || !callMachine.state.wssMedia) return
        val id = identity ?: return
        val callId = callMachine.state.callId
        val peerId = callMachine.state.peerDeviceId
        if (callId.isBlank() || peerId.isBlank()) return
        val peer = resolveCallPeer(peerId, _state.value.peer) ?: return
        if (peer.publicIdentity.isEmpty()) return
        val to = PeerIds.wireId(peer, peerId)
        if (to.isBlank()) return
        try {
            val env = id.encryptTyped(
                publicIdentityFromBlob(peer.publicIdentity),
                EnvelopeTypes.CALL,
                pcm,
            )
            val payload = CallSignal(
                kind = CallSignal.AUDIO,
                frame = Base64.encodeToString(env.bytes, Base64.NO_WRAP),
            ).toJson()
            sendCall(callId, to, CallSignal.AUDIO, payload)
        } catch (_: Exception) {
        }
    }

    private fun playWssAudio(signals: List<CallSignal>) {
        val id = identity ?: return
        val peer = resolveCallPeer(callMachine.state.peerDeviceId, _state.value.peer) ?: return
        if (peer.publicIdentity.isEmpty()) return
        val session = wssAudio ?: return
        for (sig in signals) {
            if (sig.kind != CallSignal.AUDIO || sig.frame.isBlank()) continue
            val bytes = try {
                Base64.decode(sig.frame, Base64.NO_WRAP)
            } catch (_: Exception) {
                continue
            }
            if (bytes.isEmpty()) continue
            val pcm = try {
                val typed = id.decryptTyped(publicIdentityFromBlob(peer.publicIdentity), bytes)
                if (typed.msgType != EnvelopeTypes.CALL) continue
                typed.body
            } catch (_: Exception) {
                continue
            }
            session.play(pcm)
        }
    }

    private fun attachRtc(asCaller: Boolean, ice: List<IceServerSpec>, video: Boolean) {
        synchronized(rtcLock) {
            val existing = rtc
            if (existing != null) {
                if (_state.value.callMicMuted) existing.setMicEnabled(false)
                if (_state.value.callCamMuted) existing.setCameraEnabled(false)
                if (_state.value.callSpeakerOn) CallAudio.setSpeaker(app, true)
                boundRemote?.let { existing.attachRemoteSink(it) }
                boundLocal?.let { existing.attachLocalSink(it) }
                if (!_state.value.callRtcReady) {
                    _state.value = _state.value.copy(callRtcReady = true)
                }
                if (asCaller && !rtcAsCaller) {
                    rtcAsCaller = true
                    existing.createOffer()
                }
                return
            }
            val profile = store.profile() ?: _state.value.profile
            val session = try {
                WebRtcSession(
                    app,
                    iceServers = ice,
                    pinnedFingerprint = profile?.fingerprint.orEmpty(),
                    hintHost = IceServers.parseHostname(profile?.iceServersJson) ?: profile?.host,
                    publicIp = IceServers.parsePublicIp(profile?.iceServersJson),
                    polite = !asCaller,
                    wantVideo = video,
                    startCamera = VideoCallRules.shouldStartLocalCamera(video, _state.value.callCamMuted),
                    onLocalSignal = { sig ->
                        val callId = callMachine.state.callId
                        val peerId = callMachine.state.peerDeviceId
                        if (callId.isBlank() || peerId.isBlank()) return@WebRtcSession
                        val json = sig.toJson()
                        if (!VideoCallRules.fitsWss(json)) {
                            notice(VideoCallRules.sdpTooLargeNotice())
                            return@WebRtcSession
                        }
                        val peer = resolveCallPeer(peerId, _state.value.peer)
                        val to = PeerIds.wireId(peer, peerId)
                        if (to.isBlank()) return@WebRtcSession
                        if (CallSignal.trustsPlainWss(sig.kind)) {
                            sendCall(callId, to, sig.kind, json)
                        }
                        if (peer != null && peer.publicIdentity.isNotEmpty()) {
                            sendCallEnvelope(peer, callId, sig.kind, json)
                        }
                        if (sig.kind == CallSignal.OFFER) {
                            applyCallEffects(callMachine.onLocalOfferSent())
                        }
                        if (sig.kind == CallSignal.ICE) {
                            applyCallEffects(callMachine.onLocalCandidate(CallMedia.isRelayCandidate(sig.candidate)))
                        }
                    },
                    onIce = { name, viaRelay ->
                        applyCallEffects(callMachine.onIce(name, viaRelay))
                        if (callMachine.state.mediaUp && callMachine.state.video) {
                            watchOneWayVideo()
                        }
                    },
                    onCameraFailed = {
                        _state.value = _state.value.copy(
                            callCamMuted = true,
                            callNotice = VideoCallRules.cameraFailedNotice(),
                        )
                    },
                    onRemoteVideo = { onRemoteVideoBound() },
                    onLocalMirror = { mirrored ->
                        _state.value = _state.value.copy(callLocalMirrored = mirrored)
                    },
                )
            } catch (e: Exception) {
                notice(UserFacing.NO_PATH)
                return
            }
            rtc = session
            rtcAsCaller = asCaller
            if (_state.value.callMicMuted) session.setMicEnabled(false)
            if (_state.value.callCamMuted) session.setCameraEnabled(false)
            if (_state.value.callSpeakerOn) CallAudio.setSpeaker(app, true)
            boundRemote?.let { session.attachRemoteSink(it) }
            boundLocal?.let { session.attachLocalSink(it) }
            _state.value = _state.value.copy(callRtcReady = true)
            if (asCaller) session.createOffer() else session.prepareCallee()
        }
    }

    private fun watchConnecting() {
        connectWatch?.cancel()
        ringWatch?.cancel()
        ringWatch = null
        connectWatch = scope.launch {
            while (true) {
                delay(1_000L)
                val started = callMachine.state.connectStartedAtMs
                if (started <= 0L || !callMachine.state.live) return@launch
                if (callMachine.state.phase != CallPhase.ACTIVE) return@launch
                val elapsed = System.currentTimeMillis() - started
                applyCallEffects(callMachine.onConnectTick(elapsed))
                val link = callMachine.state.link
                if (link == CallLinkState.CONNECTED || link == CallLinkState.FAILED) return@launch
                if (!callMachine.state.live || callMachine.state.phase != CallPhase.ACTIVE) return@launch
            }
        }
    }

    private fun watchRing() {
        ringWatch?.cancel()
        ringWatch = scope.launch {
            delay(CallLink.RING_TIMEOUT_MS)
            applyCallEffects(callMachine.onRingTimeout())
        }
    }

    private fun cancelOneWayWatch() {
        oneWayWatch?.cancel()
        oneWayWatch = null
    }

    private fun watchOneWayVideo() {
        if (!callMachine.state.video || callMachine.state.wssMedia) {
            cancelOneWayWatch()
            return
        }
        if (rtc?.hasRemoteVideo() == true) {
            cancelOneWayWatch()
            return
        }
        if (oneWayWatch?.isActive == true) return
        oneWayWatch = scope.launch {
            delay(VideoCallRules.ONE_WAY_VIDEO_MS)
            if (!isActive) return@launch
            val s = callMachine.state
            val bound = rtc?.hasRemoteVideo() == true
            val incoming = VideoCallRules.oneWayVideoNotice(
                video = s.video,
                mediaUp = s.mediaUp,
                wssFallback = s.wssMedia,
                remoteVideoBound = bound,
                connectedForMs = VideoCallRules.ONE_WAY_VIDEO_MS,
            ) ?: return@launch
            val st = _state.value
            if (st.call == null) return@launch
            val next = VideoCallRules.keepExistingOverlayNotice(st.callNotice, incoming)
            if (next != st.callNotice) {
                _state.value = st.copy(callNotice = next)
            }
        }
    }

    private fun onRemoteVideoBound() {
        cancelOneWayWatch()
        val st = _state.value
        if (st.callNotice == VideoCallRules.oneWayRemoteNotice()) {
            _state.value = st.copy(callNotice = null)
        }
    }

    private fun sendCallEnvelope(peer: DirectoryDevice, callId: String, event: String, payload: String = "") {
        val id = identity ?: return
        if (peer.publicIdentity.isEmpty()) return
        if (payload.isNotEmpty() && !VideoCallRules.fitsWss(payload)) {
            notice(VideoCallRules.sdpTooLargeNotice())
            return
        }
        scope.launch {
            try {
                val body = CallSignal.envelopeJson(callId, event, payload).toByteArray()
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

    private fun sendCall(callId: String, to: String, event: String, payload: String): Boolean {
        val dest = PeerIds.normalize(to)
        if (dest.isBlank() || ChatIds.isGroup(to)) return false
        if (payload.isNotEmpty() && !VideoCallRules.fitsWss(payload)) {
            notice(VideoCallRules.sdpTooLargeNotice())
            return false
        }
        return socket?.send(
            JSONObject()
                .put("type", "call")
                .put("call_id", callId)
                .put("to", dest)
                .put("event", event)
                .put("payload", payload)
                .toString(),
        ) == true
    }

    private fun resolveCallPeer(
        rawId: String,
        hint: DirectoryDevice? = null,
        fetch: Boolean = false,
    ): DirectoryDevice? {
        val found = PeerIds.resolve(_state.value.devices, hint, rawId, _state.value.onlineIds)
        if (found != null || !fetch) return found
        return runCatching {
            val devices = api?.directory().orEmpty()
            val online = _state.value.onlineIds
            val mapped = devices
                .filter { !PeerIds.same(it.deviceId, identity?.deviceId()) }
                .map { it.copy(online = it.online || PeerIds.normalize(it.deviceId) in online) }
            if (mapped.isNotEmpty()) {
                _state.value = _state.value.copy(devices = mapped)
            }
            PeerIds.resolve(mapped, hint, rawId, online)
        }.getOrNull()
    }

    private fun teardownCall() {
        connectWatch?.cancel()
        connectWatch = null
        ringWatch?.cancel()
        ringWatch = null
        cancelOneWayWatch()
        rtcAsCaller = false
        callPeerName = ""
        boundRemote = null
        boundLocal = null
        callMachine.reset()
        stopTone()
        audioMode(false)
        notifier.clearCall()
        synchronized(rtcLock) {
            try {
                rtc?.close()
            } catch (_: Exception) {
            }
            rtc = null
            try {
                wssAudio?.close()
            } catch (_: Exception) {
            }
            wssAudio = null
        }
        _state.value = _state.value.copy(
            call = null,
            callMicMuted = false,
            callSpeakerOn = false,
            callCamMuted = false,
            callLocalMirrored = true,
            callNotice = null,
            callRtcReady = false,
        )
    }

    private fun startTone(outgoing: Boolean) {
        onMain {
            if (tone != null && toneOutgoing == outgoing) return@onMain
            stopToneLocked()
            val stream = if (outgoing) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_RING
            tone = ToneGenerator(stream, 80).also {
                it.startTone(ToneGenerator.TONE_SUP_RINGTONE, 30_000)
            }
            toneOutgoing = outgoing
        }
    }

    private fun stopTone() {
        onMain { stopToneLocked() }
    }

    private fun stopToneLocked() {
        try {
            tone?.stopTone()
            tone?.release()
        } catch (_: Exception) {
        }
        tone = null
        toneOutgoing = null
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun audioMode(on: Boolean) {
        CallAudio.apply(app, on)
    }

    private fun notifyIfHidden(title: String, body: String, chatId: String, albumId: String? = null) {
        if (SavedMessagesRules.isSaved(chatId)) return
        val chatOpen = _state.value.screen == Screen.Chat && openChatId() == chatId
        val appForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val cur = store.chatPrefs(chatId)
        if (!chatOpen || !appForeground) {
            store.saveChatPrefs(chatId, cur.copy(unread = cur.unread + 1))
            refreshConversations()
        }
        if (NotifyRules.shouldAlert(chatOpen, appForeground, cur.muted, _state.value.notificationsMuted)) {
            notifier.message(
                title,
                body,
                AlbumRules.notifyId(body, albumId),
                sound = _state.value.notifySound,
            )
        }
    }

    private fun ack(messageId: String) {
        socket?.send(JSONObject().put("type", "ack").put("message_id", messageId).toString())
    }

    private fun findSender(senderId: String): DirectoryDevice? {
        PeerIds.findDevice(_state.value.devices, senderId)?.let { return it }
        PeerIds.preferReachable(PeerIds.devicesForMember(_state.value.devices, senderId), _state.value.onlineIds)
            ?.let { return it }
        return try {
            val devices = api?.directory().orEmpty()
            val online = _state.value.onlineIds
            val mapped = devices.map {
                it.copy(online = it.online || PeerIds.normalize(it.deviceId) in online)
            }
            _state.value = _state.value.copy(
                devices = mapped.filter { !PeerIds.same(it.deviceId, identity?.deviceId()) },
            )
            PeerIds.findDevice(mapped, senderId)
                ?: PeerIds.preferReachable(PeerIds.devicesForMember(mapped, senderId), online)
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
            if (SavedMessagesRules.skipNetwork(msg.peerDeviceId)) continue
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
        if (!PublicBackupRules.allowApkCopy) return
        runCatching {
            PublicDownloads.write(app, assetName, "application/vnd.android.package-archive", apk.readBytes())
        }
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

    private fun notice(msg: String) {
        val text = UserFacing.of(msg)
        if (VideoCallRules.noticeUsesOverlay(_state.value.call != null)) {
            _state.value = _state.value.copy(busy = false, error = null, callNotice = text)
        } else {
            _state.value = _state.value.copy(busy = false, error = text, notice = text)
        }
    }

    private fun error(e: Exception) {
        notice(UserFacing.of(e))
    }
}
