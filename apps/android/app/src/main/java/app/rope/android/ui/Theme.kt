package app.rope.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF2AABEE)
private val DarkBg = Color(0xFF17212B)
private val DarkSurface = Color(0xFF232E3C)

@Composable
fun RopeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(primary = Blue, background = DarkBg, surface = DarkSurface)
        } else {
            lightColorScheme(primary = Blue)
        },
        content = content,
    )
}
