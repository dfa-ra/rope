package app.rope.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.rope.android.data.ImeHideRules

@Composable
fun rememberImeHideConnection(): NestedScrollConnection {
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(keyboard) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (ImeHideRules.shouldHide(available.y, ImeHideRules.isUserDrag(source.toString()))) {
                    keyboard?.hide()
                }
                return Offset.Zero
            }
        }
    }
}
