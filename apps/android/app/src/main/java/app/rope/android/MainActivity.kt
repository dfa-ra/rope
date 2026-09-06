package app.rope.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@registerForActivityResult
        (application as RopeApp).repo.join(text, "guest")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
            RopeTheme {
                RopeScaffold(
                    state = state,
                    onGo = repo::go,
                    onProvision = { host, sshPort, user, pass, key, port, url, name, upgrade ->
                        repo.provision(host, sshPort, user, pass, key, port, url, name, upgrade)
                    },
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
                )
            }
        }
    }
}
