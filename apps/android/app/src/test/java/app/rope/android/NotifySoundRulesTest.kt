package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.NotifySoundRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifySoundRulesTest {
    @Test
    fun channelToneIsNotCallOrInApp() {
        assertEquals("rope-messages", NotifySoundRules.channelId(null))
        assertEquals("rope-messages", NotifySoundRules.channelId(NotifySoundRules.DEFAULT))
        assertEquals("rope-messages-none", NotifySoundRules.channelId(NotifySoundRules.NONE))
        assertEquals("rope-messages-ring", NotifySoundRules.channelId(NotifySoundRules.RING))
        assertTrue(NotifySoundRules.isSilent(NotifySoundRules.NONE))
        assertFalse(NotifySoundRules.isSilent(NotifySoundRules.DEFAULT))
        assertTrue(NotifySoundRules.useRingtone(NotifySoundRules.RING))
        assertFalse(NotifySoundRules.useRingtone(NotifySoundRules.DEFAULT))
        assertEquals(NotifySoundRules.DEFAULT, NotifySoundRules.normalize("bogus"))
        assertEquals(NotifySoundRules.DEFAULT, NotifySoundRules.normalize("a\nb"))
        assertEquals(null, NotifySoundRules.sanitize("a\nb"))
        assertEquals("Стандарт", NotifySoundRules.label(NotifySoundRules.DEFAULT))
        assertEquals("Звук уведомлений", NotifySoundRules.TITLE)
        assertTrue(NotifySoundRules.hint().contains("шторке"))
        assertEquals(6, LocalStore.VERSION)
    }
}
