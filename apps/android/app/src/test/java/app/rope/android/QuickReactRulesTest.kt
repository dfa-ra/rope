package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.QuickReactRules
import app.rope.android.data.ReactionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickReactRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun favoriteLeadsTheTray() {
        assertEquals("❤️", QuickReactRules.parse(null))
        assertEquals("❤️", QuickReactRules.parse("nope"))
        assertEquals("👍", QuickReactRules.parse("👍"))
        assertEquals(ReactionPayload.EMOJIS, QuickReactRules.CHOICES)
        val tray = QuickReactRules.tray("👍")
        assertEquals("👍", tray.first())
        assertEquals(QuickReactRules.CHOICES.size, tray.size)
        assertEquals(QuickReactRules.CHOICES.toSet(), tray.toSet())
        assertEquals("❤️", QuickReactRules.tray(null).first())
        assertTrue(QuickReactRules.hint().contains("Первый"))
    }
}
