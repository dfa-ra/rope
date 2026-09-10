package app.rope.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rope.android.UiState
import app.rope.android.data.AppLockRules

@Composable
fun AppLockPane(
    state: UiState,
    onUnlockPin: (String) -> Unit,
    onUnlockBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    val blocked = AppLockRules.inputBlocked(System.currentTimeMillis(), state.appLock.lockedUntilMs)
    val need = state.appLock.pinLen.coerceIn(AppLockRules.PIN_MIN, AppLockRules.PIN_MAX)
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InitialsAvatar(
            title = state.profile?.displayName.orEmpty().ifBlank { "Rope" },
            group = false,
            online = false,
            size = 72.dp,
            showPresence = false,
        )
        Spacer(Modifier.height(16.dp))
        Text(AppLockRules.SECTION, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "•".repeat(pin.length.coerceAtLeast(1)),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { contentDescription = "PIN" },
        )
        val err = state.lockError
        if (!err.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { d ->
                    PadKey(d, enabled = !blocked) {
                        pin = appendDigit(pin, d, need, onUnlockPin)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.appLock.biometric) {
                Icon(
                    Icons.Outlined.Fingerprint,
                    contentDescription = AppLockRules.BIO_TITLE,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !blocked, onClick = onUnlockBiometric)
                        .padding(12.dp),
                )
            } else {
                Spacer(Modifier.size(48.dp))
            }
            PadKey("0", enabled = !blocked) {
                pin = appendDigit(pin, "0", need, onUnlockPin)
            }
            Icon(
                Icons.Outlined.Backspace,
                contentDescription = "Стереть",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = pin.isNotEmpty()) { pin = pin.dropLast(1) }
                    .padding(12.dp),
            )
        }
    }
}

private fun appendDigit(
    pin: String,
    digit: String,
    need: Int,
    onUnlockPin: (String) -> Unit,
): String {
    if (pin.length >= need) return pin
    val next = pin + digit
    if (next.length == need) onUnlockPin(next)
    return if (next.length == need) "" else next
}

@Composable
private fun PadKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
            .semantics { contentDescription = "Цифра $label" },
    )
}
