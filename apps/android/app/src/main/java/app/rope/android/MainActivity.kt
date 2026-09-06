package app.rope.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repo = (application as RopeApp).repo
        repo.start(intent?.data?.toString())
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
                    onOpenChat = repo::openChat,
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
                )
            }
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
