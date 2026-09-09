package app.rope.android.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.rope.android.RopeDarkBg
import app.rope.android.RopeShapes
import app.rope.android.UiState
import app.rope.android.data.ChatActions
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.QueryHighlight
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatSelection
import app.rope.android.data.ComposerRules
import app.rope.android.data.GroupChatUx
import app.rope.android.data.MessageSearch
import app.rope.android.data.MessageTime
import app.rope.android.data.Conversation
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.PhotoLayout
import app.rope.android.data.ReactionCodec
import app.rope.android.data.RoleRules
import app.rope.android.data.VoiceGesture
import app.rope.android.media.ImageCodec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    listMode: ChatListMode = ChatListMode.ALL,
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
        ChatListSearchField(
            value = state.chatQuery,
            onValueChange = onQuery,
            placeholder = when (listMode) {
                ChatListMode.GROUPS -> "Поиск групп"
                ChatListMode.CALLS -> "Поиск звонков"
                ChatListMode.ALL -> "Поиск"
            },
        )
        val rows = ChatListRules.rows(state.conversations, state.chatQuery, listMode)
        val pinnedRows = ChatListRules.pinnedBlock(rows, state.chatQuery)
        val otherRows = ChatListRules.unpinnedBlock(rows, state.chatQuery)
        Box(Modifier.weight(1f).fillMaxSize()) {
            if (rows.isEmpty()) {
                RopeEmptyState(
                    title = emptyTitle(listMode, state.chatQuery),
                    body = emptyBody(listMode, state.chatQuery, state.forwarding != null, state.profile?.role),
                    actionLabel = if (listMode != ChatListMode.CALLS && state.forwarding == null && state.chatQuery.isBlank()) {
                        if (listMode == ChatListMode.GROUPS) "Новая группа" else null
                    } else null,
                    onAction = if (listMode == ChatListMode.GROUPS) onNewGroup else null,
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (pinnedRows.isNotEmpty()) {
                        itemsIndexed(pinnedRows, key = { _, c -> c.id }) { _, c ->
                            FadeIn(0) {
                                ConversationRow(
                                    c,
                                    onClick = { onOpen(c) },
                                    onPin = { onPinChat(c.id) },
                                    onMute = { onMuteChat(c.id) },
                                    query = state.chatQuery,
                                )
                            }
                        }
                        if (ChatListRules.showPinDivider(rows, state.chatQuery)) {
                            item(key = "pinned-divider") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                )
                            }
                        }
                    }
                    itemsIndexed(otherRows, key = { _, c -> c.id }) { _, c ->
                        FadeIn(0) {
                            ConversationRow(
                                c,
                                onClick = { onOpen(c) },
                                onPin = { onPinChat(c.id) },
                                onMute = { onMuteChat(c.id) },
                                query = state.chatQuery,
                            )
                        }
                    }
                }
            }
            if (state.forwarding == null && listMode != ChatListMode.CALLS && !ChatListRules.searching(state.chatQuery)) {
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

@Composable
private fun ChatListSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(RopeShapes.search))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Очистить",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
}

private fun emptyTitle(mode: ChatListMode, query: String): String = when {
    query.isNotBlank() -> "Ничего не нашли"
    mode == ChatListMode.GROUPS -> "Групп пока нет"
    mode == ChatListMode.CALLS -> "Звонков ещё не было"
    else -> "Пока никого нет"
}

