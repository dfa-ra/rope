package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatSelection
import app.rope.android.data.GroupChatUx
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.NotifyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifyRulesTest {
    @Test
    fun mutedWithoutMentionNeverAlerts() {
        assertFalse(NotifyRules.shouldAlert(chatOpen = false, appForeground = false, muted = true))
        assertFalse(NotifyRules.shouldAlert(chatOpen = true, appForeground = true, muted = true))
        assertFalse(NotifyRules.shouldAlert(chatOpen = true, appForeground = false, muted = true))
        assertFalse(NotifyRules.shouldAlert(chatOpen = false, appForeground = false, muted = false, globalMuted = true))
    }

    @Test
    fun mutedChatAlertsOnMention() {
        assertTrue(NotifyRules.shouldAlert(chatOpen = false, appForeground = false, muted = true, mentioned = true))
        assertTrue(NotifyRules.shouldAlert(chatOpen = true, appForeground = false, muted = true, mentioned = true))
        assertFalse(NotifyRules.shouldAlert(chatOpen = true, appForeground = true, muted = true, mentioned = true))
        assertFalse(
            NotifyRules.shouldAlert(
                chatOpen = false,
                appForeground = false,
                muted = true,
                globalMuted = true,
                mentioned = true,
            ),
        )
        assertEquals("Только упоминания", NotifyRules.MUTED_A11Y)
    }

    @Test
    fun mentionsMeUsesAtNameSpans() {
        val names = NotifyRules.selfMentionNames("Анна")
        assertEquals(listOf("Анна", GroupChatUx.YOU), names)
        assertTrue(NotifyRules.mentionsMe("смотри @Анна", names))
        assertTrue(NotifyRules.mentionsMe("эй @Вы", names))
        assertFalse(NotifyRules.mentionsMe("смотри Анна", names))
        assertFalse(NotifyRules.mentionsMe("", names))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun openForegroundChatIsSilent() {
        assertFalse(NotifyRules.shouldAlert(chatOpen = true, appForeground = true, muted = false))
    }

    @Test
    fun backgroundStillAlertsEvenIfThatChatIsOpen() {
        assertTrue(NotifyRules.shouldAlert(chatOpen = true, appForeground = false, muted = false))
        assertTrue(NotifyRules.shouldAlert(chatOpen = false, appForeground = false, muted = false))
        assertTrue(NotifyRules.shouldAlert(chatOpen = false, appForeground = true, muted = false))
    }

    @Test
    fun selectionTitleIsOneString() {
        assertEquals("Выбрано 1", ChatSelection.title(1))
        assertEquals("Выбрано 3", ChatSelection.title(3))
        assertEquals("Выбрано 0", ChatSelection.title(0))
    }

    @Test
    fun incomingCallLockscreenOmitsPeerName() {
        val copy = NotifyRules.incomingCallText("Анна")
        assertEquals("Входящий вызов", copy.title)
        assertEquals("Анна", copy.privateBody)
        assertEquals("", copy.publicBody)
        assertFalse(copy.publicBody.contains("Анна"))
    }

    @Test
    fun openIsOnlyForLiveImages() {
        val text = ChatMessage("1", "p", false, "hi", MessageStatus.DELIVERED_TO_DEVICE, 1L)
        val photo = text.copy(id = "2", kind = MessageKind.IMAGE)
        val gone = photo.copy(deleted = true)
        assertFalse(ChatActions.canOpen(text))
        assertTrue(ChatActions.canOpen(photo))
        assertFalse(ChatActions.canOpen(gone))
    }
}
