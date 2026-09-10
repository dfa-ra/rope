package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.NotifyVibRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifyVibRulesTest {
    @Test
    fun shadeVibrationDefaultsOn() {
        assertTrue(NotifyVibRules.enabledFromKv(null))
        assertTrue(NotifyVibRules.enabledFromKv("1"))
        assertFalse(NotifyVibRules.enabledFromKv("0"))
        assertEquals("rope-messages", NotifyVibRules.channelId(true))
        assertEquals("rope-messages-novib", NotifyVibRules.channelId(false))
        assertEquals("Вибрация", NotifyVibRules.TITLE)
        assertTrue(NotifyVibRules.hint().contains("шторки"))
        assertEquals(6, LocalStore.VERSION)
    }
}
