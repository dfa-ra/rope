package app.rope.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File

class MainActivity : AppCompatActivity() {
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@registerForActivityResult
        (application as RopeApp).repo.prepareJoin(text)
    }

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { (application as RopeApp).repo.sendAttachment(it) }
    }

    private val audioPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) (application as RopeApp).repo.startVoice()
    }

    private val notifyPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repo = (application as RopeApp).repo
        repo.start(intent?.data?.toString())
        requestNotifications()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                repo.resume()
            }
        })
        setContent {
            val state by repo.state.collectAsState()
            LaunchedEffect(state.pendingApkPath) {
                val path = state.pendingApkPath ?: return@LaunchedEffect
                installApk(File(path))
                repo.consumePendingApk()
            }
            RopeTheme {
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
                    onAttach = { picker.launch("*/*") },
                    onVoiceStart = {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            repo.startVoice()
                        } else {
                            audioPerm.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onVoiceFinish = repo::finishVoice,
                    onCall = repo::startCall,
                    onPlay = repo::toggleVoice,
                    onNewGroup = { repo.go(Screen.NewGroup) },
                    onGroupName = repo::setGroupName,
                    onToggleMember = repo::toggleMember,
                    onCreateGroup = repo::createGroup,
                    onAddMember = repo::addMemberToOpenGroup,
                    onRemoveMember = repo::removeMemberFromOpenGroup,
                    onAcceptCall = repo::acceptCall,
                    onRejectCall = repo::rejectCall,
                    onHangup = repo::hangup,
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

    private fun ensureInstallPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")),
            )
            return false
        }
        return true
    }

    private fun installApk(file: File) {
        if (!ensureInstallPermission()) return
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}