private fun emptyBody(mode: ChatListMode, query: String, forwarding: Boolean, role: String?): String = when {
    forwarding -> if (RoleRules.canInvite(role)) {
        "Некуда переслать. Пригласите человека или создайте группу."
    } else {
        "Некуда переслать. Когда появятся чаты, можно будет переслать сюда."
    }
    query.isNotBlank() -> "Попробуйте другое имя или текст последнего сообщения."
    mode == ChatListMode.GROUPS -> RoleRules.groupsEmptyBody()
    mode == ChatListMode.CALLS -> RoleRules.callsEmptyBody(role)
    else -> RoleRules.chatsEmptyBody(role)
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val range = QueryHighlight.firstRange(text, query)
    val inBounds = range != null && range.first >= 0 && range.last < text.length
    if (range == null || !inBounds) {
        Text(
            text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }
    val annotated = buildAnnotatedString {
        append(text.substring(0, range.first))
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
            append(text.substring(range.first, range.last + 1))
        }
        append(text.substring(range.last + 1))
    }
    Text(
        annotated,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ConversationRow(
    c: Conversation,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit,
    query: String = "",
) {
    var menu by remember(c.id) { mutableStateOf(false) }
    BackHandler(enabled = menu) { menu = false }
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
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "chatRowS")
    Column(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(bg),
    ) {
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
                    HighlightedText(
                        text = c.title,
                        query = query,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
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
                    if (c.pinned) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = "Закреплён",
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(12.dp),
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
                HighlightedText(
                    text = c.subtitle,
                    query = query,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (c.online && !c.isGroup) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalMaterial3Api::class)
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
    onBack: () -> Unit = {},
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
    onAttachGallery: () -> Unit = onAttach,
    onAttachFile: () -> Unit = onAttach,
    onAttachUri: (Uri) -> Unit = {},
) {
    val title = state.group?.name ?: state.peer?.displayName ?: "Чат"
    val online = state.group?.let { g ->
        g.members.any { it in state.onlineIds && it != state.profile?.deviceId }
    } ?: (state.peer?.online == true)
    val typing = state.typingName
    val subtitle = when {
        !typing.isNullOrBlank() -> typing
        state.group != null -> "${state.group.members.size} участников · ${if (online) "кто-то в сети" else "все офлайн"}"
        else -> MessageTime.lastSeenLabel(state.peer?.lastSeen.orEmpty(), online)
    }
    val mentionNames = remember(state.group, state.devices, state.profile?.displayName) {
        (state.devices.map { it.displayName } + listOfNotNull(state.profile?.displayName, GroupChatUx.YOU))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
    val visible = state.messages.filter { MessageSearch.matches(it, state.messageQuery) }
    val list = rememberLazyListState()
    var showSearch by remember { mutableStateOf(false) }
    var flashId by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showAttach by remember { mutableStateOf(false) }
    var menuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var reactionExpanded by remember { mutableStateOf(false) }
    val selecting = selectedIds.isNotEmpty()
    val selectedMsgs = remember(selectedIds, visible) { visible.filter { it.id in selectedIds } }
    val singleSelected = selectedMsgs.singleOrNull()
    BackHandler(enabled = menuMessage != null) {
        if (reactionExpanded) reactionExpanded = false else menuMessage = null
    }
    BackHandler(enabled = showSearch && menuMessage == null) {
        showSearch = false
        onMessageQuery("")
    }
    BackHandler(enabled = selecting && menuMessage == null) { selectedIds = emptySet() }
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
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        if (selecting) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { selectedIds = emptySet() }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Снять выделение")
                }
                Text(
                    ChatSelection.title(selectedIds.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (singleSelected != null && ChatActions.canEdit(singleSelected)) {
                    IconButton(onClick = { onEdit(singleSelected); selectedIds = emptySet() }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Изменить")
                    }
                }
                if (selectedMsgs.any { ChatActions.canForward(it) }) {
                    IconButton(onClick = {
                        selectedMsgs.firstOrNull { ChatActions.canForward(it) }?.let(onForward)
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Переслать")
                    }
                }
                if (selectedMsgs.any { ChatActions.canDelete(it) }) {
                    IconButton(onClick = {
                        selectedMsgs.filter { ChatActions.canDelete(it) }.forEach(onDelete)
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                    }
                }
            }
        } else {
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
        }
        if (showSearch && !selecting) {
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
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (visible.isEmpty()) {
                RopeEmptyState(
                    title = if (state.messageQuery.isNotBlank()) "Ничего не нашли" else "Начните переписку",
                    body = if (state.messageQuery.isNotBlank()) {
                        "Другой запрос — или очистите поиск."
                    } else {
                        RoleRules.threadEmptyBody()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 10.dp,
                        vertical = 8.dp,
                        // glass bar sits over the last bubbles
                    ).let { base ->
                        PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = 8.dp,
                            bottom = if (selecting) 88.dp else 8.dp,
                        )
                    },
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(visible, key = { _, m -> m.id }) { index, m ->
                        MessageBubble(
                            m, state, onPlay, onReact, onEnsureMedia, onJump,
                            highlighted = flashId == m.id,
                            clusterFirst = GroupChatUx.firstInCluster(visible, index),
                            clusterLast = GroupChatUx.lastInCluster(visible, index),
                            mentionNames = mentionNames,
                            selected = m.id in selectedIds,
                            selecting = selecting,
                            onToggleSelect = {
                                selectedIds = if (m.id in selectedIds) selectedIds - m.id else selectedIds + m.id
                            },
                            onEnterSelect = {
                                if (!m.deleted) {
                                    menuMessage = null
                                    reactionExpanded = false
                                    selectedIds = selectedIds + m.id
                                }
                            },
                            onTap = {
                                if (!m.deleted) {
                                    menuMessage = m
                                    reactionExpanded = false
                                }
                            },
                        )
                    }
                }
            }
            if (selecting) {
                SelectionReplyForwardBar(
                    canReply = singleSelected != null && ChatActions.canReply(singleSelected),
                    canForward = selectedMsgs.any { ChatActions.canForward(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    onReply = {
                        singleSelected?.let { if (ChatActions.canReply(it)) onReply(it) }
                        selectedIds = emptySet()
                    },
                    onForward = {
                        selectedMsgs.firstOrNull { ChatActions.canForward(it) }?.let(onForward)
                        selectedIds = emptySet()
                    },
                )
            }
        }
        if (!selecting) {
            ComposerBar(
                state = state,
                onDraft = onDraft,
                onSend = onSend,
                onAttach = { showAttach = true },
                onVoiceStart = onVoiceStart,
                onVoiceFinish = onVoiceFinish,
                onCancelComposer = onCancelComposer,
            )
        }
    }
    menuMessage?.let { target ->
        MessageTapOverlay(
            message = target,
            pinned = state.pinnedMessageId == target.id,
            expanded = reactionExpanded,
            onToggleExpand = { reactionExpanded = !reactionExpanded },
            onDismiss = {
                menuMessage = null
                reactionExpanded = false
            },
            onReact = { emoji ->
                onReact(target, emoji)
                menuMessage = null
                reactionExpanded = false
            },
            onReply = { onReply(target); menuMessage = null },
            onCopy = { onCopy(target); menuMessage = null },
            onForward = { onForward(target); menuMessage = null },
            onPin = { onPinMessage(target); menuMessage = null },
            onDelete = { onDelete(target); menuMessage = null },
            onOpen = { onOpenImage(target); menuMessage = null },
        )
    }
    }
    if (showAttach) {
        AttachSheet(
            onGallery = {
                showAttach = false
                onAttachGallery()
            },
            onFile = {
                showAttach = false
                onAttachFile()
            },
            onUri = { uri ->
                showAttach = false
                onAttachUri(uri)
            },
            onDismiss = { showAttach = false },
        )
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
    onJump: (String?) -> Unit,
    highlighted: Boolean = false,
    clusterFirst: Boolean = true,
    clusterLast: Boolean = true,
    mentionNames: List<String> = emptyList(),
    selected: Boolean = false,
    selecting: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEnterSelect: () -> Unit = {},
    onTap: () -> Unit = {},
) {
    val mine = m.outgoing
    val inGroup = state.group != null
    val dark = MaterialTheme.colorScheme.background == RopeDarkBg
    val outBg = if (dark) OutBubbleDark else OutBubbleLight
    val outFg = if (dark) Color(0xFFF4F4F5) else Color.White
    val me = state.profile?.deviceId.orEmpty()
    val marked = selected || highlighted
    var appeared by remember(m.id) { mutableStateOf(false) }
    LaunchedEffect(m.id) { appeared = true }
    val appear by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "bubbleIn",
    )
    val selectScale by animateFloatAsState(
        targetValue = if (marked) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f),
        label = "bubbleSel",
    )
    val selectAlpha by animateFloatAsState(
        targetValue = when {
            highlighted -> 0.18f
            selected -> 0.12f
            else -> 0f
        },
        animationSpec = tween(180),
        label = "bubbleFlash",
    )
    val senderLabel = m.senderName.ifBlank { m.senderId.take(8) }
    val senderColor = Color(GroupChatUx.senderColorArgb(m.senderId, senderLabel))
    val showName = GroupChatUx.showSenderName(inGroup, mine, clusterFirst) && !m.deleted
    val showAvatar = inGroup && !mine && clusterLast
    val topPad = if (clusterFirst) 8.dp else 2.dp
    val corner = RopeShapes.bubble
    val tight = 8.dp
    val tail = RopeShapes.bubbleTail
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = topPad)
            .graphicsLayer {
                val s = 0.94f + 0.06f * appear
                scaleX = s * selectScale
                scaleY = s * selectScale
                alpha = appear
            },
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selecting) {
                SelectionMark(
                    selected = selected,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clickable(onClick = onToggleSelect),
                )
            }
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (mine) Arrangement.spacedBy(6.dp, Alignment.End) else Arrangement.spacedBy(6.dp),
            ) {
            if (inGroup && !mine) {
                Box(Modifier.width(28.dp), contentAlignment = Alignment.BottomCenter) {
                    if (showAvatar) {
                        InitialsAvatar(
                            title = senderLabel.ifBlank { "?" },
                            group = false,
                            online = false,
                            size = 28.dp,
                            tint = senderColor,
                            showPresence = false,
                        )
                    }
                }
            }
            val bubbleClick = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {
                        when {
                            selecting -> onToggleSelect()
                            !m.deleted -> onTap()
                        }
                    },
                    onLongClick = {
                        if (m.deleted) return@combinedClickable
                        if (selecting) onToggleSelect() else onEnterSelect()
                    },
                )
            val photo = m.kind == MessageKind.IMAGE && !m.deleted
            if (photo) {
                Box(bubbleClick) {
                    Column {
                        if (showName) {
                            Text(
                                senderLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = senderColor,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                            )
                        }
                        if (!m.replyToId.isNullOrBlank()) {
                            ReplyQuote(
                                name = GroupChatUx.replyQuoteName(m.replyName, m.outgoing),
                                preview = m.replyPreview.ifBlank { "Сообщение" },
                                accent = senderColor,
                                onClick = { onJump(m.replyToId) },
                            )
                        }
                        ImageBubble(m, onEnsureMedia, overlayMeta = true)
                    }
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(RopeShapes.media))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = selectAlpha)),
                    )
                }
            } else {
        Surface(
            color = if (mine) outBg else if (dark) InBubbleDark else InBubbleLight,
            contentColor = if (mine) outFg else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = if (!mine && !clusterFirst) tight else corner,
                topEnd = if (mine && !clusterFirst) tight else corner,
                bottomStart = when {
                    mine -> corner
                    clusterLast -> tail
                    else -> tight
                },
                bottomEnd = when {
                    !mine -> corner
                    clusterLast -> tail
                    else -> tight
                },
            ),
            modifier = bubbleClick,
        ) {
            Box {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    if (showName) {
                        Text(senderLabel, style = MaterialTheme.typography.labelMedium, color = senderColor)
                    }
                    if (!m.deleted && !m.replyToId.isNullOrBlank()) {
                        ReplyQuote(
                            name = GroupChatUx.replyQuoteName(m.replyName, m.outgoing),
                            preview = m.replyPreview.ifBlank { "Сообщение" },
                            accent = if (mine) outFg else senderColor,
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
                            MessageKind.VOICE -> VoiceMessageBubble(
                                message = m,
                                playing = state.playingVoiceId == m.id,
                                positionMs = if (state.voiceProgressId == m.id) state.voicePositionMs else 0L,
                                playerDurationMs = if (state.voiceProgressId == m.id) state.voiceDurationMs else 0L,
                                onPlay = onPlay,
                            )
                            MessageKind.FILE -> FileBubble(m)
                            MessageKind.CALL -> Text("📞 ${m.text}", style = MaterialTheme.typography.bodyMedium)
                            MessageKind.UNKNOWN -> Text(m.text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFC107))
                            else -> MentionText(
                                m.text,
                                mentionNames,
                                mentionColor = if (mine) outFg else senderColor,
                                styleLarge = m.kind == MessageKind.TEXT || m.kind == MessageKind.GROUP_TEXT,
                            )
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
            }
        }
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


@Composable
private fun MentionText(text: String, names: List<String>, mentionColor: Color, styleLarge: Boolean) {
    val spans = remember(text, names) { GroupChatUx.mentionSpans(text, names) }
    val style = if (styleLarge) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
    if (spans.isEmpty()) {
        Text(text, style = style)
        return
    }
    val annotated = buildAnnotatedString {
        append(text)
        spans.forEach { range ->
            addStyle(
                SpanStyle(color = mentionColor, fontWeight = FontWeight.SemiBold),
                range.first,
                range.last + 1,
            )
        }
    }
    Text(annotated, style = style)
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    val green = Color(0xFF43A047)
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(green)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Выбрано",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun MessageTapOverlay(
    message: ChatMessage,
    pinned: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .widthIn(max = 300.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReactionPicker(
                onPick = onReact,
                expanded = expanded,
                onToggleExpand = onToggleExpand,
            )
            MessageActionMenu(
                m = message,
                pinned = pinned,
                onReply = onReply,
                onCopy = onCopy,
                onForward = onForward,
                onPin = onPin,
                onDelete = onDelete,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun MessageActionMenu(
    m: ChatMessage,
    pinned: Boolean,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            if (ChatActions.canReply(m)) {
                MessageMenuRow(Icons.AutoMirrored.Outlined.Reply, "Ответить", onReply)
            }
            if (ChatActions.canCopy(m)) {
                MessageMenuRow(Icons.Outlined.ContentCopy, "Копировать", onCopy)
            }
            if (ChatActions.canForward(m)) {
                MessageMenuRow(Icons.AutoMirrored.Outlined.ArrowForward, "Переслать", onForward)
            }
            if (ChatActions.canPin(m)) {
                MessageMenuRow(
                    Icons.Outlined.PushPin,
                    if (pinned) "Открепить" else "Закрепить",
                    onPin,
                )
            }
            if (ChatActions.canDelete(m)) {
                MessageMenuRow(Icons.Outlined.Delete, "Удалить", onDelete)
            }
            if (ChatActions.canOpen(m)) {
                MessageMenuRow(Icons.Outlined.OpenInFull, "Открыть", onOpen)
            }
        }
    }
}

@Composable
private fun MessageMenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
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
private fun ImageBubble(
    m: ChatMessage,
    onEnsure: (ChatMessage) -> Unit,
    overlayMeta: Boolean = false,
) {
    LaunchedEffect(m.id, m.localPath) {
        onEnsure(m)
    }
    val bmp = m.localPath?.let { runCatching { ImageCodec.decodePreview(it) }.getOrNull() }
    if (bmp != null) {
        val box = PhotoLayout.box(bmp.width, bmp.height)
        val meta = MessageTime.meta(m.status, m.outgoing, m.timestampMs, edited = m.edited)
        Box(
            modifier = Modifier
                .width(box.widthDp.dp)
                .height(box.heightDp.dp)
                .clip(RoundedCornerShape(RopeShapes.media)),
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Фото",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (overlayMeta && meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
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
    var showEmoji by remember { mutableStateOf(false) }
    var localText by remember { mutableStateOf(state.draftText) }
    var lastSeenDraft by remember { mutableStateOf(state.draftText) }
    val scope = rememberCoroutineScope()
    var debounce by remember { mutableStateOf<Job?>(null) }
    val chatKey = state.group?.groupId ?: state.peer?.deviceId.orEmpty()
    LaunchedEffect(chatKey) {
        localText = state.draftText
        lastSeenDraft = state.draftText
    }
    fun persistDraft(text: String) {
        debounce?.cancel()
        debounce = scope.launch {
            delay(320)
            onDraft(text)
        }
    }
    LaunchedEffect(state.draftText) {
        if (state.draftText != lastSeenDraft) {
            lastSeenDraft = state.draftText
            if (state.draftText != localText) localText = state.draftText
        }
    }
    BackHandler(enabled = showEmoji && !state.recording) { showEmoji = false }
    BackHandler(enabled = state.recording) { onVoiceFinish(false) }
    LaunchedEffect(state.recording) {
        if (!state.recording) {
            recordingLocked = false
            slideHint = VoiceGesture.HOLD
        } else {
            showEmoji = false
        }
    }
    val showSend = ComposerRules.showSendButton(localText, state.recording, recordingLocked)
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
                    title = "Ответ · ${GroupChatUx.replyQuoteName(target.senderName, target.outgoing)}",
                    body = target.preview(),
                    onCancel = onCancelComposer,
                )
            }
            if (showEmoji && !state.recording) {
                EmojiPickerPanel(
                    onPick = {
                        localText += it
                        persistDraft(localText)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.recording && recordingLocked) {
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
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 0.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(start = 2.dp, end = 4.dp),
                        ) {
                            IconButton(onClick = onAttach) {
                                Icon(Icons.Outlined.AttachFile, contentDescription = "Вложение")
                            }
                            IconButton(onClick = { showEmoji = !showEmoji }) {
                                Icon(
                                    Icons.Outlined.Mood,
                                    contentDescription = "Смайлики",
                                    tint = if (showEmoji) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BasicTextField(
                                value = localText,
                                onValueChange = {
                                    localText = it
                                    persistDraft(it)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 5,
                                decorationBox = { inner ->
                                    Box {
                                        if (localText.isEmpty()) {
                                            Text(
                                                "Сообщение",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        inner()
                                    }
                                },
                            )
                        }
                    }
                }
                if (showSend) {
                    SendActionButton(
                        locked = recordingLocked,
                        onSend = {
                            if (recordingLocked || state.recording) {
                                onVoiceFinish(true)
                            } else {
                                debounce?.cancel()
                                onDraft(localText)
                                onSend()
                            }
                        },
                    )
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
private fun SendActionButton(
    locked: Boolean,
    onSend: () -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val reduce = rememberReduceMotion()
    val pop by animateFloatAsState(
        targetValue = if (tick % 2 == 0) 1f else 1.14f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 480f),
        label = "sendPop",
    )
    IconButton(
        onClick = {
            tick += 1
            onSend()
        },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
            if (!reduce && tick > 0) {
                RopeLogoMark(
                    size = 30.dp,
                    animate = true,
                    breathe = false,
                    replayKey = tick,
                    strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                )
            }
            Icon(
                if (locked) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Отправить",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    val s = if (reduce) 1f else pop
                    scaleX = s
                    scaleY = s
                },
            )
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
private fun SelectionReplyForwardBar(
    canReply: Boolean,
    canForward: Boolean,
    modifier: Modifier = Modifier,
    onReply: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canReply) {
            GlassActionButton(
                label = "Ответить",
                icon = Icons.AutoMirrored.Outlined.Reply,
                modifier = Modifier.weight(1f),
                onClick = onReply,
            )
        }
        if (canForward) {
            GlassActionButton(
                label = "Переслать",
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                modifier = Modifier.weight(1f),
                onClick = onForward,
            )
        }
    }
}

@Composable
private fun GlassActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                RoundedCornerShape(16.dp),
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachSheet(
    onGallery: () -> Unit,
    onFile: () -> Unit,
    onUri: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val recents = remember { recentImages(context) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = RopeShapes.card, topEnd = RopeShapes.card),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Вложение", style = MaterialTheme.typography.titleMedium)
            if (recents.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recents, key = { it.toString() }) { uri ->
                        val bmp = remember(uri) { decodeRecentThumb(context, uri) }
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(RopeShapes.media))
                                .clickable { onUri(uri) }
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Галерея", modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onFile, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Файл", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun recentImages(context: Context, limit: Int = 24): List<Uri> {
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    val uris = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && uris.size < limit) {
                val id = cursor.getLong(idCol)
                uris += ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
    }
    return uris
}

private fun decodeRecentThumb(context: Context, uri: Uri, edge: Int = 144): android.graphics.Bitmap? {
    val resolver = context.contentResolver
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) } }
    val opts = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = ImageCodec.sampleSize(
            bounds.outWidth.coerceAtLeast(1),
            bounds.outHeight.coerceAtLeast(1),
            edge,
        )
    }
    return runCatching {
        resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()
}

@Composable
fun InitialsAvatar(
    title: String,
    group: Boolean,
    online: Boolean,
    size: Dp = 46.dp,
    tint: Color? = null,
    showPresence: Boolean = true,
) {
    val letter = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val iconSize = if (size < 40.dp) 16.dp else 22.dp
    val dot = if (size < 40.dp) 8.dp else 12.dp
    val bg = tint ?: if (group) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            if (group) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(iconSize))
            } else {
                Text(
                    letter,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = if (size < 40.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (showPresence) {
            Box(
                Modifier
                    .size(dot)
                    .clip(CircleShape)
                    .background(if (online) Color(0xFF43A047) else Color(0xFF9E9E9E)),
            )
        }
    }
}

@Composable
fun ImageViewer(msg: ChatMessage, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
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
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Закрыть", tint = Color.White)
        }
    }
}
