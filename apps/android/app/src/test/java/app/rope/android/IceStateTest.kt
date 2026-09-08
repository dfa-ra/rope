package app.rope.android

import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IceStateTest {
    @Test
    fun connectedAndCompletedAreTalking() {
        assertEquals(CallLinkState.CONNECTED, CallLink.applyIce("CONNECTED").first)
        assertEquals(CallLinkState.CONNECTED, CallLink.applyIce("completed", viaRelay = true).first)
        assertEquals("WebRTC · через сервер", CallLink.applyIce("CONNECTED", viaRelay = true).second)
        assertEquals("WebRTC · DTLS-SRTP", CallLink.applyIce("CONNECTED", viaRelay = false).second)
    }

    @Test
    fun failedAndClosedStopConnecting() {
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("FAILED").first)
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("failed").first)
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("CLOSED").first)
        assertTrue(CallLink.iceIsFailed("FAILED"))
        assertTrue(CallLink.iceIsFailed("closed"))
        assertFalse(CallLink.iceIsFailed("CHECKING"))
        assertFalse(CallLink.iceIsFailed("CONNECTING"))
        assertTrue(CallLink.applyIce("FAILED", hasTurn = false).second.contains("ядро"))
        assertTrue(CallLink.applyIce("FAILED", viaRelay = true, hasTurn = true).second.contains("TURN"))
    }

    @Test
    fun checkingConnectingNewStayConnecting() {
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("CHECKING").first)
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("CONNECTING").first)
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("NEW").first)
        assertEquals("WebRTC · ищем путь…", CallLink.applyIce("CHECKING", hasTurn = true).second)
        assertEquals("WebRTC · ищем путь…", app.rope.android.data.CallMedia.label("CHECKING"))
        assertTrue(CallLink.applyIce("NEW", hasTurn = false).second.contains("TURN"))
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("DISCONNECTED").first)
        assertTrue(CallLink.applyIce("DISCONNECTED").second.contains("прервалась"))
    }

    @Test
    fun peerConnectionStateNamesMap() {
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("CONNECTING").first)
        assertEquals(CallLinkState.CONNECTED, CallLink.applyIce("CONNECTED").first)
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("FAILED").first)
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("CLOSED").first)
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("DISCONNECTED").first)
    }
}
