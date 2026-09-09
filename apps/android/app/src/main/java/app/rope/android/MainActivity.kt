package app.rope.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import app.rope.android.update.ApkInstaller
import app.rope.android.update.DeviceBackup
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
        if (!granted) return@registerForActivityResult
        val repo = (application as RopeApp).repo
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
        if (granted[Manifest.permission.RECORD_AUDIO] != true) return@registerForActivityResult
        if (granted[Manifest.permission.CAMERA] != true) {
            repo.cameraDenied()
            return@registerForActivityResult
        }
        when (next) {
            "video" -> repo.startVideoCall()
            "accept" -> repo.acceptCall()
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
        repo.start(
            if (intent?.action == ApkInstaller.ACTION) null else intent?.data?.toString(),
        )
        handleInstallResult(intent)
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
                    onToggleCallCamera = repo::toggleCallCamera,
                    onFlipCallCamera = repo::flipCallCamera,
                    callEgl = { (application as RopeApp).repo.callEglContext() },
                    onBindCallRemote = { (application as RopeApp).repo.bindCallRemote(it) },
                    onBindCallLocal = { (application as RopeApp).repo.bindCallLocal(it) },
                    onToggleTheme = repo::toggleTheme,
                    onSetTheme = repo::setTheme,
                    onToggleNotifications = repo::toggleNotificationsMuted,
                    onCopyText = repo::copyText,
                    onReply = repo::startReply,
                    onReplySpan = repo::setReplySpan,
                    onEdit = repo::startEdit,
                    onDelete = repo::deleteMessage,
                    onForward = repo::startForward,
                    onCancelComposer = repo::cancelComposerExtra,
                    onCancelForward = repo::cancelForward,
                    onChatQuery = repo::setChatQuery,
                    onMessageQuery = repo::setMessageQuery,
                    onPinChat = repo::togglePinChat,
                    onMuteChat = repo::toggleMuteChat,
                    onCopy = repo::copyMessage,
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
        handleInstallResult(intent)
    }

    private fun handleInstallResult(intent: Intent?) {
        if (intent?.action != ApkInstaller.ACTION) return
        val repo = (application as RopeApp).repo
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> repo.onApkInstalled()
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    startActivity(confirm)
                } else {
                    repo.onApkInstallFailed("система не показала окно установки", status)
                }
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            -> repo.onApkInstallFailed(
                "Старая сборка подписана другим ключом CI. Ключ и логин лежат в Загрузках как ${DeviceBackup.FILE_NAME}. " +
                    "Удалите Rope, поставьте APK из Загрузок и на старте нажмите «Восстановить устройство».",
                status,
            )
            else -> repo.onApkInstallFailed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "установка не удалась ($status)",
                status,
            )
        }
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

    private fun withMic(action: String, granted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            granted()
        } else {
            afterAudio = action
            audioPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun withCallMedia(action: String, granted: () -> Unit) {
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        when {
            mic && cam -> granted()
            else -> {
                afterAudio = action
                callMediaPerm.launch(
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
                )
            }
        }
    }

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
