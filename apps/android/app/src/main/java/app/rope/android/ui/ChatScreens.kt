package app.rope.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.ComposerRules
import app.rope.android.data.MessageTime
import app.rope.android.data.Conversation
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.ReactionCodec
import app.rope.android.data.ReactionPayload
import app.rope.android.media.ImageCodec

private val OutBubbleLight = Color(0xFF2563EB)
private val OutBubbleDark = Color(0xFF00D4FF)
private val InBubbleLight = Color(0xFFF1F5F9)
private val InBubbleDark = Color(0xFF1E293B)

@Composable
fun ChatsPane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onNewGroup: () -> Unit,
    onUpdateApp: () -> Unit = {},
    onCancelForward: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        val forwarding = state.forwarding
        when {
            forwarding != null -> {
                Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
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
        Box(Modifier.weight(1f).fillMaxSize()) {
            if (state.conversations.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Пока никого нет", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.forwarding != null) {
                            "Некуда переслать. Пригласите человека или создайте группу."
                        } else {
                            "Пригласите человека QR-кодом или создайте группу."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.conversations, key = { it.id }) { c ->
                        ConversationRow(c) { onOpen(c) }
                    }
                }
            }
            if (state.forwarding == null) {
                FloatingActionButton(
                    onClick = onNewGroup,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Новая группа")
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(c: Conversation, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(c.title, c.isGroup, c.online)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
) {
    val title = state.group?.name ?: state.peer?.displayName ?: "Чат"
    val online = state.group?.let { g ->
        g.members.any { it in state.onlineIds && it != state.profile?.deviceId }
    } ?: (state.peer?.online == true)
    val subtitle = when {
        state.group != null -> "${state.group.members.size} участников · ${if (online) "кто-то в сети" else "все офлайн"}"
        online -> "в сети"
        else -> "не в сети — дойдёт, когда появится"
    }
    val list = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.lastIndex)
    }
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
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.peer != null && state.group == null) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Outlined.Call, contentDescription = "Позвонить")
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
            items(state.messages, key = { it.id }) { m ->
                MessageBubble(m, state, onPlay, onReact, onEnsureMedia, onReply, onEdit, onDelete, onForward)
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
) {
    val mine = m.outgoing
    val dark = MaterialTheme.colorScheme.background == Color(0xFF0B0F19)
    val outBg = if (dark) OutBubbleDark else OutBubbleLight
    val outFg = if (dark) Color(0xFF0B0F19) else Color.White
    var picker by remember(m.id) { mutableStateOf(false) }
    val me = state.profile?.deviceId.orEmpty()
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (mine) outBg else if (dark) InBubbleDark else InBubbleLight,
            contentColor = if (mine) outFg else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mine) 16.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 16.dp,
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (!m.deleted) picker = !picker },
                ),
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (!mine && state.group != null && !m.deleted) {
                    Text(m.senderName.ifBlank { m.senderId.take(8) }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (!m.deleted && !m.replyToId.isNullOrBlank()) {
                    ReplyQuote(
                        name = m.replyName.ifBlank { "Ответ" },
                        preview = m.replyPreview.ifBlank { "Сообщение" },
                        accent = if (mine) outFg else MaterialTheme.colorScheme.primary,
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
                        MessageKind.IMAGE -> ImageBubble(m, onEnsureMedia)
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
        }
        if (picker && !m.deleted) {
            ReactionPicker { emoji ->
                onReact(m, emoji)
                picker = false
            }
            MessageActionRow(
                m,
                onReply = { onReply(m); picker = false },
                onForward = { onForward(m); picker = false },
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
private fun ReplyQuote(name: String, preview: String, accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.16f))
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
    onReply: () -> Unit,
    onForward: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
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
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            ReactionPayload.EMOJIS.forEach { emoji ->
                Text(
                    emoji,
                    modifier = Modifier
                        .clickable { onPick(emoji) }
                        .padding(6.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
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
            Surface(
                color = if (mineHere) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { onReact(m, emoji) },
            ) {
                Text(
                    "$emoji ${people.size}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (mine) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun VoiceBubble(m: ChatMessage, playing: Boolean, onPlay: (ChatMessage) -> Unit) {
    val extra = runCatching { MediaPayload.parse(m.extra) }.getOrNull()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { onPlay(m) }, modifier = Modifier.size(36.dp)) {
            Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = "Голос")
        }
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
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
private fun ImageBubble(m: ChatMessage, onEnsure: (ChatMessage) -> Unit) {
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
                .clip(RoundedCornerShape(8.dp)),
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
        Icon(Icons.Outlined.InsertDriveFile, contentDescription = null)
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
    val showSend = ComposerRules.showSendButton(state.draftText, state.recording)
    Surface(tonalElevation = 2.dp) {
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
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onAttach, enabled = !state.recording) {
                Icon(Icons.Outlined.AttachFile, contentDescription = "Вложение")
            }
            if (state.recording) {
                Text(
                    "Запись ${MediaPayload.formatDuration(state.recordMs)}  ·  влево — отмена",
                    color = Color(0xFFE53935),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 12.dp),
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
                    shape = RoundedCornerShape(22.dp),
                    maxLines = 5,
                )
            }
            if (showSend) {
                IconButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Отправить")
                }
            } else {
                Box(
                    Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                onVoiceStart()
                                var cancel = false
                                drag(down.id) { change ->
                                    if (change.position.x - down.position.x < -80f) cancel = true
                                    change.consume()
                                }
                                onVoiceFinish(!cancel)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Голосовое: удерживайте",
                        tint = if (state.recording) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        }
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

