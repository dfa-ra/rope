package app.rope.android

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.LightMode
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.rope.android.data.ThemeMode
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
    onOpenConversation: (app.rope.android.data.Conversation) -> Unit,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onInvite: () -> Unit,
    onStatus: () -> Unit,
    onScan: () -> Unit,
    onUpdateApp: () -> Unit,
    onUpgradeCore: (String, String) -> Unit,
    onRestoreBackup: () -> Unit,
    onAttach: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceFinish: (Boolean) -> Unit,
    onCall: () -> Unit,
    onPlay: (app.rope.android.data.ChatMessage) -> Unit,
    onGroupName: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onHangup: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Box {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val me = state.profile?.displayName.orEmpty()
                    val title = when {
                        state.offline -> "Rope · офлайн"
                        me.isNotBlank() -> "Rope · $me"
                        else -> "Rope"
                    }
                    Text(title)
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (state.theme == ThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (state.theme == ThemeMode.DARK) "Светлая тема" else "Тёмная тема",
                        )
                    }
                    if (state.profile != null) {
                        IconButton(onClick = onInvite) {
                            Icon(Icons.Outlined.QrCode, contentDescription = "Пригласить")
                        }
                        IconButton(onClick = onStatus) {
                            Icon(Icons.Outlined.Dns, contentDescription = "Сервер")
                        }
                        IconButton(onClick = { onGo(Screen.Chats) }) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Чаты")
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
                    Screen.Start -> StartPane(onGo, onRestoreBackup)
                    Screen.Provision -> ProvisionPane(!state.busy, onProvision) { onGo(Screen.Start) }
                    Screen.Join -> JoinPane(!state.busy, state.pendingInvite.orEmpty(), onJoin, onJoinDev, onScan) { onGo(Screen.Start) }
                    Screen.Chats -> app.rope.android.ui.ChatsPane(state, onOpenConversation) { onGo(Screen.NewGroup) }
                    Screen.Chat -> app.rope.android.ui.ChatPane(
                        state, onDraft, onSend, onAttach, onVoiceStart, onVoiceFinish, onCall, onPlay,
                    ) { onGo(Screen.GroupInfo) }
                    Screen.Invite -> InvitePane(state.inviteUrl.orEmpty())
                    Screen.Status -> app.rope.android.ui.StatusPane(state, onUpdateApp, onUpgradeCore)
                    Screen.Settings -> Text("Settings", modifier = Modifier.padding(16.dp))
                    Screen.NewGroup -> app.rope.android.ui.NewGroupPane(state, onGroupName, onToggleMember, onCreateGroup) { onGo(Screen.Chats) }
                    Screen.GroupInfo -> app.rope.android.ui.GroupInfoPane(state, onAddMember, onRemoveMember) { onGo(Screen.Chat) }
                }
            }
        }
    }
    state.call?.let { call ->
        app.rope.android.ui.CallOverlay(call, onAcceptCall, onRejectCall, onHangup)
    }
    }
}

@Composable
private fun StartPane(onGo: (Screen) -> Unit, onRestore: () -> Unit) {
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
        TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
            Text("Восстановить устройство из Загрузок")
        }
        Text(
            "Если пришлось удалить приложение из‑за другой подписи: выберите файл rope-device.backup из Загрузок.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    var name by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(ServerTarget.AUTO) }
    var advanced by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    val canInstall = enabled && host.isNotBlank() && user.isNotBlank() &&
        (password.isNotBlank() || key.isNotBlank()) && LoginRules.isValid(name)

    fun form(upgrade: Boolean, reinstall: Boolean = false) = ProvisionForm(
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
        reinstall = reinstall,
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
                "Нужен VPS с Ubuntu/Debian и SSH. Если Rope уже стоит — обычная установка не затрёт его: обновите ядро или сотрите и зайдите заново как owner.",
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
                label = { Text("Ваш логин") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            ArchPicker(target = target, enabled = enabled, onSelect = { target = it })
            TextButton(onClick = { advanced = !advanced }, enabled = enabled) {
                Text(if (advanced) "Скрыть доп. настройки" else "Дополнительно: порт, обновление, переустановка")
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
                    onClick = { onProvision(form(upgrade = true)) },
                    enabled = canInstall,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Обновить ядро, данные сохранить") }
                Text(
                    "Как обновление в Amnezia: бинарник меняется, чаты и owner остаются.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        if (!confirmWipe) {
                            confirmWipe = true
                        } else {
                            onProvision(form(upgrade = false, reinstall = true))
                        }
                    },
                    enabled = canInstall,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (confirmWipe) "Точно стереть owner и чаты?" else "Стереть старое и стать владельцем")
                }
                Text(
                    "Если потеряли телефон-owner: сотрёт data.db и выдаст новый setup token. Сертификат TLS останется.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun JoinPane(
    enabled: Boolean,
    initialInvite: String,
    onJoin: (String, String) -> Unit,
    onJoinDev: (String, Int, String, String) -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit,
) {
    var url by remember(initialInvite) { mutableStateOf(initialInvite) }
    var name by remember { mutableStateOf("") }
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
                label = { Text("Придумайте логин") },
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
                    enabled = enabled && LoginRules.isValid(name),
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
                enabled = enabled && url.isNotBlank() && LoginRules.isValid(name),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Войти по ссылке") }
            TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Назад")
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
