package app.rope.android

import app.rope.android.data.ImeHideRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeHideRulesTest {
    @Test
    fun userDragHidesFlingAndTinyMovesDoNot() {
        assertTrue(ImeHideRules.isUserDrag("Drag"))
        assertTrue(ImeHideRules.isUserDrag("UserInput"))
        assertFalse(ImeHideRules.isUserDrag("Fling"))
        assertFalse(ImeHideRules.isUserDrag("Relocate"))
        assertFalse(ImeHideRules.isUserDrag(""))
        assertTrue(ImeHideRules.shouldHide(8f, userDrag = true))
        assertTrue(ImeHideRules.shouldHide(-8f, userDrag = true))
        assertTrue(ImeHideRules.shouldHide(ImeHideRules.MIN_DY, userDrag = true))
        assertFalse(ImeHideRules.shouldHide(ImeHideRules.MIN_DY - 0.5f, userDrag = true))
        assertFalse(ImeHideRules.shouldHide(20f, userDrag = false))
        assertFalse(ImeHideRules.shouldHide(0f, userDrag = true))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
