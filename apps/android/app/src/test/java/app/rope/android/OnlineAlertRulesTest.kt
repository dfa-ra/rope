package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.OnlineAlertRules
import app.rope.android.data.SavedMessagesRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineAlertRulesTest {
    @Test
    fun inheritStoreAndRejectControlChars() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Сообщить, когда в сети", OnlineAlertRules.LABEL)
        assertFalse(OnlineAlertRules.LABEL.contains("FCM", ignoreCase = true))
        assertFalse(OnlineAlertRules.HINT.contains("FCM", ignoreCase = true))
        val dm = "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899"
        assertEquals(dm, OnlineAlertRules.sanitizeId("  $dm  "))
        assertTrue(OnlineAlertRules.canEnable(dm))
        assertNull(OnlineAlertRules.sanitizeId("id\nbad"))
        assertNull(OnlineAlertRules.sanitizeId("id\rbad"))
        assertNull(OnlineAlertRules.sanitizeId("id\u0000bad"))
        assertNull(OnlineAlertRules.sanitizeId(""))
        assertNull(OnlineAlertRules.sanitizeId(" "))
        assertNull(OnlineAlertRules.sanitizeId("a".repeat(OnlineAlertRules.MAX_ID + 1)))
        assertNull(OnlineAlertRules.sanitizeId("id with space"))
        assertNull(OnlineAlertRules.sanitizeId("javascript:alert(1)"))
        assertNull(OnlineAlertRules.sanitizeId(SavedMessagesRules.ID))
        assertNull(OnlineAlertRules.sanitizeId(ChatIds.group("g-uuid")))
        assertFalse(OnlineAlertRules.canEnable(null))
        assertFalse(OnlineAlertRules.canEnable(dm, isGroup = true))
        assertFalse(OnlineAlertRules.canEnable(dm, saved = true))
    }

    @Test
    fun oneShotWhenPeerComesOnline() {
        assertTrue(OnlineAlertRules.appeared(wasOnline = false, nowOnline = true))
        assertFalse(OnlineAlertRules.appeared(wasOnline = true, nowOnline = true))
        assertFalse(OnlineAlertRules.appeared(wasOnline = false, nowOnline = false))
        assertFalse(OnlineAlertRules.appeared(wasOnline = true, nowOnline = false))
        assertTrue(
            OnlineAlertRules.shouldNotify(
                armed = true,
                wasOnline = false,
                nowOnline = true,
                globalMuted = false,
                watching = false,
            ),
        )
        assertFalse(
            OnlineAlertRules.shouldNotify(
                armed = false,
                wasOnline = false,
                nowOnline = true,
                globalMuted = false,
                watching = false,
            ),
        )
        assertFalse(
            OnlineAlertRules.shouldNotify(
                armed = true,
                wasOnline = false,
                nowOnline = true,
                globalMuted = true,
                watching = false,
            ),
        )
        assertFalse(
            OnlineAlertRules.shouldNotify(
                armed = true,
                wasOnline = false,
                nowOnline = true,
                globalMuted = false,
                watching = true,
            ),
        )
        assertTrue(OnlineAlertRules.shouldConsume(true, false, true))
        assertFalse(OnlineAlertRules.shouldConsume(true, true, true))
        assertFalse(OnlineAlertRules.shouldConsume(false, false, true))
        assertTrue(OnlineAlertRules.watching(appForeground = true, onThisPeer = true))
        assertFalse(OnlineAlertRules.watching(appForeground = false, onThisPeer = true))
        assertFalse(OnlineAlertRules.watching(appForeground = true, onThisPeer = false))
    }

    @Test
    fun prefsRoundTripAndConsume() {
        val legacy = JSONObject()
            .put("pinned", false)
            .put("muted", false)
            .put("unread", 0)
            .toString()
        assertFalse(ChatPrefs.parse(legacy).onlineAlert)
        val written = ChatPrefs.parse(ChatPrefs(onlineAlert = true, muted = true).toJson())
        assertTrue(written.onlineAlert)
        assertTrue(written.muted)
        assertFalse(OnlineAlertRules.consume(written).onlineAlert)
        val peer = "peer-1"
        val armed = Conversation(
            id = peer,
            title = "Анна",
            subtitle = "",
            isGroup = false,
            online = false,
            last = null,
            onlineAlert = true,
        )
        assertTrue(OnlineAlertRules.armedOf(listOf(armed), peer))
        assertFalse(OnlineAlertRules.armedOf(listOf(armed.copy(onlineAlert = false)), peer))
        assertFalse(OnlineAlertRules.armedOf(listOf(armed), "missing"))
        assertEquals("Анна", OnlineAlertRules.title("Анна"))
        assertEquals("Контакт", OnlineAlertRules.title("  "))
        assertEquals(OnlineAlertRules.notifyId(peer), OnlineAlertRules.notifyId(peer))
        assertTrue(OnlineAlertRules.present(peer, setOf(peer)))
        assertFalse(OnlineAlertRules.present(peer, setOf("other")))
        assertFalse(OnlineAlertRules.present("id\nbad", setOf("id\nbad")))
    }
}
