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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.rope.android.NavRules
import app.rope.android.UiState
import app.rope.android.data.RoleRules
import app.rope.android.data.CallInfo
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallPhase
import app.rope.android.data.Conversation

@Composable
fun CallsPane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onInvite: () -> Unit,
) {
    val recent = NavRules.callsOf(state.conversations)
    val people = NavRules.peopleOf(state.devices, state.profile?.deviceId)
    Column(Modifier.fillMaxSize()) {
        FadeIn(40) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Звонки", style = MaterialTheme.typography.titleLarge)
                Text(
                    "История по последнему звонку в чате. Чтобы позвонить — откройте личный чат.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                        FadeIn(80 + SplashTiming.staggerDelayMs(index)) {
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
                        FadeIn(120 + SplashTiming.staggerDelayMs(index)) {
                            SectionCard(Modifier.padding(horizontal = 12.dp), onClick = { onOpen(conversationOf(d)) }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InitialsAvatar(d.displayName.ifBlank { "?" }, group = false, online = d.online)
                                    Column(Modifier.weight(1f)) {
                                        Text(d.displayName.ifBlank { d.deviceId.take(8) }, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (d.online) "в сети · WebRTC" else "не в сети",
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
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit,
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
                    CallLink.heading(call.phase, call.link),
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
                    when {
                        call.media.isNotBlank() -> call.media
                        call.link == CallLinkState.CONNECTED -> "WebRTC · DTLS-SRTP"
                        call.phase == CallPhase.ACTIVE -> "WebRTC · соединяем"
                        else -> "один тап — ответить"
                    },
                    color = if (call.link == CallLinkState.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            when (call.phase) {
                CallPhase.RINGING_IN -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CircleAction("Отклонить", Color(0xFFE53935), onReject)
                    CircleAction("Ответить", Color(0xFF43A047), onAccept)
                }
                else -> Button(
                    onClick = onHangup,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Icon(Icons.Outlined.CallEnd, contentDescription = null)
                    Text("  Завершить")
                }
            }
        }
    }
}

@Composable
private fun CircleAction(label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
        ) {
            Icon(if (label == "Ответить") Icons.Outlined.Call else Icons.Outlined.CallEnd, contentDescription = label)
        }
        Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
    }
}
