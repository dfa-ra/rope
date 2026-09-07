package app.rope.android.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rope.android.RopeDarkBg
import app.rope.android.RopeShapes
import app.rope.android.UiState
import app.rope.android.data.ChatActions
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ComposerRules
import app.rope.android.data.MessageSearch
import app.rope.android.data.MessageTime
import app.rope.android.data.Conversation
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.ReactionCodec
import app.rope.android.data.ReactionPayload
import app.rope.android.data.VoiceGesture
import app.rope.android.media.ImageCodec
import kotlinx.coroutines.delay

private val OutBubbleLight = Color(0xFF3F3F46)
private val OutBubbleDark = Color(0xFF3F3F46)
private val InBubbleLight = Color(0xFFF4F4F5)
private val InBubbleDark = Color(0xFF27272A)
private val RecRed = Color(0xFFE53935)

@Composable
fun ChatsPane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onNewGroup: () -> Unit,
    onUpdateApp: () -> Unit = {},
    onCancelForward: () -> Unit = {},
    onQuery: (String) -> Unit = {},
    onPinChat: (String) -> Unit = {},
    onMuteChat: (String) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        val forwarding = state.forwarding
        when {
            forwarding != null -> {
                Surface(
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(bottomStart = RopeShapes.card, bottomEnd = RopeShapes.card),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Переслать в чат", style = MaterialTheme.typography.titleSmall)
                            Text(
                                forwarding.preview(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onCancelForward) { Text("Отмена") }
                    }
                }
            }
            state.appUpdateAvailable -> {
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(bottomStart = RopeShapes.card, bottomEnd = RopeShapes.card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onUpdateApp),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            "Доступно приложение ${state.latestAppVersion.ifBlank { "новее" }}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Нажмите, чтобы поставить поверх. Owner для этого не нужен.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        TextField(
            value = state.chatQuery,
            onValueChange = onQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = { Text("Поиск чатов") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (state.chatQuery.isNotBlank()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Очистить")
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(RopeShapes.search),
        )
        val rows = state.conversations.filter { ChatListRules.matches(it, state.chatQuery) }
        Box(Modifier.weight(1f).fillMaxSize()) {
            if (rows.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        if (state.chatQuery.isNotBlank()) "Ничего не нашли" else "Пока никого нет",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (state.forwarding != null) {
                            "Некуда переслать. Пригласите человека или создайте группу."
                        } else if (state.chatQuery.isNotBlank()) {
                            "Попробуйте другое имя или текст последнего сообщения."
                        } else {
                            "Пригласите человека QR-кодом или создайте группу."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(rows, key = { it.id }) { c ->
                        ConversationRow(
                            c,
                            onClick = { onOpen(c) },
                            onPin = { onPinChat(c.id) },
                            onMute = { onMuteChat(c.id) },
                        )
                    }
                }
            }
            if (state.forwarding == null) {
                FloatingActionButton(
                    onClick = onNewGroup,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Новая группа")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    c: Conversation,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit,
) {
    var menu by remember(c.id) { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            menu -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "chatRow",
    )
    Column(Modifier.background(bg)) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = { menu = !menu },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialsAvatar(c.title, c.isGroup, c.online)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (c.pinned) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = "Закреплён",
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(c.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (c.muted) {
                        Icon(
                            Icons.Outlined.NotificationsOff,
                            contentDescription = "Без звука",
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    c.last?.let {
                        Text(
                            MessageTime.label(it.timestampMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    c.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (c.online && !c.isGroup) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (c.unread > 0) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (c.unread > 99) "99+" else c.unread.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        if (menu) {
            Row(
                Modifier.padding(start = 72.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { onPin(); menu = false }) {
                    Text(if (c.pinned) "Открепить" else "Закрепить")
                }
                TextButton(onClick = { onMute(); menu = false }) {
                    Text(if (c.muted) "Включить звук" else "Без звука")
                }
            }
        }
    }
}

@Composable
fun ChatPane(
    state: UiState,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceFinish: (Boolean) -> Unit,
    onCall: () -> Unit,
    onPlay: (ChatMessage) -> Unit,
    onReact: (ChatMessage, String) -> Unit,
    onEnsureMedia: (ChatMessage) -> Unit,
    onGroupInfo: () -> Unit,
    onReply: (ChatMessage) -> Unit = {},
    onEdit: (ChatMessage) -> Unit = {},
    onDelete: (ChatMessage) -> Unit = {},
    onForward: (ChatMessage) -> Unit = {},
    onCancelComposer: () -> Unit = {},
    onCopy: (ChatMessage) -> Unit = {},
    onPinMessage: (ChatMessage) -> Unit = {},
    onJump: (String?) -> Unit = {},
    onOpenImage: (ChatMessage) -> Unit = {},
    onMessageQuery: (String) -> Unit = {},
    onConsumedScroll: () -> Unit = {},
) {
    val title = state.group?.name ?: state.peer?.displayName ?: "Чат"
    val online = state.group?.let { g ->
        g.members.any { it in state.onlineIds && it != state.profile?.deviceId }
    } ?: (state.peer?.online == true)
    val subtitle = when {
        !state.typingName.isNullOrBlank() ->
            if (state.group != null) "${state.typingName} печатает…" else "печатает…"
        state.group != null -> "${state.group.members.size} участников · ${if (online) "кто-то в сети" else "все офлайн"}"
        else -> MessageTime.lastSeenLabel(state.peer?.lastSeen.orEmpty(), online)
    }
    val visible = state.messages.filter { MessageSearch.matches(it, state.messageQuery) }
    val list = rememberLazyListState()
    var showSearch by remember { mutableStateOf(false) }
    var flashId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(visible.size, state.messageQuery) {
        if (visible.isNotEmpty() && state.scrollToMessageId == null) list.animateScrollToItem(visible.lastIndex)
    }
    LaunchedEffect(state.scrollToMessageId) {
        val id = state.scrollToMessageId ?: return@LaunchedEffect
        val idx = visible.indexOfFirst { it.id == id }
        if (idx >= 0) list.animateScrollToItem(idx)
        flashId = id
        onConsumedScroll()
        delay(700)
        if (flashId == id) flashId = null
    }
    val pinned = state.messages.find { it.id == state.pinnedMessageId && !it.deleted }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = state.group != null, onClick = onGroupInfo)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InitialsAvatar(title, state.group != null, online)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!state.typingName.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showSearch = !showSearch; if (!showSearch) onMessageQuery("") }) {
                Icon(Icons.Outlined.Search, contentDescription = "Поиск в чате")
            }
            if (state.peer != null && state.group == null) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Outlined.Call, contentDescription = "Позвонить")
                }
            }
        }
        if (showSearch) {
            TextField(
                value = state.messageQuery,
                onValueChange = onMessageQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Найти в чате") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(RopeShapes.search),
            )
        }
        pinned?.let { pin ->
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = RopeShapes.quote, bottomEnd = RopeShapes.quote),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJump(pin.id) },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.PushPin, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Закреплено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(pin.preview(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        LazyColumn(
            state = list,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(visible, key = { it.id }) { m ->
                MessageBubble(
                    m, state, onPlay, onReact, onEnsureMedia, onReply, onEdit, onDelete, onForward,
                    onCopy, onPinMessage, onJump, onOpenImage,
                    highlighted = flashId == m.id,
                )
            }
        }
        ComposerBar(state, onDraft, onSend, onAttach, onVoiceStart, onVoiceFinish, onCancelComposer)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    m: ChatMessage,
    state: UiState,
    onPlay: (ChatMessage) -> Unit,
    onReact: (ChatMessage, String) -> Unit,
    onEnsureMedia: (ChatMessage) -> Unit,
    onReply: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onForward: (ChatMessage) -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onPinMessage: (ChatMessage) -> Unit,
    onJump: (String?) -> Unit,
    onOpenImage: (ChatMessage) -> Unit,
    highlighted: Boolean = false,
) {
    val mine = m.outgoing
    val dark = MaterialTheme.colorScheme.background == RopeDarkBg
    val outBg = if (dark) OutBubbleDark else OutBubbleLight
    val outFg = if (dark) Color(0xFFF4F4F5) else Color.White
    var picker by remember(m.id) { mutableStateOf(false) }
    val me = state.profile?.deviceId.orEmpty()
    val selected = picker || highlighted
    var appeared by remember(m.id) { mutableStateOf(false) }
    LaunchedEffect(m.id) { appeared = true }
    val appear by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "bubbleIn",
    )
    val selectScale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f),
        label = "bubbleSel",
    )
    val selectAlpha by animateFloatAsState(
        targetValue = if (highlighted) 0.18f else 0f,
        animationSpec = tween(180),
        label = "bubbleFlash",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = 0.94f + 0.06f * appear
                scaleX = s * selectScale
                scaleY = s * selectScale
                alpha = appear
            },
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (mine) outBg else if (dark) InBubbleDark else InBubbleLight,
            contentColor = if (mine) outFg else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = RopeShapes.bubble,
                topEnd = RopeShapes.bubble,
                bottomStart = if (mine) RopeShapes.bubble else RopeShapes.bubbleTail,
                bottomEnd = if (mine) RopeShapes.bubbleTail else RopeShapes.bubble,
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (!m.deleted) picker = !picker },
                ),
        ) {
            Box {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    if (!mine && state.group != null && !m.deleted) {
                        Text(m.senderName.ifBlank { m.senderId.take(8) }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (!m.deleted && !m.replyToId.isNullOrBlank()) {
                        ReplyQuote(
                            name = m.replyName.ifBlank { "Ответ" },
                            preview = m.replyPreview.ifBlank { "Сообщение" },
                            accent = if (mine) outFg else MaterialTheme.colorScheme.primary,
                            onClick = { onJump(m.replyToId) },
                        )
                    }
                    if (m.deleted) {
                        Text(
                            "Сообщение удалено",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = if (mine) outFg.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        when (m.kind) {
                            MessageKind.VOICE -> VoiceBubble(m, state.playingVoiceId == m.id, onPlay)
                            MessageKind.IMAGE -> ImageBubble(m, onEnsureMedia, onOpenImage)
                            MessageKind.FILE -> FileBubble(m)
                            MessageKind.CALL -> Text("📞 ${m.text}", style = MaterialTheme.typography.bodyMedium)
                            MessageKind.UNKNOWN -> Text(m.text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFC107))
                            else -> Text(m.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    val meta = MessageTime.meta(m.status, m.outgoing, m.timestampMs, edited = m.edited && !m.deleted)
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mine) outFg.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = selectAlpha)),
                )
            }
        }
        if (picker && !m.deleted) {
            ReactionPicker { emoji ->
                onReact(m, emoji)
                picker = false
            }
            MessageActionRow(
                m,
                pinned = state.pinnedMessageId == m.id,
                onReply = { onReply(m); picker = false },
                onForward = { onForward(m); picker = false },
                onCopy = { onCopy(m); picker = false },
                onPin = { onPinMessage(m); picker = false },
                onEdit = { onEdit(m); picker = false },
                onDelete = { onDelete(m); picker = false },
            )
        }
        if (m.reactions.isNotEmpty() && !m.deleted) {
            ReactionRow(m, me, mine, onReact)
        }
    }
}

