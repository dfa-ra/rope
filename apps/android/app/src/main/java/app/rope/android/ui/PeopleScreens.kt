package app.rope.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.rope.android.NavRules
import app.rope.android.RopeShapes
import app.rope.android.UiState
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.PeopleSearchRules
import app.rope.android.data.QueryHighlight
import app.rope.android.data.RevokeRules
import app.rope.android.data.RoleRules

@Composable
fun PeoplePane(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onInvite: () -> Unit,
    onRevokeMember: (String) -> Unit = {},
) {
    val people = NavRules.peopleOf(state.devices, state.profile?.deviceId)
    val canRevoke = RevokeRules.canRevoke(state.profile?.role)
    var query by remember { mutableStateOf("") }
    var pendingMemberId by remember { mutableStateOf<String?>(null) }
    val rows = PeopleSearchRules.rows(people, query)
    Column(Modifier.fillMaxSize()) {
        if (PeopleSearchRules.showSearch(people)) {
            PeopleSearchField(value = query, onValueChange = { query = it })
        }
        if (people.isEmpty()) {
            val role = state.profile?.role
            RopeEmptyState(
                title = "Пока никого нет",
                body = RoleRules.peopleEmptyHint(role),
                actionLabel = RoleRules.peopleInviteAction(role),
                onAction = if (RoleRules.canInvite(role)) onInvite else null,
            )
        } else if (rows.isEmpty()) {
            RopeEmptyState(
                title = PeopleSearchRules.SEARCH_TITLE,
                body = PeopleSearchRules.searchBody(query),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (canRevoke && !PeopleSearchRules.searching(query)) {
                    item {
                        Text(
                            RevokeRules.peopleHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                itemsIndexed(rows, key = { _, d -> d.deviceId }) { _, d ->
                    FadeIn(0) {
                        PersonRow(
                            d = d,
                            query = query,
                            showRevoke = RevokeRules.canRevokeTarget(
                                state.profile?.role,
                                state.profile?.memberId,
                                state.profile?.deviceId,
                                d,
                            ),
                            confirming = pendingMemberId == d.memberId,
                            onOpen = {
                                if (pendingMemberId != d.memberId) onOpen(conversationOf(d))
                            },
                            onAskRevoke = { pendingMemberId = d.memberId },
                            onConfirmRevoke = {
                                onRevokeMember(d.memberId)
                                pendingMemberId = null
                            },
                            onCancelRevoke = { pendingMemberId = null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = PeopleSearchRules.PLACEHOLDER },
        decorationBox = { inner ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(RopeShapes.search))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            PeopleSearchRules.PLACEHOLDER,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Очистить",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PersonRow(
    d: DirectoryDevice,
    query: String,
    showRevoke: Boolean,
    confirming: Boolean,
    onOpen: () -> Unit,
    onAskRevoke: () -> Unit,
    onConfirmRevoke: () -> Unit,
    onCancelRevoke: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .pressScale(onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialsAvatar(d.displayName.ifBlank { "?" }, group = false, online = d.online)
            Column(Modifier.weight(1f)) {
                PeopleNameText(PeopleSearchRules.displayName(d), query)
                Text(
                    if (confirming) RevokeRules.confirmPrompt(d.displayName.ifBlank { d.deviceId.take(8) })
                    else if (d.online) "в сети" else "не в сети",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (confirming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (confirming) {
                    Text(
                        RevokeRules.confirmBody(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (showRevoke) {
            if (confirming) {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = onConfirmRevoke,
                        modifier = Modifier.semantics { contentDescription = RevokeRules.confirmAction() },
                    ) {
                        Text(RevokeRules.confirmAction(), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onCancelRevoke) {
                        Text(RevokeRules.cancelAction())
                    }
                }
            } else {
                TextButton(
                    onClick = onAskRevoke,
                    modifier = Modifier.semantics { contentDescription = RevokeRules.actionLabel() },
                ) {
                    Text(RevokeRules.actionLabel(), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PeopleNameText(name: String, query: String) {
    val style = MaterialTheme.typography.titleMedium
    val range = QueryHighlight.firstRange(name, query)
    val inBounds = range != null && range.first >= 0 && range.last < name.length
    if (range == null || !inBounds) {
        Text(name, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    val annotated = buildAnnotatedString {
        append(name.substring(0, range.first))
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
            append(name.substring(range.first, range.last + 1))
        }
        append(name.substring(range.last + 1))
    }
    Text(annotated, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

internal fun conversationOf(d: DirectoryDevice): Conversation = Conversation(
    id = d.deviceId,
    title = PeopleSearchRules.displayName(d),
    subtitle = if (d.online) "в сети" else "не в сети",
    isGroup = false,
    online = d.online,
    last = null,
    peer = d,
)
