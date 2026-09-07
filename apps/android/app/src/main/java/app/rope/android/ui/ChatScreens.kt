package app.rope.android.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.ChatMessage
import app.rope.android.data.ComposerRules
import app.rope.android.data.Conversation
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind

private val OutBubble = Color(0xFF2AABEE)
private val InBubbleLight = Color(0xFFE8EDF2)
private val InBubbleDark = Color(0xFF182533)

@Composable
fun ChatsPane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onNewGroup: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (state.conversations.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Пока никого нет", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Пригласите человека QR-кодом или создайте группу.",
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
                        timeLabel(it.timestampMs),
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
    onGroupInfo: () -> Unit,
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
                MessageBubble(m, state, onPlay)
            }
        }
        ComposerBar(state, onDraft, onSend, onAttach, onVoiceStart, onVoiceFinish)
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, state: UiState, onPlay: (ChatMessage) -> Unit) {
    val mine = m.outgoing
    val dark = MaterialTheme.colorScheme.background == Color(0xFF17212B)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (mine) OutBubble else if (dark) InBubbleDark else InBubbleLight,
            contentColor = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mine) 16.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (!mine && state.group != null) {
                    Text(m.senderName.ifBlank { m.senderId.take(8) }, style = MaterialTheme.typography.labelMedium, color = OutBubble)
                }
                when (m.kind) {
                    MessageKind.VOICE -> VoiceBubble(m, state.playingVoiceId == m.id, onPlay)
                    MessageKind.IMAGE -> ImageBubble(m)
                    MessageKind.FILE -> FileBubble(m)
                    MessageKind.UNKNOWN -> Text(m.text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFC107))
                    else -> Text(m.text, style = MaterialTheme.typography.bodyLarge)
                }
                val mark = ComposerRules.statusLabel(m.status, m.outgoing)
                if (mark.isNotBlank()) {
                    Text(
                        mark,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceBubble(m: ChatMessage, playing: Boolean, onPlay: (ChatMessage) -> Unit) {
    val extra = runCatching { MediaPayload.parse(m.extra) }.getOrNull()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { if (m.localPath != null) onPlay(m) }, modifier = Modifier.size(36.dp)) {
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
                extra?.let { MediaPayload.formatDuration(it.durationMs) } ?: m.text,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ImageBubble(m: ChatMessage) {
    val bmp = m.localPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
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
        Text(m.text, style = MaterialTheme.typography.bodyMedium)
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
) {
    val showSend = ComposerRules.showSendButton(state.draftText, state.recording)
    Surface(tonalElevation = 2.dp) {
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

@Composable
fun InitialsAvatar(title: String, group: Boolean, online: Boolean) {
    val letter = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (group) Color(0xFF0088CC) else Color(0xFF2AABEE)),
            contentAlignment = Alignment.Center,
        ) {
            if (group) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Text(letter, color = Color.White, style = MaterialTheme.typography.titleMedium)
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

private fun timeLabel(ms: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    return "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
}
