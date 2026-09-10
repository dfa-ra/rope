package app.rope.android.ui

import android.content.Intent
import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.rope.android.RopeDarkBg
import app.rope.android.RopeShapes
import app.rope.android.UiState
import app.rope.android.data.AlbumRules
import app.rope.android.data.ArchiveRules
import app.rope.android.data.ArchiveSwipeRules
import app.rope.android.data.ChatActions
import app.rope.android.data.ChatListEmptyRules
import app.rope.android.data.ChatListPreviewRules
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatThreadItem
import app.rope.android.data.DateSeparatorRules
import app.rope.android.data.ForwardRules
import app.rope.android.data.LinkPreviewRules
import app.rope.android.data.PackedLinkPreview
import app.rope.android.data.PeerProfileRules
import app.rope.android.data.QueryHighlight
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.SwipeToReplyRules
import app.rope.android.data.ThreadEmptyRules
import app.rope.android.data.VideoCallRules
import app.rope.android.data.UnreadBadgeKind
import app.rope.android.data.UnreadBadgeRules
import app.rope.android.data.UnreadFab
import app.rope.android.data.UnreadSeparatorRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatSelection
import app.rope.android.data.ComposerHintCopy
import app.rope.android.data.ComposerHintRules
import app.rope.android.data.ComposerRules
import app.rope.android.data.GroupChatUx
import app.rope.android.data.MessageSearch
import app.rope.android.data.MessageTime
import app.rope.android.data.QuoteSpan
import app.rope.android.data.QuoteSpanRules
import app.rope.android.data.Conversation
import app.rope.android.data.MediaPayload
import app.rope.android.data.MediaSendRules
import app.rope.android.data.NoteGalleryRules
import app.rope.android.data.MessageKind
import app.rope.android.data.PhotoLayout
import app.rope.android.data.ReactionCodec
import app.rope.android.data.VoiceGesture
import app.rope.android.media.ImageCodec
import app.rope.android.media.VideoCodec
import kotlin.math.roundToInt
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
    onArchiveChat: (String) -> Unit = {},
    onUnarchiveChat: (String) -> Unit = {},
    onOpenArchive: () -> Unit = {},
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
                ChatListMode.ARCHIVE -> ArchiveRules.SEARCH_PLACEHOLDER
                ChatListMode.ALL -> "Поиск"
            },
        )
        val isForwarding = state.forwarding != null
        val archived = ArchiveRules.archivedOf(state.conversations)
        val source = ArchiveRules.sourceForList(state.conversations, listMode, isForwarding)
        val rows = ChatListRules.rows(source, state.chatQuery, listMode)
        val pinnedRows = ChatListRules.pinnedBlock(rows, state.chatQuery)
        val otherRows = ChatListRules.unpinnedBlock(rows, state.chatQuery)
        val showArchiveRow = ArchiveRules.rowVisible(archived.size, listMode, isForwarding)
        val inArchive = listMode == ChatListMode.ARCHIVE
        val empty = ChatListEmptyRules.copy(
            listMode,
            state.chatQuery,
            isForwarding,
            state.profile?.role,
        )
        Box(Modifier.weight(1f).fillMaxSize()) {
            if (rows.isEmpty() && !showArchiveRow) {
                RopeEmptyState(
                    title = empty.title,
                    body = empty.body,
                    actionLabel = empty.actionLabel,
                    onAction = if (empty.actionLabel != null) onNewGroup else null,
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (showArchiveRow) {
                        item(key = "archive-row") {
                            ArchiveHeaderRow(
                                archived = archived,
                                onClick = onOpenArchive,
                            )
                        }
                    }
                    if (pinnedRows.isNotEmpty()) {
                        itemsIndexed(pinnedRows, key = { _, c -> c.id }) { _, c ->
                            FadeIn(0) {
                                ConversationRow(
                                    c,
                                    onClick = { onOpen(c) },
                                    onPin = { onPinChat(c.id) },
                                    onMute = { onMuteChat(c.id) },
                                    query = state.chatQuery,
                                    onArchive = if (inArchive || isForwarding) null else ({ onArchiveChat(c.id) }),
                                    onUnarchive = if (inArchive) ({ onUnarchiveChat(c.id) }) else null,
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
                                onArchive = if (inArchive || isForwarding) null else ({ onArchiveChat(c.id) }),
                                onUnarchive = if (inArchive) ({ onUnarchiveChat(c.id) }) else null,
                            )
                        }
                    }
                    if (inArchive) {
                        item(key = "archive-footer") {
                            Text(
                                ArchiveRules.DEVICE_ONLY,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
            if (ChatListEmptyRules.showFab(listMode, state.chatQuery, state.forwarding != null)) {
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
private fun ArchiveHeaderRow(
    archived: List<Conversation>,
    onClick: () -> Unit,
) {
    val badge = ArchiveRules.badgeKind(archived)
    val unread = ArchiveRules.unreadSum(archived)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(120),
        label = "archiveRow",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {},
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Archive,
                contentDescription = ArchiveRules.TITLE,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(ArchiveRules.TITLE, style = MaterialTheme.typography.titleMedium)
            Text(
                ArchiveRules.preview(archived),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badge != UnreadBadgeKind.NONE) {
            val badgeBg = when (badge) {
                UnreadBadgeKind.ACCENT -> MaterialTheme.colorScheme.primary
                UnreadBadgeKind.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
                UnreadBadgeKind.NONE -> Color.Transparent
            }
            val badgeFg = when (badge) {
                UnreadBadgeKind.ACCENT -> MaterialTheme.colorScheme.onPrimary
                UnreadBadgeKind.MUTED -> MaterialTheme.colorScheme.surface
                UnreadBadgeKind.NONE -> Color.Transparent
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(badgeBg)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    UnreadBadgeRules.label(unread),
                    color = badgeFg,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ConversationRow(
    c: Conversation,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit,
    query: String = "",
    onArchive: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
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
    val swipeAction = onUnarchive ?: onArchive
    val swipeEnabled = swipeAction != null && ArchiveSwipeRules.canSwipe(c.id, header = false)
    SwipeArchiveRow(
        enabled = swipeEnabled,
        unarchive = onUnarchive != null,
        onCommit = { swipeAction?.invoke(); menu = false },
    ) {
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
            InitialsAvatar(c.title, c.isGroup, c.online, saved = SavedMessagesRules.isSaved(c))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HighlightedText(
                            text = c.title,
                            query = query,
                            style = if (UnreadBadgeRules.emphasizeTitle(c.unread)) {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (c.muted) {
                            Icon(
                                Icons.Outlined.NotificationsOff,
                                contentDescription = "Без звука",
                                modifier = Modifier
                                    .padding(start = 4.dp, end = 6.dp)
                                    .size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HighlightedText(
                        text = c.subtitle,
                        query = query,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            ChatListPreviewRules.isDraft(c.subtitle) -> MaterialTheme.colorScheme.primary
                            c.last == null && c.online && !c.isGroup -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f),
                    )
                    val badge = UnreadBadgeRules.kind(c.unread, c.muted)
                    if (badge != UnreadBadgeKind.NONE) {
                        val badgeBg = when (badge) {
                            UnreadBadgeKind.ACCENT -> MaterialTheme.colorScheme.primary
                            UnreadBadgeKind.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
                            UnreadBadgeKind.NONE -> Color.Transparent
                        }
                        val badgeFg = when (badge) {
                            UnreadBadgeKind.ACCENT -> MaterialTheme.colorScheme.onPrimary
                            UnreadBadgeKind.MUTED -> MaterialTheme.colorScheme.surface
                            UnreadBadgeKind.NONE -> Color.Transparent
                        }
                        Box(
                            Modifier
                                .padding(start = 8.dp)
                                .clip(CircleShape)
                                .background(badgeBg)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                UnreadBadgeRules.label(c.unread),
                                color = badgeFg,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
        if (menu) {
            Row(
                Modifier.padding(start = 72.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onUnarchive != null) {
                    TextButton(onClick = { onUnarchive(); menu = false }) {
                        Text(ArchiveRules.UNARCHIVE)
                    }
                } else if (ArchiveRules.canPin(c)) {
                    TextButton(onClick = { onPin(); menu = false }) {
                        Text(if (c.pinned) "Открепить" else "Закрепить")
                    }
                }
                TextButton(onClick = { onMute(); menu = false }) {
                    Text(if (c.muted) "Включить звук" else "Без звука")
                }
                if (onArchive != null && ArchiveRules.canArchive(c.id)) {
                    TextButton(onClick = { onArchive(); menu = false }) {
                        Text(ArchiveRules.ARCHIVE)
                    }
                }
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
    onVideoCall: () -> Unit = {},
    onPlay: (ChatMessage) -> Unit,
    onReact: (ChatMessage, String) -> Unit,
    onEnsureMedia: (ChatMessage) -> Unit,
    onGroupInfo: () -> Unit,
    onPeerProfile: () -> Unit = {},
    onBack: () -> Unit = {},
    onReply: (ChatMessage) -> Unit = {},
    onReplySpan: (QuoteSpan?) -> Unit = {},
    onEdit: (ChatMessage) -> Unit = {},
    onDelete: (ChatMessage) -> Unit = {},
    onForward: (ChatMessage) -> Unit = {},
    onCancelComposer: () -> Unit = {},
    onDismissLinkPreview: () -> Unit = {},
    onCancelPendingMedia: () -> Unit = {},
    onToggleSendAsNote: () -> Unit = {},
    onStageAsVideoNote: (Uri) -> Unit = {},
    onCopy: (ChatMessage) -> Unit = {},
    onPinMessage: (ChatMessage) -> Unit = {},
    onJump: (String?) -> Unit = {},
    onOpenImage: (ChatMessage) -> Unit = {},
    onMessageQuery: (String) -> Unit = {},
    onConsumedScroll: () -> Unit = {},
    onAttachGallery: () -> Unit = onAttach,
    onAttachFile: () -> Unit = onAttach,
    onAttachUri: (Uri) -> Unit = {},
    onAttachUris: (List<Uri>) -> Unit = { uris -> uris.forEach(onAttachUri) },
    onSeekVoice: (ChatMessage, Long) -> Unit = { _, _ -> },
    onCycleVoiceSpeed: () -> Unit = {},
    onVideoNoteStart: () -> Unit = {},
    onVideoNoteFinish: (Boolean) -> Unit = {},
    onVideoNotePreview: (android.view.SurfaceHolder, Int) -> Unit = { _, _ -> },
    onVideoNotePreviewGone: () -> Unit = {},
) {
    val saved = SavedMessagesRules.isSaved(state.peer?.deviceId) && state.group == null
    val title = if (saved) SavedMessagesRules.TITLE else state.group?.name ?: state.peer?.displayName ?: "Чат"
    val online = state.group?.let { g ->
        g.members.any { it in state.onlineIds && it != state.profile?.deviceId }
    } ?: (state.peer?.online == true)
    val typing = state.typingName
    val subtitle = when {
        !typing.isNullOrBlank() -> typing
        saved -> SavedMessagesRules.IDLE_SUBTITLE
        state.group != null -> "${state.group.members.size} участников · ${if (online) "кто-то в сети" else "все офлайн"}"
        else -> MessageTime.lastSeenLabel(state.peer?.lastSeen.orEmpty(), online)
    }
    val mentionNames = remember(state.group, state.devices, state.profile?.displayName) {
        (state.devices.map { it.displayName } + listOfNotNull(state.profile?.displayName, GroupChatUx.YOU))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
    val visible = remember(state.messages, state.messageQuery) {
        state.messages.filter { MessageSearch.matches(it, state.messageQuery) }
    }
    val todayKey = DateSeparatorRules.dayKey(System.currentTimeMillis())
    val threadItems = remember(visible, todayKey, state.unreadAnchorId, state.messageQuery) {
        UnreadSeparatorRules.insert(
            AlbumRules.collapse(DateSeparatorRules.items(visible)),
            state.unreadAnchorId,
            searching = state.messageQuery.isNotBlank(),
        )
    }
    val list = rememberLazyListState()
    val jumpScope = rememberCoroutineScope()
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
    LaunchedEffect(threadItems.size, state.messageQuery) {
        if (threadItems.isNotEmpty() && state.scrollToMessageId == null) {
            list.animateScrollToItem(threadItems.lastIndex)
        }
    }
    LaunchedEffect(state.scrollToMessageId) {
        val id = state.scrollToMessageId ?: return@LaunchedEffect
        val idx = UnreadSeparatorRules.scrollIndex(threadItems, id, state.unreadAnchorId)
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
                    .clickable(
                        enabled = PeerProfileRules.headerClickable(
                            isGroup = state.group != null,
                            hasPeer = state.peer != null,
                            saved = saved,
                        ),
                        onClick = {
                            if (PeerProfileRules.opensPeerProfile(
                                    isGroup = state.group != null,
                                    hasPeer = state.peer != null,
                                    saved = saved,
                                )
                            ) {
                                onPeerProfile()
                            } else {
                                onGroupInfo()
                            }
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InitialsAvatar(title, state.group != null, online, saved = saved, showPresence = !saved)
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
                if (state.peer != null && VideoCallRules.showHeader(state.peer.deviceId, state.group != null)) {
                    IconButton(onClick = onCall) {
                        Icon(Icons.Outlined.Call, contentDescription = "Позвонить")
                    }
                    IconButton(onClick = onVideoCall) {
                        Icon(Icons.Outlined.Videocam, contentDescription = "Видеозвонок")
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
                val empty = ThreadEmptyRules.copy(state.messageQuery, saved = saved)
                RopeEmptyState(
                    title = empty.title,
                    body = empty.body,
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
                    items(threadItems, key = { it.key }) { item ->
                        when (item) {
                            is ChatThreadItem.Day -> DateChip(item.label)
                            is ChatThreadItem.Unread -> UnreadChip()
                            is ChatThreadItem.Bubble -> {
                                val m = item.msg
                                val index = visible.indexOfFirst { it.id == m.id }
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
                                    onSwipeReply = { onReply(m) },
                                    onSeekVoice = onSeekVoice,
                                    onCycleVoiceSpeed = onCycleVoiceSpeed,
                                )
                            }
                            is ChatThreadItem.Album -> {
                                val members = item.members
                                val first = members.first()
                                val index = visible.indexOfFirst { it.id == first.id }
                                AlbumBubble(
                                    members = members,
                                    state = state,
                                    onEnsureMedia = onEnsureMedia,
                                    onJump = onJump,
                                    onOpenImage = onOpenImage,
                                    onReact = onReact,
                                    highlighted = members.any { it.id == flashId },
                                    clusterFirst = GroupChatUx.firstInCluster(visible, index),
                                    clusterLast = GroupChatUx.lastInCluster(visible, visible.indexOfFirst { it.id == members.last().id }.coerceAtLeast(index)),
                                    selected = members.any { it.id in selectedIds },
                                    selecting = selecting,
                                    onToggleSelect = {
                                        val ids = members.map { it.id }.toSet()
                                        selectedIds = if (ids.any { it in selectedIds }) selectedIds - ids else selectedIds + ids
                                    },
                                    onEnterSelect = {
                                        menuMessage = null
                                        reactionExpanded = false
                                        selectedIds = selectedIds + members.map { it.id }
                                    },
                                    onLongPressMember = { m ->
                                        menuMessage = m
                                        reactionExpanded = false
                                    },
                                    onSwipeReply = { target -> onReply(target) },
                                )
                            }
                        }
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
            } else {
                val unreadIdx = threadItems.indexOfFirst { it is ChatThreadItem.Unread }
                val atBottom by remember { derivedStateOf { !list.canScrollForward } }
                val unreadVisible by remember(unreadIdx) {
                    derivedStateOf {
                        unreadIdx >= 0 && list.layoutInfo.visibleItemsInfo.any { it.index == unreadIdx }
                    }
                }
                val unreadAbove by remember(unreadIdx) {
                    derivedStateOf {
                        unreadIdx >= 0 && list.firstVisibleItemIndex > unreadIdx
                    }
                }
                val fabKind = UnreadSeparatorRules.fab(atBottom, unreadVisible, unreadIdx >= 0, unreadAbove)
                if (fabKind != null) {
                    FloatingActionButton(
                        onClick = {
                            jumpScope.launch {
                                val target = if (fabKind == UnreadFab.UP) {
                                    unreadIdx
                                } else {
                                    threadItems.lastIndex
                                }
                                if (target >= 0) list.animateScrollToItem(target)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp),
                        shape = CircleShape,
                    ) {
                        Icon(
                            if (fabKind == UnreadFab.UP) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (fabKind == UnreadFab.UP) "К непрочитанным" else "К последним",
                        )
                    }
                }
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
                onDismissLinkPreview = onDismissLinkPreview,
                onCancelPendingMedia = onCancelPendingMedia,
                onToggleSendAsNote = onToggleSendAsNote,
                onReplySpan = onReplySpan,
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
    if (state.recordingVideoNote) {
        VideoNoteRecorderOverlay(
            recordMs = state.recordMs,
            onPreviewReady = onVideoNotePreview,
            onPreviewGone = onVideoNotePreviewGone,
            onSend = { onVideoNoteFinish(true) },
            onCancel = { onVideoNoteFinish(false) },
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
            onUris = { uris ->
                showAttach = false
                onAttachUris(uris)
            },
            onVideoNoteUri = { uri ->
                showAttach = false
                onStageAsVideoNote(uri)
            },
            onVideoNote = {
                showAttach = false
                onVideoNoteStart()
            },
            onDismiss = { showAttach = false },
        )
    }
}

@Composable
private fun SwipeArchiveRow(
    enabled: Boolean,
    unarchive: Boolean,
    onCommit: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var liveOffset by remember { mutableFloatStateOf(0f) }
    val settle by animateFloatAsState(
        targetValue = if (dragging) liveOffset else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "swipeArchiveSettle",
    )
    val offsetDp = if (dragging) liveOffset else settle
    val progress = ArchiveSwipeRules.progress(offsetDp)
    val density = LocalDensity.current
    val view = LocalView.current
    Box(
        Modifier
            .fillMaxWidth()
            .pointerInput(enabled, unarchive) {
                if (!enabled) return@pointerInput
                val pxPerDp = density.density
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var locked = false
                    var prev = 0f
                    liveOffset = 0f
                    dragging = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val dxDp = (change.position.x - down.position.x) / pxPerDp
                        val dyDp = (change.position.y - down.position.y) / pxPerDp
                        if (!change.pressed) {
                            val commit = locked && ArchiveSwipeRules.shouldCommit(liveOffset)
                            dragging = false
                            liveOffset = 0f
                            if (commit) onCommit()
                            break
                        }
                        if (!locked) {
                            when {
                                ArchiveSwipeRules.shouldAbort(dxDp, dyDp) -> break
                                ArchiveSwipeRules.shouldLock(dxDp, dyDp) -> {
                                    locked = true
                                    dragging = true
                                }
                                else -> continue
                            }
                        }
                        event.changes.forEach { it.consume() }
                        val now = ArchiveSwipeRules.offset(dxDp)
                        if (ArchiveSwipeRules.crossedCommit(prev, now)) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        prev = now
                        liveOffset = now
                    }
                    dragging = false
                    liveOffset = 0f
                }
            },
    ) {
        Icon(
            if (unarchive) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .graphicsLayer {
                    alpha = ArchiveSwipeRules.iconAlpha(progress)
                    val s = ArchiveSwipeRules.iconScale(progress)
                    scaleX = s
                    scaleY = s
                },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset((offsetDp * density.density).roundToInt(), 0) },
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeReplyRow(
    enabled: Boolean,
    onCommit: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var liveOffset by remember { mutableFloatStateOf(0f) }
    val settle by animateFloatAsState(
        targetValue = if (dragging) liveOffset else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "swipeReplySettle",
    )
    val offsetDp = if (dragging) liveOffset else settle
    val progress = SwipeToReplyRules.progress(offsetDp)
    val density = LocalDensity.current
    val view = LocalView.current
    Box(
        Modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val pxPerDp = density.density
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var locked = false
                    var prev = 0f
                    liveOffset = 0f
                    dragging = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val dxDp = (change.position.x - down.position.x) / pxPerDp
                        val dyDp = (change.position.y - down.position.y) / pxPerDp
                        if (!change.pressed) {
                            val commit = locked && SwipeToReplyRules.shouldCommit(liveOffset)
                            dragging = false
                            liveOffset = 0f
                            if (commit) onCommit()
                            break
                        }
                        if (!locked) {
                            when {
                                SwipeToReplyRules.shouldAbort(dxDp, dyDp) -> break
                                SwipeToReplyRules.shouldLock(dxDp, dyDp) -> {
                                    locked = true
                                    dragging = true
                                }
                                else -> continue
                            }
                        }
                        event.changes.forEach { it.consume() }
                        val now = SwipeToReplyRules.offset(dxDp)
                        if (SwipeToReplyRules.crossedCommit(prev, now)) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        prev = now
                        liveOffset = now
                    }
                    dragging = false
                    liveOffset = 0f
                }
            },
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .graphicsLayer {
                    alpha = SwipeToReplyRules.iconAlpha(progress)
                    val s = SwipeToReplyRules.iconScale(progress)
                    scaleX = s
                    scaleY = s
                },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset((offsetDp * density.density).roundToInt(), 0) },
        ) {
            content()
        }
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
    onSwipeReply: () -> Unit = {},
    onSeekVoice: (ChatMessage, Long) -> Unit = { _, _ -> },
    onCycleVoiceSpeed: () -> Unit = {},
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
    SwipeReplyRow(
        enabled = SwipeToReplyRules.canSwipe(m, selecting),
        onCommit = onSwipeReply,
    ) {
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
            val mediaTile = (m.kind == MessageKind.IMAGE || m.kind == MessageKind.VIDEO) && !m.deleted
            if (mediaTile) {
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
                        AttributionChrome(
                            msg = m,
                            accent = senderColor,
                            onJump = onJump,
                        )
                        if (m.kind == MessageKind.VIDEO) {
                            VideoMessageBubble(m, onEnsureMedia, overlayMeta = true)
                        } else {
                            ImageBubble(m, onEnsureMedia, overlayMeta = true)
                        }
                        MediaCaptionLine(MediaSendRules.captionOf(m))
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
                    AttributionChrome(
                        msg = m,
                        accent = if (mine) outFg else senderColor,
                        onJump = onJump,
                    )
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
                                speed = state.voiceSpeed,
                                onPlay = onPlay,
                                onSeek = onSeekVoice,
                                onCycleSpeed = onCycleVoiceSpeed,
                            )
                            MessageKind.VIDEO_NOTE -> VideoNoteBubble(m, onEnsureMedia)
                            MessageKind.FILE -> FileBubble(m)
                            MessageKind.CALL -> Text("📞 ${m.text}", style = MaterialTheme.typography.bodyMedium)
                            MessageKind.UNKNOWN -> Text(m.text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFC107))
                            else -> {
                                val context = LocalContext.current
                                MentionText(
                                    m.text,
                                    mentionNames,
                                    mentionColor = if (mine) outFg else senderColor,
                                    styleLarge = m.kind == MessageKind.TEXT || m.kind == MessageKind.GROUP_TEXT,
                                    onPlainTap = onTap,
                                )
                                val preview = m.linkPreview
                                if (preview != null) {
                                    LaunchedEffect(m.id, preview.objectId, preview.localPath) {
                                        onEnsureMedia(m)
                                    }
                                    LinkPreviewCard(
                                        preview = preview,
                                        onOpen = { openHttps(context, it) },
                                    )
                                }
                            }
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
}

@Composable
private fun AttributionChrome(msg: ChatMessage, accent: Color, onJump: (String) -> Unit) {
    val from = ForwardRules.attributedName(msg)
    if (from != null) {
        ForwardedHeader(from)
    }
    val replyId = msg.replyToId
    if (!msg.deleted && !replyId.isNullOrBlank() && !ForwardRules.hidesReplyQuote(msg)) {
        ReplyQuote(
            name = GroupChatUx.replyQuoteName(msg.replyName, msg.outgoing),
            preview = QuoteSpanRules.displayPreview(msg.replyPreview, msg.quoteText).ifBlank { "Сообщение" },
            accent = accent,
            onClick = { onJump(replyId) },
        )
    }
}

@Composable
private fun ForwardedHeader(name: String) {
    Text(
        ForwardRules.headerLabel(name),
        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 4.dp),
    )
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
private fun MentionText(
    text: String,
    names: List<String>,
    mentionColor: Color,
    styleLarge: Boolean,
    onPlainTap: () -> Unit = {},
) {
    val spans = remember(text, names) { GroupChatUx.mentionSpans(text, names) }
    val links = remember(text) { LinkPreviewRules.spans(text) }
    val style = if (styleLarge) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    if (spans.isEmpty() && links.isEmpty()) {
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
        links.forEach { link ->
            addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                link.start,
                link.endExclusive,
            )
            addStringAnnotation("URL", link.url, link.start, link.endExclusive)
        }
    }
    ClickableText(
        text = annotated,
        style = style.copy(color = LocalContentColor.current),
        onClick = { offset ->
            val url = annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.item
            if (url != null) openHttps(context, url) else onPlainTap()
        },
    )
}

@Composable
private fun ComposerLinkPreview(preview: PackedLinkPreview, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(Modifier.weight(1f)) {
            Text(
                preview.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                preview.host,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "Убрать предпросмотр")
        }
    }
}

@Composable
private fun LinkPreviewCard(preview: PackedLinkPreview, onOpen: (String) -> Unit) {
    val bmp = preview.localPath?.let { runCatching { ImageCodec.decodePreview(it) }.getOrNull() }
    Surface(
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onOpen(preview.url) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
    ) {
        Column {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = preview.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                )
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    preview.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (preview.description.isNotBlank()) {
                    Text(
                        preview.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    preview.host.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun openHttps(context: Context, url: String) {
    if (!url.startsWith("https://", ignoreCase = true)) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumBubble(
    members: List<ChatMessage>,
    state: UiState,
    onEnsureMedia: (ChatMessage) -> Unit,
    onJump: (String?) -> Unit,
    onOpenImage: (ChatMessage) -> Unit,
    onReact: (ChatMessage, String) -> Unit,
    highlighted: Boolean,
    clusterFirst: Boolean,
    clusterLast: Boolean,
    selected: Boolean,
    selecting: Boolean,
    onToggleSelect: () -> Unit,
    onEnterSelect: () -> Unit,
    onLongPressMember: (ChatMessage) -> Unit,
    onSwipeReply: (ChatMessage) -> Unit = {},
) {
    val first = members.firstOrNull() ?: return
    val last = members.last()
    val mine = first.outgoing
    val inGroup = state.group != null
    val senderLabel = first.senderName.ifBlank { first.senderId.take(8) }
    val senderColor = Color(GroupChatUx.senderColorArgb(first.senderId, senderLabel))
    val showName = GroupChatUx.showSenderName(inGroup, mine, clusterFirst) && !first.deleted
    val selectAlpha by animateFloatAsState(if (selected) 0.28f else 0f, label = "albumSelect")
    val tiles = PhotoLayout.mosaic(members.size)
    val meta = MessageTime.meta(last.status, last.outgoing, last.timestampMs, edited = last.edited)
    val replyTarget = SwipeToReplyRules.replyTarget(members)
    SwipeReplyRow(
        enabled = SwipeToReplyRules.canSwipeAlbum(members, selecting),
        onCommit = { replyTarget?.let(onSwipeReply) },
    ) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = if (clusterFirst) 8.dp else 2.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (showName) {
            Text(
                senderLabel,
                style = MaterialTheme.typography.labelMedium,
                color = senderColor,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        AttributionChrome(
            msg = first,
            accent = senderColor,
            onJump = onJump,
        )
        Box(
            Modifier
                .width(PhotoLayout.MOSAIC_WIDTH_DP.dp)
                .height(PhotoLayout.MOSAIC_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(RopeShapes.media)),
        ) {
            members.forEachIndexed { i, m ->
                val tile = tiles.getOrElse(i) { tiles.last() }
                MosaicTile(
                    m = m,
                    tile = tile,
                    overlayMeta = i == members.lastIndex && meta.isNotBlank(),
                    meta = meta,
                    onEnsure = onEnsureMedia,
                    onClick = {
                        when {
                            selecting -> onToggleSelect()
                            else -> onOpenImage(m)
                        }
                    },
                    onLongClick = {
                        if (selecting) onToggleSelect() else {
                            onEnterSelect()
                            onLongPressMember(m)
                        }
                    },
                )
            }
            if (selectAlpha > 0f || highlighted) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (highlighted) 0.18f else selectAlpha)),
                )
            }
        }
        MediaCaptionLine(MediaSendRules.albumCaption(members))
        if (last.reactions.isNotEmpty()) {
            ReactionRow(last, state.profile?.deviceId.orEmpty(), mine, onReact)
        }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MosaicTile(
    m: ChatMessage,
    tile: PhotoLayout.Tile,
    overlayMeta: Boolean,
    meta: String,
    onEnsure: (ChatMessage) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    LaunchedEffect(m.id, m.localPath) { onEnsure(m) }
    val extra = runCatching { MediaPayload.parse(m.extra) }.getOrNull()
    val video = m.kind == MessageKind.VIDEO
    val bmp = m.localPath?.let { path ->
        if (video) VideoCodec.poster(path) else runCatching { ImageCodec.decodePreview(path) }.getOrNull()
    }
    Box(
        Modifier
            .offset(x = tile.xDp.dp, y = tile.yDp.dp)
            .width(tile.widthDp.dp)
            .height(tile.heightDp.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = if (video) "Видео" else "Фото",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (video) {
            Text(
                extra?.durationMs?.takeIf { it > 0 }?.let { MediaPayload.formatDuration(it) } ?: "видео",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        if (overlayMeta) {
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
private fun MediaCaptionLine(caption: String?) {
    val text = caption?.trim().orEmpty()
    if (text.isEmpty()) return
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .widthIn(max = PhotoLayout.MAX_WIDTH_DP.dp)
            .padding(start = 4.dp, end = 4.dp, top = 4.dp),
    )
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
    onDismissLinkPreview: () -> Unit = {},
    onCancelPendingMedia: () -> Unit = {},
    onToggleSendAsNote: () -> Unit = {},
    onReplySpan: (QuoteSpan?) -> Unit = {},
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
    val showSend = ComposerRules.showSendButton(
        localText,
        state.recording,
        recordingLocked,
        pendingMedia = state.pendingAttachments.isNotEmpty(),
    )
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
                    copy = ComposerHintRules.edit(target.preview()),
                    onCancel = onCancelComposer,
                )
            } ?: state.replyTo?.let { target ->
                ComposerHint(
                    copy = ComposerHintRules.reply(
                        name = GroupChatUx.replyQuoteName(target.senderName, target.outgoing),
                        preview = target.preview(),
                        spanText = QuoteSpanRules.preview(target.preview(), state.replySpan).takeIf {
                            state.replySpan != null
                        }.orEmpty(),
                    ),
                    onCancel = onCancelComposer,
                    sourceText = target.preview(),
                    span = state.replySpan,
                    onSpan = onReplySpan.takeIf { QuoteSpanRules.canSelect(target) },
                )
            }
            if (state.editTarget == null && state.pendingAttachments.isNotEmpty()) {
                val context = LocalContext.current
                val videos = remember(state.pendingAttachments) {
                    MediaSendRules.videoCount(
                        state.pendingAttachments.map { context.contentResolver.getType(it).orEmpty() },
                        state.pendingAttachments.map { it.lastPathSegment.orEmpty() },
                    )
                }
                val durationMs = remember(state.pendingAttachments) {
                    val uri = state.pendingAttachments.singleOrNull() ?: return@remember 0L
                    VideoCodec.durationMs(context, uri)
                }
                val asNote = NoteGalleryRules.sendAsNote(
                    state.sendPendingAsNote,
                    state.pendingAttachments.size,
                    videos,
                    durationMs,
                )
                ComposerHint(
                    copy = NoteGalleryRules.hint(asNote, state.pendingAttachments.size, videos),
                    onCancel = onCancelPendingMedia,
                )
                if (NoteGalleryRules.shows(state.pendingAttachments.size, videos, durationMs)) {
                    FilterChip(
                        selected = asNote,
                        onClick = onToggleSendAsNote,
                        label = { Text(NoteGalleryRules.ACTION) },
                        modifier = Modifier
                            .padding(start = 12.dp, bottom = 4.dp)
                            .semantics { contentDescription = NoteGalleryRules.ACTION },
                    )
                }
            }
            state.composerPreview?.takeIf { state.editTarget == null && !state.recording && !state.recordingVideoNote }?.let { preview ->
                ComposerLinkPreview(
                    preview = preview,
                    onDismiss = onDismissLinkPreview,
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
                            if (ComposerRules.showAttach(state.editTarget != null)) {
                                IconButton(onClick = onAttach) {
                                    Icon(Icons.Outlined.AttachFile, contentDescription = "Вложение")
                                }
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
                                                if (state.pendingAttachments.isNotEmpty()) {
                                                    if (state.sendPendingAsNote) {
                                                        NoteGalleryRules.TITLE
                                                    } else {
                                                        MediaSendRules.PLACEHOLDER
                                                    }
                                                } else {
                                                    "Сообщение"
                                                },
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
private fun DateChip(label: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(RopeShapes.chip))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UnreadChip() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            UnreadSeparatorRules.LABEL,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ComposerHint(
    copy: ComposerHintCopy,
    onCancel: () -> Unit,
    sourceText: String = "",
    span: QuoteSpan? = null,
    onSpan: ((QuoteSpan?) -> Unit)? = null,
) {
    val selectable = onSpan != null && QuoteSpanRules.canSelect(sourceText)
    var picking by remember(sourceText) { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var anchor by remember { mutableIntStateOf(-1) }
    val highlight = span?.let { QuoteSpanRules.clamp(sourceText, it.start, it.end) }
    val body = if (picking && selectable) sourceText else copy.body
    val annotated = buildAnnotatedString {
        append(body)
        if (picking && selectable && highlight != null && highlight.end <= body.length) {
            addStyle(
                SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                highlight.start,
                highlight.end,
            )
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(if (picking) 64.dp else 36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(Modifier.weight(1f)) {
            Text(
                copy.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                annotated,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (picking) 6 else 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onTextLayout = { layout = it },
                modifier = if (!selectable) {
                    Modifier
                } else {
                    Modifier.pointerInput(sourceText) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                picking = true
                                anchor = -1
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val measured = layout ?: return@detectDragGesturesAfterLongPress
                                if (measured.layoutInput.text.text != sourceText) return@detectDragGesturesAfterLongPress
                                val now = measured.getOffsetForPosition(change.position)
                                if (anchor < 0) anchor = now
                                val start = minOf(anchor, now)
                                val end = maxOf(anchor, now).coerceAtLeast(start + 1)
                                onSpan?.invoke(
                                    QuoteSpanRules.clamp(sourceText, start, end.coerceAtMost(sourceText.length)),
                                )
                            },
                            onDragEnd = { picking = false },
                            onDragCancel = { picking = false },
                        )
                    }
                },
            )
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Outlined.Close, contentDescription = copy.dismissContentDescription)
        }
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
    onUris: (List<Uri>) -> Unit = { uris -> uris.forEach(onUri) },
    onVideoNote: () -> Unit = {},
    onVideoNoteUri: (Uri) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val recents = remember { recentMedia(context) }
    var selected by remember { mutableStateOf(listOf<Uri>()) }
    fun toggle(uri: Uri) {
        selected = when {
            uri in selected -> selected - uri
            selected.size >= AlbumRules.MAX_PHOTOS -> selected
            else -> selected + uri
        }
    }
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
                Text(
                    "До ${AlbumRules.MAX_PHOTOS} фото или видео",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recents, key = { it.uri.toString() }) { item ->
                        val bmp = remember(item.uri) { decodeRecentThumb(context, item.uri, item.video) }
                        val order = selected.indexOf(item.uri)
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(RopeShapes.media))
                                .clickable { toggle(item.uri) }
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
                            if (item.video) {
                                Text(
                                    if (item.durationMs > 0) MediaPayload.formatDuration(item.durationMs) else "видео",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                            if (order >= 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${order + 1}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (selected.isNotEmpty()) {
                TextButton(
                    onClick = { onUris(selected) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (selected.size == 1) "Отправить" else "Отправить ${selected.size}",
                        modifier = Modifier.weight(1f),
                    )
                }
                val picked = selected.singleOrNull()?.let { uri -> recents.find { it.uri == uri } }
                if (picked != null && NoteGalleryRules.shows(1, if (picked.video) 1 else 0, picked.durationMs)) {
                    TextButton(
                        onClick = { onVideoNoteUri(picked.uri) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Videocam, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(NoteGalleryRules.ACTION, modifier = Modifier.weight(1f))
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
            TextButton(onClick = onVideoNote, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Videocam, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Видеосообщение", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class RecentMedia(
    val uri: Uri,
    val video: Boolean,
    val added: Long,
    val durationMs: Long = 0,
)

private fun recentMedia(context: Context, limit: Int = 24): List<RecentMedia> {
    val imagePerm = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val videoPerm = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val items = mutableListOf<RecentMedia>()
    if (ContextCompat.checkSelfPermission(context, imagePerm) == PackageManager.PERMISSION_GRANTED) {
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext() && items.size < limit) {
                    val id = cursor.getLong(idCol)
                    items += RecentMedia(
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        video = false,
                        added = cursor.getLong(dateCol),
                    )
                }
            }
        }
    }
    if (ContextCompat.checkSelfPermission(context, videoPerm) == PackageManager.PERMISSION_GRANTED) {
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.DURATION,
                ),
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                var n = 0
                while (cursor.moveToNext() && n < limit) {
                    val id = cursor.getLong(idCol)
                    items += RecentMedia(
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        video = true,
                        added = cursor.getLong(dateCol),
                        durationMs = cursor.getLong(durCol),
                    )
                    n++
                }
            }
        }
    }
    return items.sortedByDescending { it.added }.take(limit)
}

private fun decodeRecentThumb(
    context: Context,
    uri: Uri,
    video: Boolean = false,
    edge: Int = 144,
): android.graphics.Bitmap? {
    if (video) {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
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
    saved: Boolean = false,
) {
    val letter = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val iconSize = if (size < 40.dp) 16.dp else 22.dp
    val dot = if (size < 40.dp) 8.dp else 12.dp
    val bg = tint ?: when {
        saved -> MaterialTheme.colorScheme.primary
        group -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            when {
                saved -> Icon(
                    Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize),
                )
                group -> Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(iconSize))
                else -> Text(
                    letter,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = if (size < 40.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (showPresence && !saved) {
            Box(
                Modifier
                    .size(dot)
                    .clip(CircleShape)
                    .background(if (online) Color(0xFF43A047) else Color(0xFF9E9E9E)),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewer(
    msg: ChatMessage,
    siblings: List<ChatMessage> = listOf(msg),
    onClose: () -> Unit,
    onShow: (ChatMessage) -> Unit = {},
    onEnsure: (ChatMessage) -> Unit = {},
) {
    val album = siblings.ifEmpty { listOf(msg) }
    val start = album.indexOfFirst { it.id == msg.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = start) { album.size }
    val albumKey = album.joinToString { it.id }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage, albumKey) {
        album.getOrNull(pagerState.currentPage)?.let { current ->
            onEnsure(current)
            if (current.id != msg.id) onShow(current)
        }
    }
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f)),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = album[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                if (item.kind == MessageKind.VIDEO) {
                    val path = item.localPath
                    if (!path.isNullOrBlank()) {
                        VideoViewerSurface(path, Modifier.fillMaxWidth().padding(12.dp))
                    } else {
                        Text("Видео ещё качается", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    val bmp = item.localPath?.let { runCatching { ImageCodec.decodePreview(it) }.getOrNull() }
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
        }
        if (album.size > 1) {
            Text(
                "${pagerState.currentPage + 1} / ${album.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(album, key = { _, m -> m.id }) { index, item ->
                    val thumb = item.localPath?.let { runCatching { ImageCodec.decodePreview(it) }.getOrNull() }
                    val active = index == pagerState.currentPage
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                if (active) 2.dp else 0.dp,
                                Color.White,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .background(Color.DarkGray),
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
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