@Composable
private fun ReplyQuote(name: String, preview: String, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(90), label = "quote")
    val bgAlpha by animateFloatAsState(if (pressed) 0.30f else 0.16f, tween(90), label = "quoteBg")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .scale(scale)
            .clip(RoundedCornerShape(RopeShapes.quote))
            .background(accent.copy(alpha = bgAlpha))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(preview, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageActionRow(
    m: ChatMessage,
    pinned: Boolean,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(RopeShapes.action),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        FlowRow(
            Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (ChatActions.canReply(m)) {
                TextButton(onClick = onReply) { Text("Ответить") }
            }
            if (ChatActions.canForward(m)) {
                TextButton(onClick = onForward) { Text("Переслать") }
            }
            if (ChatActions.canCopy(m)) {
                TextButton(onClick = onCopy) { Text("Копировать") }
            }
            if (ChatActions.canPin(m)) {
                TextButton(onClick = onPin) { Text(if (pinned) "Открепить" else "Закрепить") }
            }
            if (ChatActions.canEdit(m)) {
                TextButton(onClick = onEdit) { Text("Изменить") }
            }
            if (ChatActions.canDelete(m)) {
                TextButton(onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}

@Composable
private fun ReactionPicker(onPick: (String) -> Unit) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(RopeShapes.picker),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            ReactionPayload.EMOJIS.forEach { emoji ->
                ReactionPickEmoji(emoji, onPick)
            }
        }
    }
}

@Composable
private fun ReactionPickEmoji(emoji: String, onPick: (String) -> Unit) {
    var pop by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pop) 1.38f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "reactPick",
    )
    Text(
        emoji,
        modifier = Modifier
            .scale(scale)
            .clickable {
                pop = true
                onPick(emoji)
            }
            .padding(8.dp),
        style = MaterialTheme.typography.headlineSmall,
    )
    LaunchedEffect(pop) {
        if (pop) {
            delay(180)
            pop = false
        }
    }
}

