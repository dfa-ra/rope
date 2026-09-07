package app.rope.android

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.rope.android.data.ThemeMode

val RopeDeepBlue = Color(0xFF0F172A)
val RopeAccentLight = Color(0xFF2563EB)
val RopeDarkBg = Color(0xFF0B0F19)
val RopeDarkSurface = Color(0xFF121826)
val RopeTextDark = Color(0xFF38BDF8)
val RopeNeon = Color(0xFF00D4FF)

private val LightColors = lightColorScheme(
    primary = RopeAccentLight,
    onPrimary = Color.White,
    secondary = RopeAccentLight,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = RopeDeepBlue,
    surface = Color.White,
    onSurface = RopeDeepBlue,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFB91C1C),
)

private val DarkColors = darkColorScheme(
    primary = RopeNeon,
    onPrimary = RopeDarkBg,
    secondary = RopeNeon,
    onSecondary = RopeDarkBg,
    background = RopeDarkBg,
    onBackground = RopeTextDark,
    surface = RopeDarkSurface,
    onSurface = RopeTextDark,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF7DD3FC),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
)

/** Slightly rounder than default Material3 — Telegram-ish, not a new system. */
val RopeMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object RopeShapes {
    val bubble = 20.dp
    val bubbleTail = 6.dp
    val field = 24.dp
    val search = 22.dp
    val card = 18.dp
    val chip = 16.dp
    val quote = 12.dp
    val picker = 24.dp
    val action = 18.dp
    val media = 12.dp
}

@Composable
fun RopeTheme(
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (mode == ThemeMode.DARK) DarkColors else LightColors,
        shapes = RopeMaterialShapes,
        content = content,
    )
}
