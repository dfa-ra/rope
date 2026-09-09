package app.rope.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rope.android.data.ChatMessage
import app.rope.android.data.DateJumpRules

@Composable
fun DateJumpDialog(
    messages: List<ChatMessage>,
    onJump: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val days = DateJumpRules.choices(messages)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(DateJumpRules.TITLE) },
        text = {
            if (days.isEmpty()) {
                Text(DateJumpRules.EMPTY)
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(days, key = { it.dayKey }) { day ->
                        Text(
                            day.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onJump(day.messageId)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp)
                                .semantics { contentDescription = day.label },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(DateJumpRules.CLOSE)
            }
        },
    )
}