@Composable
private fun ReactionRow(
    m: ChatMessage,
    myId: String,
    mine: Boolean,
    onReact: (ChatMessage, String) -> Unit,
) {
    Row(
        Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReactionCodec.grouped(m.reactions).forEach { (emoji, people) ->
            val mineHere = people.any { it.deviceId == myId }
            ReactionChip(emoji, people.size, mineHere, mine) { onReact(m, emoji) }
        }
    }
}

@Composable
private fun ReactionChip(emoji: String, count: Int, mineHere: Boolean, mine: Boolean, onClick: () -> Unit) {
    var pop by remember(count, mineHere) { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (pop) 1.22f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "reactChip",
    )
    LaunchedEffect(count, mineHere) {
        pop = true
        delay(160)
        pop = false
    }
    Surface(
        color = if (mineHere) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(RopeShapes.chip),
        modifier = Modifier
            .scale(scale)
            .clickable(onClick = onClick),
    ) {
        Text(
            "$emoji $count",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (mine) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun VoiceBubble(m: ChatMessage, playing: Boolean, onPlay: (ChatMessage) -> Unit) {
    val extra = runCatching { MediaPayload.parse(m.extra) }.getOrNull()
    val wave by animateFloatAsState(if (playing) 1f else 0.35f, tween(180), label = "wave")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { onPlay(m) }, modifier = Modifier.size(36.dp)) {
            Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = "Голос")
        }
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(RopeShapes.media))
                    .background(Color.White.copy(alpha = 0.18f + 0.18f * wave)),
            )
            Text(
                when {
                    extra != null && m.localPath == null -> "скачивается…"
                    extra != null -> MediaPayload.formatDuration(extra.durationMs)
                    else -> m.text
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ImageBubble(m: ChatMessage, onEnsure: (ChatMessage) -> Unit, onOpen: (ChatMessage) -> Unit) {
    LaunchedEffect(m.id, m.localPath) {
        onEnsure(m)
    }
    val bmp = m.localPath?.let { runCatching { ImageCodec.decodePreview(it) }.getOrNull() }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Фото",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(RopeShapes.media))
                .clickable { onOpen(m) },
        )
    } else {
        Text(
            if (m.extra.isNotBlank()) "Фото · загружается…" else m.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable { onEnsure(m) },
        )
    }
}

