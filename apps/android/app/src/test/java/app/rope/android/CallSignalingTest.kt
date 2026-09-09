package app.rope.android

import app.rope.android.data.CallEffect
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallMachine
import app.rope.android.data.CallMedia
import app.rope.android.data.CallPhase
import app.rope.android.data.CallRtcRole
import app.rope.android.data.CallSignal
import app.rope.android.data.VideoCallRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallSignalingTest {
    private fun offer(sdp: String = "v=0") = CallSignal(CallSignal.OFFER, sdp = sdp)
    private fun answer(sdp: String = "v=0") = CallSignal(CallSignal.ANSWER, sdp = sdp)
    private fun ice(cand: String = "typ host") =
        CallSignal(CallSignal.ICE, candidate = cand, sdpMid = "0", sdpMLineIndex = 0)

    private fun CallMachine.media(from: String, event: String, callId: String, sig: CallSignal, me: String) =
        onWire(from, event, callId, sig.toJson(), me)

    @Test
    fun callerOrderRingAcceptOfferAnswerIceConnected() {
        val m = CallMachine()
        val start = m.localStart("c1", "bob", "alice")
        assertTrue(start.any { it is CallEffect.Send && it.event == CallSignal.RING })
        assertTrue(start.contains(CallEffect.RingOut))
        assertEquals(CallPhase.RINGING_OUT, m.state.phase)

        val accept = m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        assertTrue(accept.any { it is CallEffect.StartRtc && it.asCaller })
        assertEquals(CallPhase.ACTIVE, m.state.phase)
        assertEquals(CallRtcRole.OFFERER, m.state.role)

        assertTrue(m.onSessionAttached().none { it is CallEffect.DeliverRemote })
        assertTrue(m.onLocalOfferSent().none { it is CallEffect.DeliverRemote })

        val ans = m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        val delivered = ans.filterIsInstance<CallEffect.DeliverRemote>().single()
        assertEquals(listOf(CallSignal.ANSWER), delivered.signals.map { it.kind })

        val trickle = m.media("bob", CallSignal.ICE, "c1", ice(), "alice")
        assertTrue(trickle.any { it is CallEffect.DeliverRemote })

        m.onIce("CHECKING", false)
        assertEquals(CallLinkState.CONNECTING, m.state.link)
        m.onIce("CONNECTED", false)
        assertEquals(CallLinkState.CONNECTED, m.state.link)
        assertEquals("Разговор", CallLink.heading(m.state.phase, m.state.link))
    }

    @Test
    fun calleeOrderRingAcceptOfferIce() {
        val m = CallMachine()
        val ring = m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        assertTrue(ring.any { it is CallEffect.NotifyIncoming })
        assertTrue(ring.contains(CallEffect.RingIn))
        assertEquals(CallPhase.RINGING_IN, m.state.phase)

        val accept = m.localAccept()
        assertTrue(accept.any { it is CallEffect.Send && it.event == CallSignal.ACCEPT })
        assertTrue(accept.any { it is CallEffect.StartRtc && !it.asCaller })
        assertEquals(CallRtcRole.ANSWERER, m.state.role)

        m.onSessionAttached()
        val off = m.media("alice", CallSignal.OFFER, "c1", offer(), "bob")
        assertEquals(
            listOf(CallSignal.OFFER),
            off.filterIsInstance<CallEffect.DeliverRemote>().single().signals.map { it.kind },
        )
    }

    @Test
    fun answerBeforeLocalOfferIsQueuedThenFlushed() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")

        val early = m.media("bob", CallSignal.ANSWER, "c1", answer("v=early"), "alice")
        assertTrue(early.none { it is CallEffect.DeliverRemote })
        assertEquals(1, m.state.queue.size)
        assertEquals(CallSignal.ANSWER, m.state.queue.single().kind)

        m.onSessionAttached()
        assertEquals(1, m.state.queue.size)

        val flushed = m.onLocalOfferSent()
        val delivered = flushed.filterIsInstance<CallEffect.DeliverRemote>().single()
        assertEquals("v=early", delivered.signals.single().sdp)
        assertTrue(m.state.queue.isEmpty())
        assertTrue(m.state.remoteDescriptionReady)
    }

    @Test
    fun iceBeforeRemoteDescriptionIsQueuedUntilOffer() {
        val m = CallMachine()
        m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        m.localAccept()

        val earlyIce = m.media("alice", CallSignal.ICE, "c1", ice("typ relay"), "bob")
        assertTrue(earlyIce.none { it is CallEffect.DeliverRemote })
        assertEquals(1, m.state.queue.size)

        m.media("alice", CallSignal.OFFER, "c1", offer(), "bob")
        assertEquals(2, m.state.queue.size)

        val flushed = m.onSessionAttached()
        val delivered = flushed.filterIsInstance<CallEffect.DeliverRemote>().single().signals
        assertEquals(listOf(CallSignal.OFFER, CallSignal.ICE), delivered.map { it.kind })
        assertTrue(m.state.queue.isEmpty())
    }

    @Test
    fun glarePicksOneOffererAndSharedCallId() {
        val a = CallMachine()
        val b = CallMachine()
        a.localStart("call-aaa", "bbb", "aaa")
        b.localStart("call-bbb", "aaa", "bbb")

        val aGlare = a.onWire("bbb", CallSignal.RING, "call-bbb", "", "aaa")
        val bGlare = b.onWire("aaa", CallSignal.RING, "call-aaa", "", "bbb")

        val shared = CallLink.canonicalCallId("call-aaa", "call-bbb")
        assertEquals(shared, a.state.callId)
        assertEquals(shared, b.state.callId)
        assertTrue(a.state.altCallId.isNotBlank())
        assertTrue(b.state.altCallId.isNotBlank())

        val aStarts = aGlare.filterIsInstance<CallEffect.StartRtc>().single()
        val bStarts = bGlare.filterIsInstance<CallEffect.StartRtc>().single()
        assertTrue(aStarts.asCaller)
        assertFalse(bStarts.asCaller)
        assertEquals(CallRtcRole.OFFERER, a.state.role)
        assertEquals(CallRtcRole.ANSWERER, b.state.role)

        b.onSessionAttached()
        val offerOnAlt = b.media("aaa", CallSignal.OFFER, "call-aaa", offer(), "bbb")
        assertTrue(offerOnAlt.any { it is CallEffect.DeliverRemote })
    }

    @Test
    fun hangupTearsDownAndDropsQueuedAnswer() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        assertTrue(m.state.queue.isNotEmpty())

        val hang = m.localHangup()
        assertTrue(hang.any { it is CallEffect.Send && it.event == CallSignal.HANGUP })
        assertTrue(hang.contains(CallEffect.TearDown))
        assertFalse(m.state.live)
        assertTrue(m.state.queue.isEmpty())

        val after = m.media("bob", CallSignal.ANSWER, "c1", answer("v=late"), "alice")
        assertTrue(after.isEmpty())
        assertFalse(m.state.live)
    }

    @Test
    fun rejectTearsDownBothSides() {
        val callee = CallMachine()
        callee.onWire("alice", CallSignal.RING, "c1", "", "bob")
        val reject = callee.localReject()
        assertTrue(reject.any { it is CallEffect.Send && it.event == CallSignal.REJECT })
        assertTrue(reject.contains(CallEffect.TearDown))
        assertFalse(callee.state.live)

        val caller = CallMachine()
        caller.localStart("c1", "bob", "alice")
        val remote = caller.onWire("bob", CallSignal.REJECT, "c1", "", "alice")
        assertEquals(listOf(CallEffect.TearDown), remote)
        assertFalse(caller.state.live)
    }

    @Test
    fun remoteHangupAlwaysTearsDownSamePeer() {
        val m = CallMachine()
        m.localStart("mine", "bob", "alice")
        m.onWire("bob", CallSignal.RING, "theirs", "", "alice")
        assertTrue(m.state.live)
        val end = m.onWire("bob", CallSignal.HANGUP, "theirs", "", "alice")
        assertEquals(listOf(CallEffect.TearDown), end)
        assertFalse(m.state.live)
    }

    @Test
    fun connectTimeoutFallsBackToWssNotFailed() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        m.onIce("CHECKING", false)

        val first = m.onConnectTimeout()
        assertEquals(CallLinkState.CONNECTING, m.state.link)
        assertTrue(m.state.wssMedia)
        assertEquals(CallMedia.CHAT, m.state.media)
        assertTrue(first.contains(CallEffect.StartWssMedia))
        assertTrue(first.contains(CallEffect.StopTone))
        assertTrue(first.any { it is CallEffect.Send && it.event == CallSignal.RELAY })
        assertFalse(first.contains(CallEffect.RestartIce))
        assertTrue(m.state.live)
        assertEquals("", m.state.lastIce)

        val again = m.onConnectTimeout()
        assertTrue(again.isEmpty())

        val ack = m.onWire("bob", CallSignal.RELAY, "c1", "", "alice")
        assertEquals(CallLinkState.CONNECTED, m.state.link)
        assertTrue(ack.contains(CallEffect.CancelWatch))
        assertEquals("Разговор", CallLink.heading(m.state.phase, m.state.link))
    }

    @Test
    fun iceFailedAfterRestartStartsWss() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        val restart = m.onIce("FAILED", false)
        assertTrue(restart.contains(CallEffect.RestartIce))
        assertFalse(m.state.wssMedia)
        val fallback = m.onIce("FAILED", false)
        assertTrue(fallback.contains(CallEffect.StartWssMedia))
        assertTrue(fallback.contains(CallEffect.StopTone))
        assertTrue(m.state.wssMedia)
        assertEquals(CallLinkState.CONNECTING, m.state.link)
        val connected = m.onIce("CONNECTED", true)
        assertTrue(connected.isEmpty())
        assertEquals(CallLinkState.CONNECTING, m.state.link)
    }

    @Test
    fun audioAndRelayAreNotQueued() {
        assertFalse(
            CallLink.shouldQueueSignal(
                CallSignal.AUDIO,
                sessionReady = false,
                localOfferReady = false,
                remoteDescriptionReady = false,
            ),
        )
        assertFalse(
            CallLink.shouldQueueSignal(
                CallSignal.RELAY,
                sessionReady = false,
                localOfferReady = false,
                remoteDescriptionReady = false,
            ),
        )
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        val audio = m.onWire(
            "bob",
            CallSignal.AUDIO,
            "c1",
            CallSignal(kind = CallSignal.AUDIO, frame = "YWJj").toJson(),
            "alice",
        )
        assertTrue(m.state.wssMedia)
        assertTrue(audio.any { it is CallEffect.DeliverAudio })
        assertTrue(m.state.queue.none { it.kind == CallSignal.AUDIO })
    }

    @Test
    fun parsesAudioAndRelayWithoutSdp() {
        val audio = CallSignal(kind = CallSignal.AUDIO, frame = "YWJj")
        val parsed = CallSignal.parse(audio.toJson())!!
        assertEquals(CallSignal.AUDIO, parsed.kind)
        assertEquals("YWJj", parsed.frame)
        assertTrue(parsed.sdp.isBlank())
        assertEquals(CallSignal.AUDIO, CallSignal.parseEvent("AUDIO"))
        assertEquals(CallSignal.RELAY, CallSignal.parseEvent("relay"))
        val relay = CallSignal.parseMedia(CallSignal.RELAY, "")!!
        assertEquals(CallSignal.RELAY, relay.kind)
        val viaMedia = CallSignal.parseMedia(CallSignal.AUDIO, audio.toJson())!!
        assertEquals("YWJj", viaMedia.frame)
        assertTrue(CallSignal.skipMailbox(CallSignal.AUDIO))
        assertTrue(CallSignal.skipMailbox(CallSignal.RELAY))
        assertFalse(CallSignal.skipMailbox(CallSignal.OFFER))
        assertNull(CallSignal.parse("""{"kind":"offer","sdp":""}"""))
    }

    @Test
    fun outgoingRingTimeoutStaysFailedOnScreen() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        val timed = m.onRingTimeout()
        assertEquals(CallLinkState.FAILED, m.state.link)
        assertEquals(CallLink.noAnswerDetail(), m.state.media)
        assertTrue(m.state.live)
        assertTrue(timed.any { it is CallEffect.Send && it.event == CallSignal.HANGUP })
        assertFalse(timed.contains(CallEffect.TearDown))
        assertEquals("Нет ответа", CallLink.heading(m.state.phase, m.state.link, m.state.media))
    }

    @Test
    fun offlineRingStaysFailedOnScreen() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        val failed = m.onRingSendFailed()
        assertEquals(CallLinkState.FAILED, m.state.link)
        assertEquals(CallLink.offlineDetail(), m.state.media)
        assertTrue(m.state.live)
        assertFalse(failed.contains(CallEffect.TearDown))
        assertEquals("Не в сети", CallLink.heading(m.state.phase, m.state.link, m.state.media))
    }

    @Test
    fun droppedRingRecoveredByEarlyOffer() {
        val m = CallMachine()
        val effects = m.media("alice", CallSignal.OFFER, "c1", offer(), "bob")
        assertEquals(CallPhase.RINGING_IN, m.state.phase)
        assertEquals(1, m.state.queue.size)
        assertTrue(effects.any { it is CallEffect.NotifyIncoming })
        m.localAccept()
        val flushed = m.onSessionAttached()
        assertEquals(
            CallSignal.OFFER,
            flushed.filterIsInstance<CallEffect.DeliverRemote>().single().signals.single().kind,
        )
    }

    @Test
    fun envelopeType4AndWssPayloadParseTheSame() {
        val sig = offer("v=offer")
        val body = JSONObject(CallSignal.envelopeJson("c9", CallSignal.OFFER, sig.toJson()))
        assertEquals("c9", body.getString("call_id"))
        assertEquals(CallSignal.OFFER, body.getString("event"))
        val parsed = CallSignal.parseMedia(body.getString("event"), body.opt("payload"))!!
        assertEquals(CallSignal.OFFER, parsed.kind)
        assertEquals("v=offer", parsed.sdp)
        assertEquals(CallSignal.RING, CallSignal.parseEvent("RING"))
        assertEquals(null, CallSignal.parse("""{"kind":"ring"}"""))
    }

    @Test
    fun startWhileIncomingIsAccept() {
        val m = CallMachine()
        m.onWire("alice", CallSignal.RING, "c1", "", "bob")
        val effects = m.localStart("ignored", "alice", "bob")
        assertTrue(effects.any { it is CallEffect.Send && it.event == CallSignal.ACCEPT })
        assertEquals(CallPhase.ACTIVE, m.state.phase)
        assertEquals("c1", m.state.callId)
    }

    @Test
    fun mixedCaseFromStillMatchesLiveCall() {
        val m = CallMachine()
        m.localStart("c1", "BOB", "ALICE")
        assertEquals("bob", m.state.peerDeviceId)
        val accept = m.onWire("Bob", CallSignal.ACCEPT, "c1", "", "Alice")
        assertTrue(accept.any { it is CallEffect.StartRtc && it.asCaller })
        m.onSessionAttached()
        m.onLocalOfferSent()
        val ans = m.media("BOB", CallSignal.ANSWER, "c1", answer(), "alice")
        assertTrue(ans.any { it is CallEffect.DeliverRemote })
    }

    @Test
    fun glareWithMixedCasePicksOneOfferer() {
        val a = CallMachine()
        val b = CallMachine()
        a.localStart("call-aaa", "BBB", "AAA")
        b.localStart("call-bbb", "AAA", "BBB")
        val aGlare = a.onWire("Bbb", CallSignal.RING, "call-bbb", "", "Aaa")
        val bGlare = b.onWire("Aaa", CallSignal.RING, "call-aaa", "", "Bbb")
        val aStarts = aGlare.filterIsInstance<CallEffect.StartRtc>().single()
        val bStarts = bGlare.filterIsInstance<CallEffect.StartRtc>().single()
        assertTrue(aStarts.asCaller xor bStarts.asCaller)
        assertEquals(a.state.callId, b.state.callId)
    }

    @Test
    fun groupChatIdIsNotACallTarget() {
        val m = CallMachine()
        val start = m.localStart("c1", "g:crew", "alice")
        assertTrue(start.isEmpty())
        assertFalse(m.state.live)
    }

    @Test
    fun wssFallbackIncludesStopTone() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice")
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        m.onIce("CHECKING", false)
        val fallback = m.onConnectTimeout()
        assertTrue(fallback.contains(CallEffect.StopTone))
        assertTrue(fallback.contains(CallEffect.StartWssMedia))
        val again = m.onConnectTimeout()
        assertFalse(again.contains(CallEffect.StopTone))
        assertFalse(again.contains(CallEffect.StartWssMedia))
    }

    @Test
    fun videoRingMarksStateAndStartRtc() {
        val m = CallMachine()
        val start = m.localStart("c1", "bob", "alice", video = true)
        assertTrue(m.state.video)
        val ring = start.filterIsInstance<CallEffect.Send>().single { it.event == CallSignal.RING }
        assertTrue(VideoCallRules.parseRingVideo(ring.payload))
        val accept = m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        val rtc = accept.filterIsInstance<CallEffect.StartRtc>().single()
        assertTrue(rtc.asCaller)
        assertTrue(rtc.video)
    }

    @Test
    fun incomingVideoRingFromPayload() {
        val m = CallMachine()
        m.onWire("bob", CallSignal.RING, "c1", VideoCallRules.ringPayload(true), "alice")
        assertTrue(m.state.video)
        assertEquals(CallPhase.RINGING_IN, m.state.phase)
        val accept = m.localAccept()
        assertTrue(accept.filterIsInstance<CallEffect.StartRtc>().single().video)
    }

    @Test
    fun offerSdpCanUpgradeToVideo() {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice", video = false)
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onSessionAttached()
        val sdp = "v=0\nm=audio 9 UDP/TLS/RTP/SAVPF 111\nm=video 9 UDP/TLS/RTP/SAVPF 96\n"
        m.media("bob", CallSignal.OFFER, "c1", CallSignal(CallSignal.OFFER, sdp = sdp), "alice")
        assertTrue(m.state.video)
    }

    @Test
    fun localHangupFromActiveSendsEndAndClears() {
        val m = connectedCall()
        val hang = m.localHangup()
        val send = hang.filterIsInstance<CallEffect.Send>().single { it.event == CallSignal.HANGUP }
        assertEquals("c1", send.callId)
        assertEquals("bob", send.peerId)
        assertTrue(hang.contains(CallEffect.TearDown))
        assertFalse(m.state.live)
        assertNull(m.snapshot("Bob"))
    }

    @Test
    fun remoteHangupAndByeClearCallWithoutSending() {
        val hangup = connectedCall()
        val remote = hangup.onWire("bob", CallSignal.HANGUP, "c1", "", "alice")
        assertEquals(listOf(CallEffect.TearDown), remote)
        assertFalse(hangup.state.live)

        val bye = connectedCall()
        assertEquals(CallSignal.HANGUP, CallSignal.parseEvent("bye"))
        assertEquals(CallSignal.HANGUP, CallSignal.parseEvent("BYE"))
        val end = bye.onWire("bob", "bye", "c1", "", "alice")
        assertEquals(listOf(CallEffect.TearDown), end)
        assertFalse(bye.state.live)
        assertNull(bye.snapshot("Bob"))
    }

    @Test
    fun iceBlipAfterConnectedDoesNotHangupOrFallback() {
        val m = connectedCall()
        assertTrue(m.state.mediaUp)
        val disc = m.onIce("DISCONNECTED", true)
        assertTrue(disc.none { it is CallEffect.TearDown || it is CallEffect.StartWssMedia })
        assertTrue(disc.none { it is CallEffect.Send && it.event == CallSignal.HANGUP })
        assertTrue(m.state.live)
        assertEquals(CallLinkState.CONNECTED, m.state.link)
        assertFalse(m.state.wssMedia)

        val closed = m.onIce("CLOSED", true)
        assertTrue(closed.isEmpty())
        assertTrue(m.state.live)
        assertEquals(CallLinkState.CONNECTED, m.state.link)

        val failed = m.onIce("FAILED", true)
        assertTrue(failed.contains(CallEffect.RestartIce))
        assertTrue(failed.none { it is CallEffect.TearDown || it is CallEffect.StartWssMedia })
        assertTrue(failed.none { it is CallEffect.Send && it.event == CallSignal.HANGUP })
        assertTrue(m.state.live)
        assertFalse(m.state.wssMedia)
        assertEquals(CallLinkState.CONNECTED, m.state.link)

        val failedAgain = m.onIce("FAILED", true)
        assertTrue(failedAgain.none { it is CallEffect.TearDown || it is CallEffect.StartWssMedia })
        assertTrue(m.state.live)
        assertFalse(m.state.wssMedia)
    }

    @Test
    fun muteFlipCameraDenyAreNotHangupSignals() {
        assertFalse(VideoCallRules.sdpErrorFailsIce())
        assertTrue(VideoCallRules.iceBlipKeepsCall("FAILED", mediaWasUp = true))
        assertTrue(VideoCallRules.iceBlipKeepsCall("DISCONNECTED", mediaWasUp = true))
        assertTrue(VideoCallRules.iceBlipKeepsCall("CLOSED", mediaWasUp = false))
        assertTrue(VideoCallRules.ignorePcClosed("CLOSED"))
        assertFalse(VideoCallRules.iceFailedFallsBackToWss("FAILED", mediaWasUp = true))
        assertTrue(VideoCallRules.iceFailedFallsBackToWss("FAILED", mediaWasUp = false))
        assertFalse(VideoCallRules.wireEventIsHangup(CallSignal.ICE))
        assertTrue(VideoCallRules.wireEventIsHangup(CallSignal.HANGUP))
        assertTrue(VideoCallRules.wireEventIsHangup("bye"))
    }

    private fun connectedCall(): CallMachine {
        val m = CallMachine()
        m.localStart("c1", "bob", "alice", video = true)
        m.onWire("bob", CallSignal.ACCEPT, "c1", "", "alice")
        m.onHasTurn(true, "")
        m.onSessionAttached()
        m.onLocalOfferSent()
        m.media("bob", CallSignal.ANSWER, "c1", answer(), "alice")
        m.onIce("CONNECTED", true)
        assertEquals(CallLinkState.CONNECTED, m.state.link)
        assertTrue(m.state.mediaUp)
        assertTrue(m.state.live)
        return m
    }
}
