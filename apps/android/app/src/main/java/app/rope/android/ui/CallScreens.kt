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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.SpeakerPhone
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rope.android.NavRules
import app.rope.android.UiState
import app.rope.android.data.RoleRules
import app.rope.android.data.CallChromeRules
import app.rope.android.data.CallControlKind
import app.rope.android.data.CallControlSpec
import app.rope.android.data.CallInfo
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMedia
import app.rope.android.data.CallPhase
import app.rope.android.data.Conversation
import app.rope.android.data.NicknameRules
import kotlinx.coroutines.delay
import org.webrtc.EglBase
import org.webrtc.VideoSink
import app.rope.android.data.VideoCallRules
import app.rope.android.media.CallVideoRenderer

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
                            SectionCard(Modifier.padding(horizontal = 12.dp), onClick = { onOpen(conversationOf(d, state.nicks[d.deviceId])) }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    val shown = NicknameRules.display(
                                        state.nicks[d.deviceId],
                                        d.displayName,
                                        d.deviceId,
                                    )
                                    InitialsAvatar(shown.ifBlank { "?" }, group = false, online = d.online)
                                    Column(Modifier.weight(1f)) {
                                        Text(shown, style = MaterialTheme.typography.titleMedium)
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
    camMuted: Boolean = false,
    banner: String? = null,
    onToggleMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onFlipCamera: () -> Unit = {},
    localMirror: Boolean = true,
    eglContext: () -> EglBase.Context? = { null },
    rtcReady: Boolean = false,
    onBindRemote: (VideoSink) -> Unit = {},
    onBindLocal: (VideoSink) -> Unit = {},
    onUnbindRemote: (VideoSink) -> Unit = {},
    onUnbindLocal: (VideoSink) -> Unit = {},
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
    val heading = CallLink.heading(call.phase, call.link, subtitle, call.video)
    val clock = CallLink.clock(now - startAt, call.phase, call.link)
    val ice = if (VideoCallRules.showIceCompact(call.link, call.media)) {
        CallLink.iceCompact(call.lastIce)
    } else {
        ""
    }
    val failed = call.link == CallLinkState.FAILED
    val liveVideo = call.video && call.phase == CallPhase.ACTIVE && call.media != CallMedia.CHAT
    val showVideo = VideoCallRules.mountCallRenderer(
        video = call.video,
        phase = call.phase,
        media = call.media,
        rtcReady = rtcReady,
    )
    // FadeIn uses graphicsLayer; TextureView draws black inside an offscreen layer.
    Box(
        Modifier
            .fillMaxSize()
            .background(
                if (liveVideo) Color.Black else MaterialTheme.colorScheme.background,
            ),
    ) {
        if (showVideo) {
            CallVideoView(
                modifier = Modifier.fillMaxSize(),
                mirror = false,
                eglContext = eglContext,
                onBind = onBindRemote,
                onUnbind = onUnbindRemote,
            )
            if (!camMuted) {
                CallVideoView(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .width(112.dp)
                        .height(160.dp),
                    mirror = localMirror,
                    eglContext = eglContext,
                    onBind = onBindLocal,
                    onUnbind = onUnbindLocal,
                    cornerRadiusDp = 12f,
                )
            }
        }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!liveVideo) {
                        RopeLogoMark(
                            size = 56.dp,
                            animate = call.phase != CallPhase.ENDED,
                            loop = call.phase != CallPhase.ACTIVE && call.phase != CallPhase.ENDED,
                            breathe = call.phase != CallPhase.ENDED,
                        )
                    }
                    Text(
                        heading,
                        color = (if (liveVideo) Color.White else MaterialTheme.colorScheme.onSurface)
                            .copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (!liveVideo) {
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
                    }
                    Text(
                        call.peerName,
                        color = if (liveVideo) Color.White else MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        subtitle,
                        color = if (failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            (if (liveVideo) Color.White else MaterialTheme.colorScheme.onSurface)
                                .copy(alpha = 0.7f)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!banner.isNullOrBlank()) {
                        Text(
                            banner,
                            color = if (liveVideo) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (clock != null || ice.isNotBlank()) {
                        Text(
                            listOfNotNull(clock, ice.takeIf { it.isNotBlank() }).joinToString(" · "),
                            color = (if (liveVideo) Color.White else MaterialTheme.colorScheme.onSurface)
                                .copy(alpha = 0.55f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                when (call.phase) {
                    CallPhase.RINGING_IN -> Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (spec in CallChromeRules.ringingControls()) {
                            CircleAction(
                                spec,
                                micMuted,
                                speakerOn,
                                camMuted,
                                onClick = callControlClick(
                                    spec.kind,
                                    onAccept,
                                    onReject,
                                    onHangup,
                                    onToggleMute,
                                    onToggleSpeaker,
                                    onToggleCamera,
                                    onFlipCamera,
                                ),
                            )
                        }
                    }
                    else -> Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (spec in CallChromeRules.inCallControls(
                            video = call.video && call.media != CallMedia.CHAT,
                            micMuted = micMuted,
                            speakerOn = speakerOn,
                            camMuted = camMuted,
                            showFlip = CallChromeRules.showFlip(
                                camMuted = camMuted,
                                rtcReady = eglContext() != null && call.media != CallMedia.CHAT,
                            ),
                        )) {
                            CircleAction(
                                spec,
                                micMuted,
                                speakerOn,
                                camMuted,
                                onClick = callControlClick(
                                    spec.kind,
                                    onAccept,
                                    onReject,
                                    onHangup,
                                    onToggleMute,
                                    onToggleSpeaker,
                                    onToggleCamera,
                                    onFlipCamera,
                                ),
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun CallVideoView(
    modifier: Modifier,
    mirror: Boolean,
    eglContext: () -> EglBase.Context?,
    onBind: (VideoSink) -> Unit,
    onUnbind: (VideoSink) -> Unit,
    cornerRadiusDp: Float = 0f,
) {
    val egl = eglContext() ?: return
    val radiusPx = with(LocalDensity.current) { cornerRadiusDp.dp.toPx() }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CallVideoRenderer(ctx).apply {
                if (radiusPx > 0f) roundCorners(radiusPx)
                init(egl, mirror)
                onBind(this)
            }
        },
        update = { view -> view.setMirror(mirror) },
        onRelease = { view ->
            onUnbind(view)
            view.release()
        },
    )
}

@Composable
private fun CircleAction(
    spec: CallControlSpec,
    micMuted: Boolean,
    speakerOn: Boolean,
    camMuted: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = callControlColor(spec.kind, micMuted, speakerOn, camMuted),
            contentColor = Color.White,
        ),
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(callControlIcon(spec.kind, micMuted, camMuted), contentDescription = spec.a11y)
    }
}

private fun callControlClick(
    kind: CallControlKind,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
): () -> Unit = when (kind) {
    CallControlKind.ACCEPT -> onAccept
    CallControlKind.REJECT -> onReject
    CallControlKind.HANGUP -> onHangup
    CallControlKind.MUTE -> onToggleMute
    CallControlKind.SPEAKER -> onToggleSpeaker
    CallControlKind.CAMERA -> onToggleCamera
    CallControlKind.FLIP -> onFlipCamera
}

private fun callControlColor(
    kind: CallControlKind,
    micMuted: Boolean,
    speakerOn: Boolean,
    camMuted: Boolean,
): Color = when (kind) {
    CallControlKind.REJECT, CallControlKind.HANGUP -> Color(0xFFE53935)
    CallControlKind.ACCEPT -> Color(0xFF43A047)
    CallControlKind.MUTE -> if (micMuted) Color(0xFF616161) else Color(0xFF3F3F46)
    CallControlKind.CAMERA -> if (camMuted) Color(0xFF616161) else Color(0xFF3F3F46)
    CallControlKind.SPEAKER -> if (speakerOn) Color(0xFF1565C0) else Color(0xFF3F3F46)
    CallControlKind.FLIP -> Color(0xFF3F3F46)
}

private fun callControlIcon(
    kind: CallControlKind,
    micMuted: Boolean,
    camMuted: Boolean,
): ImageVector = when (kind) {
    CallControlKind.REJECT, CallControlKind.HANGUP -> Icons.Outlined.CallEnd
    CallControlKind.ACCEPT -> Icons.Outlined.Call
    CallControlKind.MUTE -> if (micMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic
    CallControlKind.CAMERA -> if (camMuted) Icons.Outlined.VideocamOff else Icons.Outlined.Videocam
    CallControlKind.FLIP -> Icons.Outlined.Cameraswitch
    CallControlKind.SPEAKER -> Icons.Outlined.SpeakerPhone
}