@Composable
private fun FileBubble(m: ChatMessage) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null)
        Text(m.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ComposerBar(
    state: UiState,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceFinish: (Boolean) -> Unit,
    onCancelComposer: () -> Unit,
) {
    var recordingLocked by remember { mutableStateOf(false) }
    var slideHint by remember { mutableStateOf(VoiceGesture.HOLD) }
    LaunchedEffect(state.recording) {
        if (!state.recording) {
            recordingLocked = false
            slideHint = VoiceGesture.HOLD
        }
    }
    val showSend = ComposerRules.showSendButton(state.draftText, state.recording, recordingLocked)
    val micScale by animateFloatAsState(
        targetValue = if (state.recording && !recordingLocked) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "mic",
    )
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = RopeShapes.card, topEnd = RopeShapes.card),
    ) {
        Column(Modifier.fillMaxWidth()) {
            state.editTarget?.let { target ->
                ComposerHint(
                    title = "Редактирование",
                    body = target.text,
                    onCancel = onCancelComposer,
                )
            } ?: state.replyTo?.let { target ->
                ComposerHint(
                    title = if (target.outgoing) "Ответ себе" else "Ответ · ${target.senderName.ifBlank { "сообщение" }}",
                    body = target.preview(),
                    onCancel = onCancelComposer,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!state.recording) {
                    IconButton(onClick = onAttach) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = "Вложение")
                    }
                } else if (recordingLocked) {
                    IconButton(onClick = { onVoiceFinish(false) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Отменить запись", tint = RecRed)
                    }
                }
                if (state.recording) {
                    RecordingStrip(
                        recordMs = state.recordMs,
                        locked = recordingLocked,
                        hint = slideHint,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 6.dp),
                    )
                } else {
                    TextField(
                        value = state.draftText,
                        onValueChange = onDraft,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Сообщение") },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(RopeShapes.field),
                        maxLines = 5,
                    )
                }
                if (showSend) {
                    IconButton(
                        onClick = {
                            if (recordingLocked || state.recording) onVoiceFinish(true) else onSend()
                        },
                    ) {
                        Icon(
                            if (recordingLocked) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Отправить",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(48.dp)
                            .scale(micScale)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    onVoiceStart()
                                    var latest = VoiceGesture.HOLD
                                    var lockedNow = false
                                    slideHint = VoiceGesture.HOLD
                                    drag(down.id) { change ->
                                        val dx = change.position.x - down.position.x
                                        val dy = change.position.y - down.position.y
                                        latest = ComposerRules.voiceGesture(dx, dy)
                                        if (latest == VoiceGesture.LOCK) {
                                            lockedNow = true
                                            recordingLocked = true
                                        }
                                        slideHint = if (lockedNow) VoiceGesture.LOCK else latest
                                        change.consume()
                                    }
                                    when {
                                        lockedNow -> slideHint = VoiceGesture.LOCK
                                        latest == VoiceGesture.CANCEL -> onVoiceFinish(false)
                                        else -> onVoiceFinish(true)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Голосовое: удерживайте",
                            tint = if (state.recording) RecRed else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingStrip(
    recordMs: Long,
    locked: Boolean,
    hint: VoiceGesture,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "recPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "recGlow",
    )
    val hintColor = when (hint) {
        VoiceGesture.CANCEL -> RecRed
        VoiceGesture.LOCK -> MaterialTheme.colorScheme.primary
        VoiceGesture.HOLD -> RecRed
    }
    val caption = when {
        locked -> "запись закреплена"
        hint == VoiceGesture.CANCEL -> "отпустите — отмена"
        hint == VoiceGesture.LOCK -> "отпустите — закрепить"
        else -> "влево — отмена · вверх — закрепить"
    }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(RecRed.copy(alpha = glow)),
        )
        Text(
            MediaPayload.formatDuration(recordMs),
            color = RecRed,
            style = MaterialTheme.typography.titleMedium,
        )
        Icon(
            if (locked) Icons.Outlined.Lock else Icons.Outlined.KeyboardArrowUp,
            contentDescription = if (locked) "Запись закреплена" else "Вверх — закрепить",
            tint = if (locked) MaterialTheme.colorScheme.primary else hintColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            caption,
            color = hintColor,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ComposerHint(title: String, body: String, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onCancel) { Text("Отмена") }
    }
}

@Composable
fun InitialsAvatar(title: String, group: Boolean, online: Boolean) {
    val letter = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (group) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (group) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            } else {
                Text(letter, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (online) Color(0xFF43A047) else Color(0xFF9E9E9E)),
        )
    }
}

@Composable
fun ImageViewer(msg: ChatMessage, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = msg.localPath?.let { runCatching { ImageCodec.decodePreview(it) }.getOrNull() }
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Фото",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(RopeShapes.media)),
            )
        } else {
            Text("Фото ещё качается", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
