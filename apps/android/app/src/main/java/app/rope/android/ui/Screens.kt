package app.rope.android

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.MessageStatus
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RopeScaffold(
    state: UiState,
    onGo: (Screen) -> Unit,
    onProvision: (String, Int, String, String, String, Int, String, String, Boolean) -> Unit,
    onJoin: (String, String) -> Unit,
    onJoinDev: (String, Int, String, String) -> Unit,
    onOpenChat: (DirectoryDevice) -> Unit,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onInvite: () -> Unit,
    onStatus: () -> Unit,
    onScan: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.offline) "Rope · offline" else "Rope") },
                actions = {
                    if (state.profile != null) {
                        TextButton(onClick = onInvite) { Text("Invite") }
                        TextButton(onClick = onStatus) { Text("Server") }
                        TextButton(onClick = { onGo(Screen.Chats) }) { Text("Chats") }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.busy) CircularProgressIndicator()
            when (state.screen) {
                Screen.Start -> StartPane(onGo)
                Screen.Provision -> ProvisionPane(onProvision, { onGo(Screen.Start) })
                Screen.Join -> JoinPane(onJoin, onJoinDev, onScan, { onGo(Screen.Start) })
                Screen.Chats -> ChatsPane(state, onOpenChat)
                Screen.Chat -> ChatPane(state, onDraft, onSend)
                Screen.Invite -> InvitePane(state.inviteUrl.orEmpty())
                Screen.Status -> StatusPane(state.statusText) { onGo(Screen.Provision) }
                Screen.Settings -> Text("Settings")
            }
        }
    }
}

@Composable
private fun StartPane(onGo: (Screen) -> Unit) {
    Text("Private self-hosted messenger", style = MaterialTheme.typography.headlineSmall)
    Text("Create a server on your VPS or join with a QR invite.")
    Button(onClick = { onGo(Screen.Provision) }, modifier = Modifier.fillMaxWidth()) { Text("Create server") }
    Button(onClick = { onGo(Screen.Join) }, modifier = Modifier.fillMaxWidth()) { Text("Join with QR / link") }
}

@Composable
private fun ProvisionPane(
    onProvision: (String, Int, String, String, String, Int, String, String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("root") }
    var password by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var listen by remember { mutableStateOf("8443") }
    var name by remember { mutableStateOf("owner") }
    var url by remember { mutableStateOf("https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64") }
    OutlinedTextField(host, { host = it }, label = { Text("VPS host") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(sshPort, { sshPort = it }, label = { Text("SSH port") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(user, { user = it }, label = { Text("SSH user") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(password, { password = it }, label = { Text("SSH password") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(key, { key = it }, label = { Text("SSH private key (optional)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(listen, { listen = it }, label = { Text("Rope port") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(url, { url = it }, label = { Text("Server binary URL") }, modifier = Modifier.fillMaxWidth())
    Button(
        onClick = {
            onProvision(host, sshPort.toIntOrNull() ?: 22, user, password, key, listen.toIntOrNull() ?: 8443, url, name, false)
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Install and connect") }
    Button(
        onClick = {
            onProvision(host, sshPort.toIntOrNull() ?: 22, user, password, key, listen.toIntOrNull() ?: 8443, url, name, true)
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Update server core (keep data)") }
    Text("SSH нужен только для установки/обновления. После успеха пароль на сервер мессенджера не уходит.", style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = onBack) { Text("Back") }
}

@Composable
private fun StatusPane(statusText: String, onUpgrade: () -> Unit) {
    Text(if (statusText.isBlank()) "Server status" else statusText)
    Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) { Text("Update server core") }
    Text(
        "Обновление скачает свежий rope-server с GitHub Releases и заменит бинарник. data.db и TLS сохраняются.",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun JoinPane(
    onJoin: (String, String) -> Unit,
    onJoinDev: (String, Int, String, String) -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("guest") }
    var host by remember { mutableStateOf("10.0.2.2") }
    var port by remember { mutableStateOf("8443") }
    var token by remember { mutableStateOf("") }
    OutlinedTextField(url, { url = it }, label = { Text("rope://join?...") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { onJoin(url, name) }, modifier = Modifier.fillMaxWidth()) { Text("Join link") }
    Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scan QR") }
    Text("Debug HTTP (emulator)", style = MaterialTheme.typography.labelLarge)
    OutlinedTextField(host, { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(token, { token = it }, label = { Text("setup/invite token") }, modifier = Modifier.fillMaxWidth())
    Button(
        onClick = { onJoinDev(host, port.toIntOrNull() ?: 8443, token, name) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Join debug HTTP") }
    TextButton(onClick = onBack) { Text("Back") }
}

@Composable
private fun ChatsPane(state: UiState, onOpen: (DirectoryDevice) -> Unit) {
    if (state.devices.isEmpty()) {
        Text("No other devices yet. Create an invite.")
    }
    LazyColumn {
        items(state.devices, key = { it.deviceId }) { d ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(d) }.padding(vertical = 8.dp)) {
                Text(d.displayName.ifBlank { d.deviceId.take(12) }, style = MaterialTheme.typography.titleMedium)
                Text(d.deviceId.take(16), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChatPane(state: UiState, onDraft: (String) -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(state.peer?.displayName ?: "chat", style = MaterialTheme.typography.titleLarge)
    LazyColumn(Modifier.weight(1f, fill = true)) {
        items(state.messages, key = { it.id }) { m ->
            val who = if (m.outgoing) "You" else "Them"
            val mark = when (m.status) {
                MessageStatus.CREATED -> "·"
                MessageStatus.SENT_TO_SERVER -> "✓"
                MessageStatus.DELIVERED_TO_DEVICE -> "✓✓"
            }
            Text("$who $mark  ${m.text}")
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(state.draftText, onDraft, modifier = Modifier.weight(1f), label = { Text("Message") })
        Button(onClick = onSend) { Text("Send") }
    }
    }
}

@Composable
private fun InvitePane(url: String) {
    Text("Show this QR to the guest. One-time, expires.")
    if (url.isNotBlank()) {
        Image(bitmap = qrBitmap(url).asImageBitmap(), contentDescription = "invite", modifier = Modifier.size(240.dp))
        Text(url, style = MaterialTheme.typography.bodySmall)
    }
}

private fun qrBitmap(text: String): Bitmap {
    val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
    val bmp = Bitmap.createBitmap(bits.width, bits.height, Bitmap.Config.RGB_565)
    for (x in 0 until bits.width) {
        for (y in 0 until bits.height) {
            bmp.setPixel(x, y, if (bits[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bmp
}
