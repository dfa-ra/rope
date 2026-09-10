package app.rope.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.rope.android.data.CallMediaStart
import app.rope.android.data.VideoCallRules
import app.rope.android.update.ApkInstaller
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File

class MainActivity : AppCompatActivity() {
    private var waitingForInstallPerm = false
    private var startedInstallFor: String? = null
    private val composeReady = java.util.concurrent.atomic.AtomicBoolean(false)

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@registerForActivityResult
        (application as RopeApp).repo.prepareJoin(text)
    }

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { (application as RopeApp).repo.sendAttachment(it) }
    }

    private val galleryPicker = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(app.rope.android.data.AlbumRules.MAX_PHOTOS),
    ) { uris ->
        if (uris.isNotEmpty()) (application as RopeApp).repo.stageAttachments(uris)
    }

    private val restorePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val bytes = uri?.let { contentResolver.openInputStream(it)?.use { s -> s.readBytes() } } ?: return@registerForActivityResult
        (application as RopeApp).repo.restoreFromFile(bytes)
    }

    private var afterAudio: String? = null

    private val audioPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val next = afterAudio
        afterAudio = null
        val repo = (application as RopeApp).repo
        if (!granted) {
            repo.micDenied()
            return@registerForActivityResult
        }
        when (next) {
            "call" -> repo.startCall()
            "video" -> repo.startVideoCall()
            "accept" -> repo.acceptCall()
            else -> repo.startVoice()
        }
    }

    private val callMediaPerm = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val next = afterAudio
        afterAudio = null
        val repo = (application as RopeApp).repo
        val mic = granted[Manifest.permission.RECORD_AUDIO] == true || hasMic()
        val cam = granted[Manifest.permission.CAMERA] == true || hasCam()
        when (next) {
            "video" -> when (VideoCallRules.afterOutgoingVideoPermission(mic, cam)) {
                CallMediaStart.ABORT -> {
                    if (!mic) repo.micDenied()
                }
                CallMediaStart.VIDEO -> repo.startVideoCall()
                CallMediaStart.AUDIO -> {
                    repo.startCall()
                    repo.cameraDenied()
                }
            }
            "accept" -> {
                if (!VideoCallRules.proceedIncoming(mic)) {
                    repo.micDenied()
                    return@registerForActivityResult
                }
                if (VideoCallRules.incomingCameraMuted(cam)) repo.cameraDenied()
                repo.acceptCall()
            }
            "unmute-cam" -> {
                if (!VideoCallRules.applyUnmuteCamResult(repo.state.value.call != null)) {
                    return@registerForActivityResult
                }
                if (cam) repo.toggleCallCamera() else repo.cameraDenied()
            }
            "video-note" -> {
                if (!mic) {
                    repo.micDenied()
                    return@registerForActivityResult
                }
                if (!cam) {
                    repo.cameraDenied()
                    return@registerForActivityResult
                }
                repo.startVideoNote()
            }
        }
    }

    private val notifyPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val installSources = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        waitingForInstallPerm = false
        tryInstallPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !composeReady.get() }
        val repo = (application as RopeApp).repo
        repo.start(intent?.data?.toString())
        requestNotifications()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                repo.resume()
                if (canInstallPackages() && startedInstallFor == null) tryInstallPending()
            }
        })
        setContent {
            val state by repo.state.collectAsState()
            LaunchedEffect(Unit) { composeReady.set(true) }
            LaunchedEffect(state.pendingApkPath, state.installTick) {
                startedInstallFor = null
                if (state.pendingApkPath != null) tryInstallPending()
            }
            LaunchedEffect(state.screen) {
                if (SecureDisplayRules.lockRecents(state.screen)) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            RopeTheme(mode = state.theme) {
                RopeScaffold(
                    state = state,
                    onGo = repo::go,
                    onProvision = repo::provision,
                    onJoin = repo::join,
                    onJoinDev = repo::joinDevHttp,
                    onOpenConversation = repo::openConversation,
                    onDraft = repo::setDraft,
                    onSend = repo::sendDraft,
                    onInvite = repo::createInvite,
                    onStatus = repo::refreshStatus,
                    onScan = {
                        scanner.launch(
                            ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt("Scan Rope invite"),
                        )
                    },
                    onUpdateApp = {
                        if (!ensureInstallPermission()) return@RopeScaffold
                        repo.updateApp()
                    },
                    onUpgradeCore = repo::upgradeCore,
                    onRestoreBackup = { restorePicker.launch("*/*") },
                    onAttach = { picker.launch("*/*") },
                    onAttachGallery = {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                        )
                    },
                    onAttachFile = { picker.launch("*/*") },
                    onAttachUri = { (application as RopeApp).repo.sendAttachment(it) },
                    onAttachUris = { (application as RopeApp).repo.stageAttachments(it) },
                    onVoiceStart = { withMic("voice") { repo.startVoice() } },
                    onVoiceFinish = repo::finishVoice,
                    onSeekVoice = repo::seekVoice,
                    onCycleVoiceSpeed = repo::cycleVoiceSpeed,
                    onVideoNoteStart = { withNoteMedia { repo.startVideoNote() } },
                    onVideoNoteFinish = repo::finishVideoNote,
                    onVideoNotePreview = repo::bindVideoNotePreview,
                    onVideoNotePreviewGone = repo::unbindVideoNotePreview,
                    onCall = { withMic("call") { repo.startCall() } },
                    onVideoCall = { withCallMedia("video") { repo.startVideoCall() } },
                    onPlay = repo::toggleVoice,
                    onReact = repo::react,
                    onEnsureMedia = repo::ensureMedia,
                    onGroupName = repo::setGroupName,
                    onToggleMember = repo::toggleMember,
                    onCreateGroup = repo::createGroup,
                    onAddMember = repo::addMemberToOpenGroup,
                    onRemoveMember = repo::removeMemberFromOpenGroup,
                    onLeaveGroup = repo::leaveOpenGroup,
                    onRevokeMember = repo::revokeMember,
                    onAcceptCall = {
                        val video = (application as RopeApp).repo.state.value.call?.video == true
                        if (video) withCallMedia("accept") { repo.acceptCall() }
                        else withMic("accept") { repo.acceptCall() }
                    },
                    onRejectCall = repo::rejectCall,
                    onHangup = repo::hangup,
                    onToggleCallMute = repo::toggleCallMute,
                    onToggleCallSpeaker = repo::toggleCallSpeaker,
                    onToggleCallCamera = {
                        val r = (application as RopeApp).repo
                        val muted = r.state.value.callCamMuted
                        if (VideoCallRules.inCallUnmuteNeedsCameraPermission(muted, hasCam())) {
                            afterAudio = "unmute-cam"
                            callMediaPerm.launch(arrayOf(Manifest.permission.CAMERA))
                        } else {
                            r.toggleCallCamera()
                        }
                    },
                    onFlipCallCamera = repo::flipCallCamera,
                    callEgl = { (application as RopeApp).repo.callEglContext() },
                    onBindCallRemote = { (application as RopeApp).repo.bindCallRemote(it) },
                    onBindCallLocal = { (application as RopeApp).repo.bindCallLocal(it) },
                    onUnbindCallRemote = { (application as RopeApp).repo.unbindCallRemote(it) },
                    onUnbindCallLocal = { (application as RopeApp).repo.unbindCallLocal(it) },
                    onToggleTheme = repo::toggleTheme,
                    onSetTheme = repo::setTheme,
                    onToggleNotifications = repo::toggleNotificationsMuted,
                    onToggleLinkPreviews = repo::toggleLinkPreviews,
                    onCopyText = repo::copyText,
                    onReply = repo::startReply,
                    onReplySpan = repo::setReplySpan,
                    onEdit = repo::startEdit,
                    onDelete = repo::deleteMessage,
                    onForward = repo::startForward,
                    onCancelComposer = repo::cancelComposerExtra,
                    onDismissLinkPreview = repo::dismissComposerPreview,
                    onCancelPendingMedia = repo::cancelPendingMedia,
                    onCancelForward = repo::cancelForward,
                    onChatQuery = repo::setChatQuery,
                    onMessageQuery = repo::setMessageQuery,
                    onPinChat = repo::togglePinChat,
                    onMuteChat = repo::toggleMuteChat,
                    onArchiveChat = repo::archiveChat,
                    onUnarchiveChat = repo::unarchiveChat,
                    onCopy = repo::copyMessage,
                    onCopySelected = repo::copyMessages,
                    onPinMessage = repo::togglePinMessage,
                    onJump = repo::jumpToMessage,
                    onOpenImage = repo::openImage,
                    onCloseImage = repo::closeImage,
                    onConsumedScroll = repo::consumeScrollTo,
                    onDismissNotice = repo::dismissNotice,
                    onBack = repo::goBack,
                    onTab = { repo.go(it, tab = true) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun tryInstallPending() {
        val repo = (application as RopeApp).repo
        val path = repo.state.value.pendingApkPath ?: return
        if (!canInstallPackages()) {
            ensureInstallPermission()
            return
        }
        if (startedInstallFor == path) return
        startedInstallFor = path
        try {
            ApkInstaller(this).install(File(path))
        } catch (e: Exception) {
            startedInstallFor = null
            repo.onApkInstallFailed(e.message ?: "не удалось начать установку", -1)
        }
    }

    private fun withNoteMedia(granted: () -> Unit) {
        if (hasMic() && hasCam()) {
            granted()
        } else {
            afterAudio = "video-note"
            callMediaPerm.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
            )
        }
    }

    private fun withMic(action: String, granted: () -> Unit) {
        if (hasMic()) {
            granted()
        } else {
            afterAudio = action
            audioPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun withCallMedia(action: String, granted: () -> Unit) {
        val mic = hasMic()
        val cam = hasCam()
        when {
            mic && cam -> granted()
            action == "accept" && mic -> {
                afterAudio = action
                callMediaPerm.launch(arrayOf(Manifest.permission.CAMERA))
            }
            else -> {
                afterAudio = action
                callMediaPerm.launch(
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
                )
            }
        }
    }

    private fun hasMic(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasCam(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifyPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()

    private fun ensureInstallPermission(): Boolean {
        if (canInstallPackages()) return true
        waitingForInstallPerm = true
        installSources.launch(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")),
        )
        return false
    }
}
