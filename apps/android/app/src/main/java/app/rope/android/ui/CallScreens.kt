package app.rope.android.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.rope.android.NavRules
import app.rope.android.UiState
import app.rope.android.data.RoleRules
import app.rope.android.data.CallInfo
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallPhase
import app.rope.android.data.Conversation
import kotlinx.coroutines.delay

@Composable
fun CallsPane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onInvite: () -> Unit,
) {
    val recent = NavRules.callsOf(state.conversations)
    val people = NavRules.peopleOf(state.devices, state.profile?.deviceId)
    Column(Modifier.fillMaxSize()) {
        if (recent.isEmpty() && people.isEmpty()) {
            val role = state.profile?.role
            RopeEmptyState(
                title = "Звонков ещё не было",
                body = RoleRules.callsEmptyBody(role),
                actionLabel = RoleRules.peopleInviteAction(role),
                onAction = if (RoleRules.canInvite(role)) onInvite else null,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (recent.isNotEmpty()) {
                    item {
                        Text(
                            "Недавние",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(recent, key = { _, c -> "call-${c.id}" }) { index, c ->
                        FadeIn(0) {
                            ConversationRow(c, onClick = { onOpen(c) }, onPin = {}, onMute = {})
                        }
                    }
                }
                if (people.isNotEmpty()) {
                    item {
                        Text(
                            "Можно позвонить",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(people, key = { _, d -> "p-${d.deviceId}" }) { index, d ->
                        FadeIn(0) {
                            SectionCard(Modifier.padding(horizontal = 12.dp), onClick = { onOpen(conversationOf(d)) }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InitialsAvatar(d.displayName.ifBlank { "?" }, group = false, online = d.online)
                                    Column(Modifier.weight(1f)) {
                                        Text(d.displayName.ifBlank { d.deviceId.take(8) }, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (d.online) "в сети" else "не в сети",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(Icons.Outlined.Call, contentDescription = "Открыть чат")
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun CallOverlay(
    call: CallInfo,
    iceServersJson: String = "",
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit,
    micMuted: Boolean = false,
    speakerOn: Boolean = false,
    onToggleMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
) {
    BackHandler {
        if (call.phase == CallPhase.RINGING_IN) onReject() else onHangup()
    }
    val reduce = rememberReduceMotion()
    val inf = rememberInfiniteTransition(label = "callRing")
    val ring by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "ring",
    )
    val scale = if (reduce || call.phase == CallPhase.ACTIVE || call.phase == CallPhase.ENDED) 1f else ring
    val fallbackStart = remember(call.callId) { System.currentTimeMillis() }
    val startAt = if (call.startedAtMs > 0L) call.startedAtMs else fallbackStart
    var now by remember(call.callId) { mutableStateOf(System.currentTimeMillis()) }
    val running = call.link != CallLinkState.CONNECTED &&
        call.link != CallLinkState.FAILED &&
        call.phase != CallPhase.ENDED
    LaunchedEffect(call.callId, running) {
        while (running) {
            now = System.currentTimeMillis()
            delay(250)
        }
        now = System.currentTimeMillis()
    }
    val subtitle = CallLink.subtitle(call, iceServersJson)
    val heading = CallLink.heading(call.phase, call.link, subtitle)
    val clock = CallLink.clock(now - startAt, call.phase, call.link)
    val ice = CallLink.iceCompact(call.lastIce)
    val failed = call.link == CallLinkState.FAILED
    FadeIn(0) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RopeLogoMark(
                    size = 56.dp,
                    animate = call.phase != CallPhase.ENDED,
                    loop = call.phase != CallPhase.ACTIVE && call.phase != CallPhase.ENDED,
                    breathe = call.phase != CallPhase.ENDED,
                )
                Text(
                    heading,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Box(
                    Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        call.peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
                Text(call.peerName, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall)
                Text(
                    subtitle,
                    color = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (clock != null || ice.isNotBlank()) {
                    Text(
                        listOfNotNull(clock, ice.takeIf { it.isNotBlank() }).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            when (call.phase) {
                CallPhase.RINGING_IN -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CircleAction("Отклонить", Color(0xFFE53935), Icons.Outlined.CallEnd, onReject)
                    CircleAction("Ответить", Color(0xFF43A047), Icons.Outlined.Call, onAccept)
                }
                else -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleAction(
                        if (micMuted) "Микрофон выкл" else "Микрофон",
                        if (micMuted) Color(0xFF616161) else Color(0xFF3F3F46),
                        if (micMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                        onToggleMute,
                    )
                    CircleAction("Завершить", Color(0xFFE53935), Icons.Outlined.CallEnd, onHangup)
                    CircleAction(
                        if (speakerOn) "Динамик вкл" else "Динамик",
                        if (speakerOn) Color(0xFF1565C0) else Color(0xFF3F3F46),
                        Icons.Outlined.VolumeUp,
                        onToggleSpeaker,
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleAction(label: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White,
            ),
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
    }
}
