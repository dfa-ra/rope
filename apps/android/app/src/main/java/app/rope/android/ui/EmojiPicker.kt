package app.rope.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.rope.android.RopeGrayDark
import app.rope.android.RopeShapes
import app.rope.android.data.EmojiPack
import app.rope.android.data.RecentEmojiRules
import kotlinx.coroutines.delay

/**
 * Categorized Unicode emoji grid for the composer and the expanded reaction picker.
 */
@Composable
fun EmojiPickerPanel(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
    recents: List<String> = emptyList(),
) {
    var categoryId by remember { mutableStateOf(RecentEmojiRules.initialCategory(recents)) }
    var query by remember { mutableStateOf("") }
    val shown = remember(categoryId, query, recents) {
        RecentEmojiRules.shown(categoryId, query, recents)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RopeShapes.picker))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RecentEmojiRules.tabs(recents).forEach { cat ->
                val selected = cat.id == categoryId && query.isBlank()
                Text(
                    "${cat.icon} ${cat.label}",
                    modifier = Modifier
                        .clip(RoundedCornerShape(RopeShapes.chip))
                        .background(if (selected) RopeGrayDark else Color.Transparent)
                        .clickable {
                            categoryId = cat.id
                            query = ""
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Color(0xFFD4D4D8) else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (showSearch) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                placeholder = { Text("Поиск: улыбки, еда, сердца…") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(RopeShapes.search),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(shown, key = { it }) { emoji ->
                Text(
                    emoji,
                    modifier = Modifier
                        .clickable { onPick(emoji) }
                        .padding(6.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
fun ReactionPicker(
    onPick: (String) -> Unit,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    recents: List<String> = emptyList(),
) {
    BackHandler(enabled = expanded) { onToggleExpand() }
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                EmojiPack.quickReactions.forEach { emoji ->
                    ReactionPickEmoji(emoji, onPick)
                }
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Ещё",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onToggleExpand)
                        .padding(6.dp),
                )
            }
            if (expanded) {
                EmojiPickerPanel(
                    onPick = onPick,
                    showSearch = true,
                    recents = recents,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ReactionPickEmoji(emoji: String, onPick: (String) -> Unit) {
    var pop by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pop) 1.38f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "reactPick",
    )
    Text(
        emoji,
        modifier = Modifier
            .scale(scale)
            .clickable {
                pop = true
                onPick(emoji)
            }
            .padding(8.dp),
        style = MaterialTheme.typography.headlineSmall,
    )
    LaunchedEffect(pop) {
        if (pop) {
            delay(180)
            pop = false
        }
    }
}
