package app.rope.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.rope.android.data.CallInfo
import app.rope.android.data.CallPhase

@Composable
fun CallOverlay(
    call: CallInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1621))
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                when (call.phase) {
                    CallPhase.RINGING_IN -> "Входящий вызов"
                    CallPhase.RINGING_OUT -> "Вызов…"
                    CallPhase.ACTIVE -> "Разговор"
                    CallPhase.ENDED -> "Завершён"
                },
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleMedium,
            )
            Box(
                Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2AABEE)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    call.peerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                )
            }
            Text(call.peerName, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(
                if (call.phase == CallPhase.ACTIVE) "аудио · E2EE сигналинг" else "один тап — ответить",
                color = Color.White.copy(alpha = 0.6f),
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
        Text(label, color = Color.White, modifier = Modifier.padding(top = 8.dp))
    }
}
