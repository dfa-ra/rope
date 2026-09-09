package app.rope.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rope.android.RopeShapes
import app.rope.android.data.GroupChatUx
import app.rope.android.data.PollRules
import app.rope.android.data.PollState
import app.rope.android.data.PollVoter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollSheet(
    onSend: (question: String, options: List<String>, multi: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    var multi by remember { mutableStateOf(false) }
    val valid = PollRules.validate(question, options)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = RopeShapes.card, topEnd = RopeShapes.card),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Опрос", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Закрыть")
                }
                TextButton(onClick = { onSend(question, PollRules.cleanOptions(options), multi) }, enabled = valid) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Отправить")
                }
            }
            TextField(
                value = question,
                onValueChange = { if (it.length <= PollRules.Q_MAX) question = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Вопрос") },
                singleLine = true,
                colors = TextFieldDefaults.colors(),
            )
            options.forEachIndexed { i, value ->
                TextField(
                    value = value,
                    onValueChange = { next ->
                        if (next.length <= PollRules.OPT_LEN) {
                            options = options.toMutableList().also { it[i] = next }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Вариант ${i + 1}") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(),
                )
            }
            if (options.size < PollRules.OPT_MAX) {
                TextButton(onClick = { options = options + "" }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Добавить вариант")
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Несколько ответов", modifier = Modifier.weight(1f))
                Switch(checked = multi, onCheckedChange = { multi = it })
            }
            Text(
                "Голоса видны участникам",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PollBubble(
    poll: PollState,
    mine: Boolean,
    accent: Color,
    headerModifier: Modifier = Modifier,
    onVote: (Int) -> Unit,
    onShowVoters: (List<PollVoter>) -> Unit,
) {
    val maxBar = poll.totalVoters.coerceAtLeast(1)
    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            poll.question,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = headerModifier.fillMaxWidth(),
        )
        poll.options.forEachIndexed { i, label ->
            val selected = i in poll.myIndexes
            val count = poll.counts.getOrElse(i) { 0 }
            val voters = poll.votersByOption.getOrElse(i) { emptyList() }
            val fraction = (count.toFloat() / maxBar).coerceIn(0f, 1f)
            val rowMod = if (poll.closed) {
                Modifier.clickable { onShowVoters(voters) }
            } else {
                Modifier.combinedClickable(
                    onClick = { onVote(i) },
                    onLongClick = { onShowVoters(voters) },
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .then(rowMod)
                    .padding(vertical = 4.dp)
                    .then(if (poll.closed) Modifier else Modifier),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Spacer(Modifier.size(18.dp))
                    }
                    Text(
                        label,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (poll.closed) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        } else {
                            Color.Unspecified
                        },
                    )
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onShowVoters(voters) },
                    )
                }
                Box(
                    Modifier
                        .padding(start = 26.dp, top = 4.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .background(accent.copy(alpha = if (mine) 0.85f else 0.7f)),
                    )
                }
            }
        }
        Text(
            PollRules.footer(poll.totalVoters, poll.closed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable {
                onShowVoters(poll.votersByOption.flatten().distinctBy { it.id })
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollVotersSheet(
    voters: List<PollVoter>,
    selfId: String,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = RopeShapes.card, topEnd = RopeShapes.card),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Голоса", style = MaterialTheme.typography.titleMedium)
            if (voters.isEmpty()) {
                Text(
                    "Пока никто не голосовал",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                voters.forEach { v ->
                    val label = if (v.id == selfId) GroupChatUx.YOU else v.name.ifBlank { v.id.take(8) }
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
