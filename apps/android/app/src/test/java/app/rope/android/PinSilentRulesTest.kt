package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.PinSilentRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSilentRulesTest {
    @Test
    fun silentPinDoesNotNotifySet() {
        assertTrue(PinSilentRules.notifyOthers(pinning = true, silent = false))
        assertFalse(PinSilentRules.notifyOthers(pinning = true, silent = true))
        assertTrue(PinSilentRules.notifyOthers(pinning = false, silent = true))
        assertTrue(PinSilentRules.notifyOthers(pinning = false, silent = false))
        assertEquals("Закрепить без звука", PinSilentRules.LABEL)
        assertEquals(6, LocalStore.VERSION)
    }
}
