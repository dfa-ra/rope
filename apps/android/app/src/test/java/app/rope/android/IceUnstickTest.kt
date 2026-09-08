package app.rope.android

import app.rope.android.data.CallEffect
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMachine
import app.rope.android.data.CallSignal
import app.rope.android.data.IceUnstick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IceUnstickTest {
    @Test
    fun noProgressFallbackIsFourSeconds() {
        assertEquals(4_000L, IceUnstick.NO_PROGRESS_MS)
        assertTrue(IceUnstick.NO_PROGRESS_MS < IceUnstick.ANSWER_FAIL_MS)
        assertTrue(IceUnstick.NO_PROGRESS_MS < IceUnstick.CONNECT_FAIL_MS)
    }

    @Test
    fun noRelayAfterTimeoutFallsBackThenFails() {
        assertTrue(
            IceUnstick.shouldFallbackRelay(
                elapsedMs = IceUnstick.NO_PROGRESS_MS,
                preferRelay = true,
                hasRelayCandidate = false,
                alreadyFellBack = false,
            ),
        )
        assertFalse(
            IceUnstick.shouldFallbackRelay(
                elapsedMs = IceUnstick.NO_PROGRESS_MS - 1,
                preferRelay = true,
                hasRelayCandidate = false,
                alreadyFellBack = false,
            ),
        )
        assertFalse(
            IceUnstick.shouldFallbackRelay(
                elapsedMs = IceUnstick.NO_PROGRESS_MS,
                preferRelay = true,
                hasRelayCandidate = true,
                alreadyFellBack = false,
            ),
        )

        val stuck = IceUnstick.Snapshot(
            elapsedMs = IceUnstick.NO_PROGRESS_MS,
            ice = "CHECKING",
            preferRelay = true,
            hasRelayCandidate = false,
            alreadyFellBack = false,
            isOfferer = true,
            remoteDescriptionReady = true,
        )
        val mid = IceUnstick.decide(stuck)
        assertTrue(mid.fallbackDirect)
        assertTrue(mid.restartIce)
        assertFalse(mid.failIce)
        assertFalse(mid.failSignal)

        val late = IceUnstick.decide(
            stuck.copy(
                elapsedMs = IceUnstick.CONNECT_FAIL_MS,
                alreadyFellBack = true,
                iceRestartUsed = true,
            ),
        )
        assertTrue(late.failIce)
        assertFalse(late.fallbackDirect)
        assertFalse(late.restartIce)
    }

    @Test
    fun missingAnswerIsSignalingFailNotIceSearch() {
        val waiting = IceUnstick.decide(
            IceUnstick.Snapshot(
                elapsedMs = IceUnstick.ANSWER_FAIL_MS,
                ice = "CHECKING",
                preferRelay = true,
                remoteDescriptionReady = false,
                isOfferer = true,
            ),
        )
        assertTrue(waiting.failSignal)
        assertFalse(waiting.failIce)
        assertFalse(waiting.fallbackDirect)
        assertTrue(IceUnstick.searchingPath("CHECKING"))
        assertTrue(IceUnstick.searchingPath("NEW"))
        assertFalse(IceUnstick.searchingPath("CONNECTED"))
        assertEquals(CallLink.waitingSdpDetail(), CallLink.connectingDetail(true, remoteReady = false))
        assertEquals("WebRTC · ищем путь…", CallLink.connectingDetail(true, remoteReady = true))
        assertEquals(CallLink.fallbackDirectDetail(), CallLink.connectingDetail(true, remoteReady = true, fellBack = true))
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("CHECKING", remoteReady = false).first)
        assertEquals(CallLink.waitingSdpDetail(), CallLink.applyIce("CHECKING", remoteReady = false).second)
    }

    @Test
    fun machineFallsBackWithoutRelayThenFailsLoudly() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        m.media("bob", CallSignal.ANSWER, "c1", CallSignal(CallSignal.ANSWER, sdp = "v=0"), "alice")
        m.onIce("CHECKING", false)
        assertEquals("WebRTC · ищем путь…", m.state.media)

        val mid = m.onConnectTick(IceUnstick.NO_PROGRESS_MS)
        assertTrue(mid.contains(CallEffect.FallbackDirect))
        assertTrue(mid.contains(CallEffect.RestartIce))
        assertEquals(CallLinkState.CONNECTING, m.state.link)
        assertEquals(CallLink.fallbackDirectDetail(), m.state.media)
        assertTrue(m.state.relayFellBack)

        val again = m.onConnectTick(IceUnstick.NO_PROGRESS_MS + 1_000)
        assertTrue(again.isEmpty())

        val done = m.onConnectTick(IceUnstick.CONNECT_FAIL_MS)
        assertEquals(CallLinkState.FAILED, m.state.link)
        assertTrue(m.state.media.contains("25"))
        assertTrue(done.any { it is CallEffect.Notice && it.message.contains("25") })
        assertTrue(m.onConnectTimeout().isEmpty())
    }

    @Test
    fun machineFailsSignalingWhenAnswerNeverArrives() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        assertEquals(CallLink.waitingSdpDetail(), m.state.media)

        val mid = m.onConnectTick(IceUnstick.NO_PROGRESS_MS)
        assertTrue(mid.contains(CallEffect.FallbackDirect))
        assertTrue(m.state.live)

        val failed = m.onConnectTick(IceUnstick.ANSWER_FAIL_MS)
        assertEquals(CallLinkState.FAILED, m.state.link)
        assertEquals(CallLink.noSdpDetail(), m.state.media)
        assertTrue(failed.any { it is CallEffect.Notice && it.message.contains("сигналинг") })
        assertFalse(m.state.media.contains("ищем путь"))
    }

    private fun CallMachine.media(from: String, event: String, callId: String, sig: CallSignal, me: String) =
        onWire(from, event, callId, sig.toJson(), me)
}
