package app.rope.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.rope.android.data.ExpireRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisappearSheet(
    ttlSec: Int,
    canSet: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmFor by remember { mutableStateOf<Int?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(ExpireRules.ROW_TITLE, style = MaterialTheme.typography.titleMedium)
            if (!canSet) {
                Text(
                    ExpireRules.guestSubtitle(ttlSec),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                ExpireRules.OPTIONS.forEach { opt ->
                    val selected = opt.ttlSec == ExpireRules.normalizeTtl(ttlSec)
                    TextButton(
                        onClick = {
                            if (ttlSec <= 0 && opt.ttlSec > 0) {
                                confirmFor = opt.ttlSec
                            } else {
                                onSelect(opt.ttlSec)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (selected) "✓ ${opt.label}" else opt.label)
                    }
                }
            }
        }
    }
    confirmFor?.let { pending ->
        AlertDialog(
            onDismissRequest = { confirmFor = null },
            title = { Text(ExpireRules.ROW_TITLE) },
            text = { Text(ExpireRules.CONFIRM) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelect(pending)
                        confirmFor = null
                    },
                ) { Text("Включить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFor = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
fun DisappearSettingsRow(
    ttlSec: Int,
    canSet: Boolean,
    onOpen: () -> Unit,
) {
    val subtitle = if (canSet) ExpireRules.rowSubtitle(ttlSec) else ExpireRules.guestSubtitle(ttlSec)
    SectionCard(onClick = if (canSet) onOpen else null) {
        Text(ExpireRules.ROW_TITLE, style = MaterialTheme.typography.titleMedium)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
