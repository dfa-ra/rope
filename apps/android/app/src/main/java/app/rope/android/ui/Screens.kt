package app.rope.android

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.net.Uri
import app.rope.android.data.RoleRules
import app.rope.android.data.ThemeMode
import app.rope.android.RopeShapes
import app.rope.android.provision.ProvisionForm
import app.rope.android.provision.ServerTarget
import app.rope.android.ui.BrandBackdrop
import app.rope.android.ui.CallsPane
import app.rope.android.ui.FadeIn
import app.rope.android.ui.GlowButton
import app.rope.android.ui.HomePane
import app.rope.android.ui.InitialsAvatar
import app.rope.android.ui.PeoplePane
import app.rope.android.ui.QuietButton
import app.rope.android.ui.RopeEmptyState
import app.rope.android.ui.RopeLogoMark
import app.rope.android.ui.RopeSplash
import app.rope.android.ui.SectionCard
import app.rope.android.ui.rememberSplashOverlay
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
    onAttachGallery: () -> Unit = {},
    onAttachFile: () -> Unit = {},
    onAttachUri: (Uri) -> Unit = {},
    onAttachUris: (List<Uri>) -> Unit = {},
    onVoiceStart: () -> Unit,
    onVoiceFinish: (Boolean) -> Unit,
    onSeekVoice: (app.rope.android.data.ChatMessage, Long) -> Unit = { _, _ -> },
    onCycleVoiceSpeed: () -> Unit = {},
    onVideoNoteStart: () -> Unit = {},
    onVideoNoteFinish: (Boolean) -> Unit = {},
    onVideoNotePreview: (android.view.SurfaceHolder, Int) -> Unit = { _, _ -> },
    onVideoNotePreviewGone: () -> Unit = {},
    onCall: () -> Unit,
    onVideoCall: () -> Unit = {},
    onPlay: (app.rope.android.data.ChatMessage) -> Unit,
    onReact: (app.rope.android.data.ChatMessage, String) -> Unit,
    onEnsureMedia: (app.rope.android.data.ChatMessage) -> Unit,
    onGroupName: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeaveGroup: () -> Unit = {},
    onRevokeMember: (String) -> Unit = {},
    onSetNickname: (String, String) -> Unit = { _, _ -> },
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onHangup: () -> Unit,
    onToggleCallMute: () -> Unit = {},
    onToggleCallSpeaker: () -> Unit = {},
    onToggleCallCamera: () -> Unit = {},
    onFlipCallCamera: () -> Unit = {},
    callEgl: () -> org.webrtc.EglBase.Context? = { null },
    onBindCallRemote: (org.webrtc.VideoSink) -> Unit = {},
    onBindCallLocal: (org.webrtc.VideoSink) -> Unit = {},
    onUnbindCallRemote: (org.webrtc.VideoSink) -> Unit = {},
    onUnbindCallLocal: (org.webrtc.VideoSink) -> Unit = {},
    onToggleTheme: () -> Unit,
    onSetTheme: (app.rope.android.data.ThemeMode) -> Unit = {},
    onToggleNotifications: () -> Unit = {},
    onToggleLinkPreviews: () -> Unit = {},
    onCopyText: (String) -> Unit = {},
    onReply: (app.rope.android.data.ChatMessage) -> Unit,
    onReplySpan: (app.rope.android.data.QuoteSpan?) -> Unit = {},
    onEdit: (app.rope.android.data.ChatMessage) -> Unit,
    onDelete: (app.rope.android.data.ChatMessage) -> Unit,
    onForward: (app.rope.android.data.ChatMessage) -> Unit,
    onCancelComposer: () -> Unit,
    onDismissLinkPreview: () -> Unit = {},
    onCancelPendingMedia: () -> Unit = {},
    onCancelForward: () -> Unit,
    onChatQuery: (String) -> Unit,
    onMessageQuery: (String) -> Unit,
    onPinChat: (String) -> Unit,
    onMuteChat: (String) -> Unit,
    onArchiveChat: (String) -> Unit = {},
    onUnarchiveChat: (String) -> Unit = {},
    onCopy: (app.rope.android.data.ChatMessage) -> Unit,
    onPinMessage: (app.rope.android.data.ChatMessage) -> Unit,
    onJump: (String?) -> Unit,
    onOpenImage: (app.rope.android.data.ChatMessage) -> Unit,
    onCloseImage: () -> Unit,
    onConsumedScroll: () -> Unit,
    onDismissNotice: () -> Unit,
    onBack: () -> Boolean = { false },
    onTab: (Screen) -> Unit = onGo,
) {
    val signedIn = state.profile != null
    val splash = rememberSplashOverlay(state)
    val selectedTab = NavRules.selectedTab(state.screen)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.notice) {
        val msg = state.notice?.trim().orEmpty()
        if (msg.isEmpty()) return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onDismissNotice()
    }
    BackHandler(enabled = BackStack.consumesSystemBack(state)) {
        onBack()
    }
    Box {
    Scaffold(
        topBar = {
            if (InstantUi.showsAppBar(state.screen)) {
            TopAppBar(
                navigationIcon = {},
                title = {
                    val title = NavRules.chromeTitle(state.screen, offline = state.offline)
                    val opensHome = NavRules.titleOpensHome(state.screen, signedIn)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RopeLogoMark(
                            modifier = Modifier
                                .semantics { contentDescription = "На главную" }
                                .clickable(enabled = opensHome) { onTab(Screen.Home) },
                            size = 28.dp,
                            animate = true,
                            breathe = state.screen == Screen.Home || state.screen == Screen.Start,
                            replayKey = selectedTab?.ordinal ?: state.screen.ordinal,
                        )
                        if (title.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text(title)
                        }
                    }
                },
                actions = {
                    if (!signedIn) {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (state.theme == ThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (state.theme == ThemeMode.DARK) "Светлая тема" else "Тёмная тема",
                            )
                        }
                    } else if (state.screen != Screen.Settings) {
                        IconButton(onClick = { onGo(Screen.Settings) }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Настройки")
                        }
                    }
                    if (signedIn) {
                        if (RoleRules.canShowInviteQr(state.profile?.role)) {
                            IconButton(onClick = onInvite) {
                                Icon(Icons.Outlined.QrCode, contentDescription = "Пригласить")
                            }
                        }
                        IconButton(onClick = onStatus) {
                            Icon(Icons.Outlined.Dns, contentDescription = "Сервер")
                        }
                        if (NavRules.chromeShowsUserChip(true)) {
                            val me = state.profile?.displayName.orEmpty()
                            IconButton(
                                onClick = { onTab(Screen.Home) },
                                modifier = Modifier.semantics {
                                    contentDescription = me.ifBlank { "Профиль" }
                                },
                            ) {
                                InitialsAvatar(
                                    title = me.ifBlank { "?" },
                                    group = false,
                                    online = !state.offline,
                                    size = 32.dp,
                                )
                            }
                        }
                    }
                },
            )
            }
        },
        bottomBar = {
            if (NavRules.showsBottomBar(state.screen, signedIn)) {
                RopeBottomBar(state.screen, onTab)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (state.busy && InstantUi.busyBlocksUi(state.screen)) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = state.screen,
                    transitionSpec = { screenTransition(initialState, targetState) },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        Screen.Start -> StartPane(onGo, onRestoreBackup)
                        Screen.Provision -> ProvisionPane(!state.busy, onProvision) { onBack() }
                        Screen.Join -> JoinPane(!state.busy, state.pendingInvite.orEmpty(), onJoin, onJoinDev, onScan) { onBack() }
                        Screen.Home -> HomePane(state, onGo, onStatus, onInvite)
                        Screen.Chats, Screen.Groups, Screen.Archive -> app.rope.android.ui.ChatsPane(
                            state,
                            onOpenConversation,
                            { onGo(Screen.NewGroup) },
                            onUpdateApp,
                            onCancelForward,
                            onChatQuery,
                            onPinChat,
                            onMuteChat,
                            onArchiveChat,
                            onUnarchiveChat,
                            { onGo(Screen.Archive) },
                            listMode = NavRules.listMode(screen),
                        )
                        Screen.Calls -> CallsPane(state, onOpenConversation, onInvite)
                        Screen.People -> PeoplePane(state, onOpenConversation, onInvite, onRevokeMember, onSetNickname)
                        Screen.Chat -> app.rope.android.ui.ChatPane(
                            state, onDraft, onSend, onAttach, onVoiceStart, onVoiceFinish, onCall, onVideoCall, onPlay,
                            onReact, onEnsureMedia,
                            onGroupInfo = { onGo(Screen.GroupInfo) },
                            onPeerProfile = { onGo(Screen.PeerProfile) },
                            onBack = { onBack() },
                            onReply = onReply,
                            onReplySpan = onReplySpan,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onForward = onForward,
                            onCancelComposer = onCancelComposer,
                            onDismissLinkPreview = onDismissLinkPreview,
                            onCancelPendingMedia = onCancelPendingMedia,
                            onCopy = onCopy,
                            onPinMessage = onPinMessage,
                            onJump = onJump,
                            onOpenImage = onOpenImage,
                            onMessageQuery = onMessageQuery,
                            onConsumedScroll = onConsumedScroll,
                            onAttachGallery = onAttachGallery,
                            onAttachFile = onAttachFile,
                            onAttachUri = onAttachUri,
                            onAttachUris = onAttachUris,
                            onSeekVoice = onSeekVoice,
                            onCycleVoiceSpeed = onCycleVoiceSpeed,
                            onVideoNoteStart = onVideoNoteStart,
                            onVideoNoteFinish = onVideoNoteFinish,
                            onVideoNotePreview = onVideoNotePreview,
                            onVideoNotePreviewGone = onVideoNotePreviewGone,
                        )
                        Screen.Invite -> if (RoleRules.canShowInviteQr(state.profile?.role)) {
                            InvitePane(state.inviteUrl.orEmpty(), { onBack() }) { onTab(Screen.Home) }
                        } else {
                            HomePane(state, onGo, onStatus, onInvite)
                        }
                        Screen.Status -> app.rope.android.ui.StatusPane(state, onUpdateApp, onUpgradeCore) { onTab(Screen.Home) }
                        Screen.Settings -> app.rope.android.ui.SettingsPane(
                            state,
                            onSetTheme,
                            onToggleNotifications,
                            onCopyText,
                            onToggleLinkPreviews,
                        )
                        Screen.NewGroup -> app.rope.android.ui.NewGroupPane(state, onGroupName, onToggleMember, onCreateGroup) { onBack() }
                        Screen.GroupInfo -> app.rope.android.ui.GroupInfoPane(state, onAddMember, onRemoveMember, onLeaveGroup) { onBack() }
                        Screen.PeerProfile -> app.rope.android.ui.PeerProfilePane(
                            state,
                            onBack = { onBack() },
                            onOpenImage = onOpenImage,
                            onEnsureMedia = onEnsureMedia,
                        )
                    }
                }
            }
        }
    }
    state.viewingImage?.let { img ->
        val siblings = if (state.screen == Screen.PeerProfile) {
            app.rope.android.data.PeerProfileRules.photos(state.messages).ifEmpty { listOf(img) }
        } else {
            app.rope.android.data.AlbumRules.siblings(state.messages, img)
        }
        app.rope.android.ui.ImageViewer(
            msg = img,
            siblings = siblings.ifEmpty { listOf(img) },
            onClose = onCloseImage,
            onShow = onOpenImage,
            onEnsure = onEnsureMedia,
        )
    }
    state.call?.let { call ->
        app.rope.android.ui.CallOverlay(
            call,
            state.profile?.iceServersJson.orEmpty(),
            onAcceptCall,
            onRejectCall,
            onHangup,
            micMuted = state.callMicMuted,
            speakerOn = state.callSpeakerOn,
            onToggleMute = onToggleCallMute,
            onToggleSpeaker = onToggleCallSpeaker,
            camMuted = state.callCamMuted,
            banner = state.callNotice,
            onToggleCamera = onToggleCallCamera,
            onFlipCamera = onFlipCallCamera,
            localMirror = state.callLocalMirrored,
            eglContext = callEgl,
            rtcReady = state.callRtcReady,
            onBindRemote = onBindCallRemote,
            onBindLocal = onBindCallLocal,
            onUnbindRemote = onUnbindCallRemote,
            onUnbindLocal = onUnbindCallLocal,
        )
    }
    if (splash.visible) {
        RopeSplash(caption = splash.caption, loop = splash.loop, compact = splash.compact)
    }
    }
}

