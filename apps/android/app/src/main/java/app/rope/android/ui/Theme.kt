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

/** Near-black surfaces. Primary stays black / white. */
val RopeBlack = Color(0xFF09090B)
val RopeWhite = Color(0xFFFAFAFA)

/** Secondary: light gray on dark, dark gray in containers / outgoing bubbles. */
val RopeGrayLight = Color(0xFFD4D4D8)
val RopeGrayDark = Color(0xFF3F3F46)
val RopeLightGray = RopeGrayLight
val RopeDarkGray = RopeGrayDark
val RopeMidGray = Color(0xFFA1A1AA)

val RopeDarkBg = RopeBlack
val RopeDarkSurface = Color(0xFF18181B)
val RopeTextDark = RopeGrayLight

/** Kept so existing call sites compile; no longer a neon accent. */
val RopeDeepBlue = RopeBlack
val RopeAccentLight = RopeGrayDark
val RopeNeon = RopeGrayLight

private val LightColors = lightColorScheme(
    primary = RopeBlack,
    onPrimary = Color.White,
    secondary = RopeGrayDark,
    onSecondary = RopeWhite,
    secondaryContainer = Color(0xFFE4E4E7),
    onSecondaryContainer = RopeBlack,
    background = Color.White,
    onBackground = RopeBlack,
    surface = Color.White,
    onSurface = RopeBlack,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFA1A1AA),
    error = Color(0xFFB91C1C),
)

private val DarkColors = darkColorScheme(
    primary = RopeWhite,
    onPrimary = RopeBlack,
    secondary = RopeGrayLight,
    onSecondary = RopeBlack,
    secondaryContainer = RopeGrayDark,
    onSecondaryContainer = RopeGrayLight,
    background = RopeDarkBg,
    onBackground = RopeGrayLight,
    surface = RopeDarkSurface,
    onSurface = Color(0xFFE4E4E7),
    surfaceVariant = Color(0xFF27272A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF52525B),
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
