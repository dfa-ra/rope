package app.rope.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.rope.android.data.MediaSpoilerRules

fun Modifier.mediaSpoilerBlur(hidden: Boolean): Modifier =
    if (hidden) blur(24.dp) else this

@Composable
fun MediaSpoilerScrim(hidden: Boolean, modifier: Modifier = Modifier) {
    if (!hidden) return
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.VisibilityOff,
            contentDescription = MediaSpoilerRules.LABEL,
            tint = Color.White,
            modifier = Modifier.size(36.dp),
        )
    }
}