@Composable
private fun RopeBottomBar(
    screen: Screen,
    onTab: (Screen) -> Unit,
) {
    val selected = NavRules.selectedTab(screen)
    NavigationBar {
        NavRules.tabs.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab.screen,
                onClick = { onTab(tab.screen) },
                icon = { Icon(tabIcon(tab.screen), contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

private fun screenTransition(from: Screen, to: Screen): ContentTransform {
    if (InstantUi.instantTransition(from, to)) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    val enterMessenger = from == Screen.Home && NavRules.isMessengerShell(to)
    val leaveMessenger = to == Screen.Home && NavRules.isMessengerShell(from)
    return when {
        enterMessenger ->
            (slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(160))) togetherWith
                (slideOutHorizontally(tween(160)) { -it / 10 } + fadeOut(tween(120)))
        leaveMessenger ->
            (slideInHorizontally(tween(220)) { -it / 6 } + fadeIn(tween(160))) togetherWith
                (slideOutHorizontally(tween(160)) { it / 10 } + fadeOut(tween(120)))
        else -> fadeIn(tween(80)) togetherWith fadeOut(tween(60))
    }
}

private fun tabIcon(screen: Screen): ImageVector = when (screen) {
    Screen.Chats -> Icons.AutoMirrored.Outlined.Chat
    Screen.Groups -> Icons.Outlined.Groups
    Screen.Calls -> Icons.Outlined.Call
    Screen.People -> Icons.Outlined.People
    else -> Icons.Outlined.Dns
}

@Composable
private fun StartPane(onGo: (Screen) -> Unit, onRestore: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BrandBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FadeIn(40) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RopeLogoMark(size = 132.dp, animate = true)
                    Spacer(Modifier.height(10.dp))
                    Text("self-hosted · E2EE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Личный мессенджер на своём VPS", style = MaterialTheme.typography.headlineSmall)
                }
            }
            FadeIn(160) {
                Text(
                    "Поднимите сервер с телефона или войдите по QR от организатора.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            FadeIn(220) {
                GlowButton("Создать сервер", { onGo(Screen.Provision) }, Modifier.fillMaxWidth())
            }
            FadeIn(280) {
                QuietButton("Войти по QR или ссылке", { onGo(Screen.Join) }, Modifier.fillMaxWidth())
            }
            TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text("Восстановить устройство из файла")
            }
            Text(
                "Выберите файл резервной копии устройства, если вы сохраняли его сами. Rope больше не кладёт ключи в Загрузки.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            FadeIn(40) { Text("Создание сервера", style = MaterialTheme.typography.titleLarge) }
            FadeIn(100) {
                Text(
                    "Нужен VPS с Ubuntu/Debian и SSH. Если Rope уже стоит — обычная установка не затрёт его: обновите ядро или сотрите и зайдите заново как owner.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                host,
                { host = it },
                label = { Text("IP или hostname VPS") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(RopeShapes.field),
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
            GlowButton(
                "Установить и подключить",
                { onProvision(form(false)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canInstall,
            )
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
            FadeIn(40) { Text("Вход", style = MaterialTheme.typography.titleLarge) }
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Придумайте логин") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(RopeShapes.field),
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
            if (JoinDebugRules.showHttpJoin(BuildConfig.DEBUG)) {
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
        }
        HorizontalDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Сканировать QR", onScan, Modifier.fillMaxWidth(), enabled = enabled)
            QuietButton(
                "Войти по ссылке",
                { onJoin(url, name) },
                Modifier.fillMaxWidth(),
                enabled = enabled && url.isNotBlank() && LoginRules.isValid(name),
            )
        }
    }
}

@Composable
private fun InvitePane(url: String, onBack: () -> Unit, onHome: () -> Unit = onBack) {
    Box(Modifier.fillMaxSize()) {
        BrandBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FadeIn(40) { RopeLogoMark(size = 72.dp, animate = true) }
            FadeIn(120) { Text("Приглашение", style = MaterialTheme.typography.titleLarge) }
            FadeIn(180) {
                Text(
                    "Покажите QR гостю. Ссылка одноразовая и сгорит по TTL.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (url.isNotBlank()) {
                FadeIn(240) {
                    SectionCard {
                        Image(
                            bitmap = qrBitmap(url).asImageBitmap(),
                            contentDescription = "invite",
                            modifier = Modifier
                                .size(260.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                        Text(url, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                RopeEmptyState(
                    title = "Ссылка ещё готовится",
                    body = "Подождите секунду. Петля в шапке вернёт на главную.",
                )
            }
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
