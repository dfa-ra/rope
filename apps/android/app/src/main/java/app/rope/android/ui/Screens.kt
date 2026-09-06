package app.rope.android

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.MessageStatus
import app.rope.android.provision.ProvisionForm
import app.rope.android.provision.ServerTarget
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RopeScaffold(
    state: UiState,
    onGo: (Screen) -> Unit,
    onProvision: (ProvisionForm) -> Unit,
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
                        IconButton(onClick = onInvite) {
                            Icon(Icons.Outlined.QrCode, contentDescription = "Пригласить")
                        }
                        IconButton(onClick = onStatus) {
                            Icon(Icons.Outlined.Dns, contentDescription = "Сервер")
                        }
                        IconButton(onClick = { onGo(Screen.Chats) }) {
                            Icon(Icons.Outlined.Chat, contentDescription = "Чаты")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state.screen) {
                    Screen.Start -> StartPane(onGo)
                    Screen.Provision -> ProvisionPane(!state.busy, onProvision) { onGo(Screen.Start) }
                    Screen.Join -> JoinPane(!state.busy, onJoin, onJoinDev, onScan) { onGo(Screen.Start) }
                    Screen.Chats -> ChatsPane(state, onOpenChat)
                    Screen.Chat -> ChatPane(state, onDraft, onSend)
                    Screen.Invite -> InvitePane(state.inviteUrl.orEmpty())
                    Screen.Status -> StatusPane(state.statusText) { onGo(Screen.Provision) }
                    Screen.Settings -> Text("Settings", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StartPane(onGo: (Screen) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Личный мессенджер на своём VPS", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Поднимите сервер с телефона или войдите по QR от организатора.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onGo(Screen.Provision) }, modifier = Modifier.fillMaxWidth()) {
            Text("Создать сервер")
        }
        OutlinedButton(onClick = { onGo(Screen.Join) }, modifier = Modifier.fillMaxWidth()) {
            Text("Войти по QR или ссылке")
        }
    }
}

@Composable
private fun ProvisionPane(
    enabled: Boolean,
    onProvision: (ProvisionForm) -> Unit,
    onBack: () -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("root") }
    var password by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var listen by remember { mutableStateOf("8443") }
    var name by remember { mutableStateOf("owner") }
    var token by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(ServerTarget.AUTO) }
    var advanced by remember { mutableStateOf(false) }
    val canInstall = enabled && host.isNotBlank() && user.isNotBlank() && (password.isNotBlank() || key.isNotBlank())

    fun form(upgrade: Boolean) = ProvisionForm(
        host = host.trim(),
        sshPort = sshPort.toIntOrNull() ?: 22,
        user = user.trim(),
        password = password,
        keyPem = key,
        listenPort = listen.toIntOrNull() ?: 8443,
        target = target,
        binaryUrl = url.trim(),
        displayName = name.trim().ifBlank { "owner" },
        githubToken = token.trim(),
        upgrade = upgrade,
    )

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Создание сервера", style = MaterialTheme.typography.titleLarge)
            Text(
                "Нужен VPS с Ubuntu/Debian и SSH. После установки SSH больше не используется для чата.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                host,
                { host = it },
                label = { Text("IP или hostname VPS") },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    sshPort,
                    { sshPort = it },
                    label = { Text("SSH порт") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    user,
                    { user = it },
                    label = { Text("SSH пользователь") },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.weight(1.4f),
                )
            }
            OutlinedTextField(
                password,
                { password = it },
                label = { Text("SSH пароль") },
                singleLine = true,
                enabled = enabled,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { showKey = !showKey }, enabled = enabled) {
                Text(if (showKey) "Скрыть SSH-ключ" else "Вставить SSH-ключ вместо пароля")
            }
            if (showKey) {
                OutlinedTextField(
                    key,
                    { key = it },
                    label = { Text("SSH ключ (если нет пароля)") },
                    enabled = enabled,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp),
                )
            }
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Ваше имя в мессенджере") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            ArchPicker(target = target, enabled = enabled, onSelect = { target = it })
            TextButton(onClick = { advanced = !advanced }, enabled = enabled) {
                Text(if (advanced) "Скрыть доп. настройки" else "Дополнительно: порт, свой URL, обновление")
            }
            if (advanced) {
                OutlinedTextField(
                    listen,
                    { listen = it },
                    label = { Text("Порт Rope на VPS") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    url,
                    { url = it },
                    label = { Text("Свой URL бинарника (необязательно)") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    token,
                    { token = it },
                    label = { Text("GitHub token (приватный репозиторий)") },
                    singleLine = true,
                    enabled = enabled,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { onProvision(form(true)) },
                    enabled = canInstall,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Обновить ядро, данные сохранить") }
                Text(
                    "Токен GitHub остаётся на телефоне и на VPS не копируется.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        HorizontalDivider()
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onProvision(form(false)) },
                enabled = canInstall,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Установить и подключить") }
            TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Назад")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArchPicker(
    target: ServerTarget,
    enabled: Boolean,
    onSelect: (ServerTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Архитектура VPS", style = MaterialTheme.typography.titleSmall)
        Text(
            "На большинстве облаков — Linux x86_64. «Авто» спросит сервер по SSH.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServerTarget.entries.forEach { option ->
                FilterChip(
                    selected = target == option,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    label = { Text(option.title) },
                )
            }
        }
    }
}

@Composable
private fun StatusPane(statusText: String, onUpgrade: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Сервер", style = MaterialTheme.typography.titleLarge)
        Text(
            statusText.ifBlank { "Статус ещё не загружен." },
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Обновить ядро")
        }
        Text(
            "Скачает новый rope-server с GitHub Releases. База и сертификаты останутся.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun JoinPane(
    enabled: Boolean,
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
    var debug by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Вход", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Ваше имя") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                url,
                { url = it },
                label = { Text("Ссылка rope://join?...") },
                enabled = enabled,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { debug = !debug }, enabled = enabled) {
                Text(if (debug) "Скрыть отладку" else "Отладка: HTTP / эмулятор")
            }
            if (debug) {
                OutlinedTextField(host, { host = it }, label = { Text("Host") }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    port,
                    { port = it },
                    label = { Text("Port") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(token, { token = it }, label = { Text("setup / invite token") }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = { onJoinDev(host, port.toIntOrNull() ?: 8443, token, name) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Войти по HTTP") }
            }
        }
        HorizontalDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onScan, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Сканировать QR")
            }
            OutlinedButton(
                onClick = { onJoin(url, name) },
                enabled = enabled && url.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Войти по ссылке") }
            TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Назад")
            }
        }
    }
}

@Composable
private fun ChatsPane(state: UiState, onOpen: (DirectoryDevice) -> Unit) {
    if (state.devices.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Пока никого нет", style = MaterialTheme.typography.titleMedium)
            Text(
                "Нажмите Invite вверху и покажите QR второму телефону.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.devices, key = { it.deviceId }) { d ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(d) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(d.displayName.ifBlank { d.deviceId.take(12) }, style = MaterialTheme.typography.titleMedium)
                Text(d.deviceId.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ChatPane(state: UiState, onDraft: (String) -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            state.peer?.displayName ?: "Чат",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.messages, key = { it.id }) { m ->
                val who = if (m.outgoing) "Вы" else "Собеседник"
                val mark = when (m.status) {
                    MessageStatus.CREATED -> "·"
                    MessageStatus.SENT_TO_SERVER -> "✓"
                    MessageStatus.DELIVERED_TO_DEVICE -> "✓✓"
                }
                Text("$who $mark  ${m.text}", style = MaterialTheme.typography.bodyLarge)
            }
        }
        HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                state.draftText,
                onDraft,
                modifier = Modifier.weight(1f),
                label = { Text("Сообщение") },
            )
            Button(onClick = onSend, enabled = state.draftText.isNotBlank(), modifier = Modifier.height(56.dp)) {
                Text("OK")
            }
        }
    }
}

@Composable
private fun InvitePane(url: String) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Приглашение", style = MaterialTheme.typography.titleLarge)
        Text(
            "Покажите QR гостю. Ссылка одноразовая и сгорит по TTL.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (url.isNotBlank()) {
            Image(bitmap = qrBitmap(url).asImageBitmap(), contentDescription = "invite", modifier = Modifier.size(260.dp))
            Text(url, style = MaterialTheme.typography.bodySmall)
        }
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
