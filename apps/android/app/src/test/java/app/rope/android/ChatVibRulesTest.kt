package app.rope.android

import app.rope.android.data.ChatPrefs
import app.rope.android.data.ChatVibRules
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatVibRulesTest {
    @Test
    fun inheritStoreAndDefaults() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertTrue(ChatVibRules.parse(hasKey = false, value = false))
        assertFalse(ChatVibRules.parse(hasKey = true, value = false))
        assertTrue(ChatVibRules.parse(hasKey = true, value = true))
        assertTrue(ChatPrefs().vibrate)
        assertTrue(ChatPrefs.parse("{}").vibrate)
        assertFalse(ChatPrefs.parse("""{"vibrate":false}""").vibrate)
        assertTrue(ChatPrefs.parse(ChatPrefs(vibrate = true).toJson()).vibrate)
        assertFalse(ChatPrefs.parse(ChatPrefs(vibrate = false).toJson()).vibrate)
    }

    @Test
    fun channelAndMenu() {
        assertTrue(ChatVibRules.shouldVibrate(alert = true, chatVibrate = true))
        assertFalse(ChatVibRules.shouldVibrate(alert = true, chatVibrate = false))
        assertFalse(ChatVibRules.shouldVibrate(alert = false, chatVibrate = true))
        assertEquals("rope-messages", ChatVibRules.channelId(true))
        assertEquals("rope-messages-novib", ChatVibRules.channelId(false))
        assertEquals(4, ChatVibRules.pattern(true).size)
        assertEquals(1, ChatVibRules.pattern(false).size)
        assertEquals("Без вибрации", ChatVibRules.menuLabel(true))
        assertEquals("Вибрация", ChatVibRules.menuLabel(false))
        assertTrue(ChatVibRules.canToggle("peer-1"))
        assertFalse(ChatVibRules.canToggle(SavedMessagesRules.ID))
        assertTrue(ChatVibRules.hint().contains("вибрац", ignoreCase = true))
        assertFalse(ChatVibRules.hint().contains('\n'))
    }
}
