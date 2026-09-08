package app.rope.android

import app.rope.android.data.CallInfo
import app.rope.android.data.CallLink
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallPhase
import app.rope.android.data.CallSignal
import app.rope.android.data.IceServerSpec
import app.rope.android.data.IceServers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallLinkTest {
    @Test
    fun headingMapsConnectingInsteadOfFakeTalk() {
        assertEquals("Входящий вызов", CallLink.heading(CallPhase.RINGING_IN, CallLinkState.RINGING))
        assertEquals("Вызов…", CallLink.heading(CallPhase.RINGING_OUT, CallLinkState.RINGING))
        assertEquals("Соединение…", CallLink.heading(CallPhase.ACTIVE, CallLinkState.CONNECTING))
        assertEquals("Разговор", CallLink.heading(CallPhase.ACTIVE, CallLinkState.CONNECTED))
        assertEquals("Нет соединения", CallLink.heading(CallPhase.ACTIVE, CallLinkState.FAILED))
        assertEquals("Завершён", CallLink.heading(CallPhase.ENDED, CallLinkState.FAILED))
        assertEquals("Нет ответа", CallLink.heading(CallPhase.RINGING_OUT, CallLinkState.FAILED))
        assertEquals("Не в сети", CallLink.heading(CallPhase.RINGING_OUT, CallLinkState.FAILED, CallLink.offlineDetail()))
    }

    @Test
    fun connectingTimesOutAfter25sUnlessConnected() {
        assertFalse(CallLink.timedOut(0, CallLinkState.CONNECTING))
        assertFalse(CallLink.timedOut(24_999, CallLinkState.CONNECTING))
        assertTrue(CallLink.timedOut(25_000, CallLinkState.CONNECTING))
        assertTrue(CallLink.timedOut(30_000, CallLinkState.RINGING))
        assertFalse(CallLink.timedOut(60_000, CallLinkState.CONNECTED))
        assertFalse(CallLink.timedOut(60_000, CallLinkState.FAILED))
    }

    @Test
    fun iceStatesMapToUiAndFailedCopy() {
        assertEquals(CallLinkState.CONNECTED, CallLink.applyIce("CONNECTED").first)
        assertEquals(CallLinkState.CONNECTED, CallLink.applyIce("completed", viaRelay = true).first)
        assertEquals("WebRTC · через сервер", CallLink.applyIce("CONNECTED", viaRelay = true).second)
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("FAILED").first)
        assertTrue(CallLink.applyIce("FAILED", hasTurn = false).second.contains("ядро"))
        assertEquals(CallLinkState.FAILED, CallLink.applyIce("CLOSED").first)
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("DISCONNECTED").first)
        assertTrue(CallLink.applyIce("DISCONNECTED").second.contains("прервалась"))
        assertEquals(CallLinkState.CONNECTING, CallLink.applyIce("CHECKING").first)
        assertTrue(CallLink.applyIce("NEW", hasTurn = false).second.contains("TURN"))
        assertTrue(CallLink.applyIce("FAILED").second.contains("ICE failed"))
        assertTrue(CallLink.applyIce("FAILED", viaRelay = true).second.contains("через сервер"))
        assertEquals("ICE FAILED", CallLink.iceCompact("failed"))
        assertEquals("", CallLink.iceCompact(" "))
    }

    @Test
    fun timeoutCopyMentionsCoreWhenTurnMissing() {
        val missing = CallLink.timeoutDetail(hasTurn = false)
        assertTrue(missing.contains("TURN"))
        assertTrue(missing.contains("ядро"))
        val withTurn = CallLink.timeoutDetail(hasTurn = true)
        assertTrue(withTurn.contains("3478"))
        assertTrue(CallLink.missingTurnDetail().contains("обновите ядро"))
    }

    @Test
    fun glarePicksStableOfferer() {
        assertTrue(CallLink.weCreateOffer("aaa", "bbb"))
        assertFalse(CallLink.weCreateOffer("bbb", "aaa"))
        assertFalse(CallLink.weCreateOffer("", "bbb"))
        assertEquals("aaa-1", CallLink.canonicalCallId("aaa-1", "zzz-2"))
        assertEquals("aaa-1", CallLink.canonicalCallId("zzz-2", "aaa-1"))
        assertTrue(CallLink.matchesCall("theirs", "bob", "mine", "theirs", "bob"))
        assertFalse(CallLink.matchesCall("other", "eve", "mine", "theirs", "bob"))
        assertTrue(CallLink.weCreateOffer("AAA", "bbb"))
        assertFalse(CallLink.weCreateOffer("BBB", "aaa"))
        assertEquals(CallLink.weCreateOffer("AbC", "def"), CallLink.weCreateOffer("abc", "DEF"))
        assertTrue(CallLink.matchesCall("c1", "BOB", "c1", "", "bob"))
        assertFalse(CallLink.matchesCall("other", "BOB", "c1", "", "eve"))
    }

    @Test
    fun queuesAnswerAndIceUntilDescriptionsReady() {
        assertTrue(
            CallLink.shouldQueueSignal(
                CallSignal.ANSWER,
                sessionReady = false,
                localOfferReady = false,
                remoteDescriptionReady = false,
            ),
        )
        assertTrue(
            CallLink.shouldQueueSignal(
                CallSignal.ANSWER,
                sessionReady = true,
                localOfferReady = false,
                remoteDescriptionReady = false,
            ),
        )
        assertFalse(
            CallLink.shouldQueueSignal(
                CallSignal.ANSWER,
                sessionReady = true,
                localOfferReady = true,
                remoteDescriptionReady = false,
            ),
        )
        assertTrue(
            CallLink.shouldQueueSignal(
                CallSignal.ICE,
                sessionReady = true,
                localOfferReady = true,
                remoteDescriptionReady = false,
            ),
        )
        assertFalse(
            CallLink.shouldQueueSignal(
                CallSignal.ICE,
                sessionReady = true,
                localOfferReady = true,
                remoteDescriptionReady = true,
            ),
        )
        assertFalse(
            CallLink.shouldQueueSignal(
                CallSignal.OFFER,
                sessionReady = true,
                localOfferReady = false,
                remoteDescriptionReady = false,
            ),
        )
        assertTrue(CallLink.ringTimedOut(45_000, CallPhase.RINGING_OUT))
        assertFalse(CallLink.ringTimedOut(44_999, CallPhase.RINGING_IN))
        assertEquals("абонент не ответил", CallLink.ringTimeoutDetail(true))
    }

    @Test
    fun callSignalRejectsNullSdpAndAcceptsObjectPayload() {
        assertNull(CallSignal.parse("""{"kind":"offer","sdp":null}"""))
        assertNull(CallSignal.parse("""{"kind":"answer","sdp":""}"""))
        assertNull(CallSignal.parse("null"))
        val obj = JSONObject()
            .put("kind", "offer")
            .put("sdp", "v=0")
        val parsed = CallSignal.parsePayload(obj)!!
        assertEquals(CallSignal.OFFER, parsed.kind)
        assertEquals("v=0", parsed.sdp)
        assertNull(CallSignal.parsePayload(null))
        assertNull(CallSignal.parsePayload("null"))
    }

    @Test
    fun resolveHintsVpsStunWhenInfoHasNoIce() {
        val resolved = IceServers.resolve(emptyList(), "203.0.113.9")
        assertTrue(resolved.any { it.urls.contains("stun:203.0.113.9:3478") })
        assertTrue(IceServers.missingTurn(resolved))
        assertNull(IceServers.stunHint("localhost"))
        assertEquals(listOf("stun:[2001:db8::1]:3478"), IceServers.stunHint("2001:db8::1")!!.urls)
        assertFalse(IceServers.missingTurn(listOf(IceServerSpec(listOf("turn:vps:3478"), "u", "c"))))
    }

    @Test
    fun missingIceServersShowsBeforeConnectTimeout() {
        assertEquals("абонент не ответил", CallLink.noAnswerDetail())
        assertEquals("абонент не в сети", CallLink.offlineDetail())
        assertTrue(CallLink.missingIceServersDetail().contains("нет TURN"))
        assertTrue(CallLink.missingIceServersDetail().contains("ice_servers"))
        assertTrue(CallLink.missingTurnDetail().startsWith("нет TURN"))
        assertFalse(CallLink.infoHasIceServers(""))
        assertFalse(CallLink.infoHasIceServers("[]"))
        assertTrue(CallLink.infoHasIceServers("""[{"urls":["stun:vps:3478"]}]"""))
        assertTrue(CallLink.infoStatusLine("").contains("нет ice_servers"))
        assertTrue(
            CallLink.infoStatusLine("""[{"urls":["turn:vps:3478"],"username":"u","credential":"c"}]""")
                .contains("TURN получен"),
        )
        val ringing = CallInfo("1", "p", "Анна", true, CallPhase.RINGING_OUT, media = "ожидаем ответа")
        val warned = CallLink.applyInfo("", ringing)
        assertTrue(warned.iceReady)
        assertFalse(warned.hasTurn)
        assertEquals(CallLink.missingIceServersDetail(), warned.media)
        assertEquals(CallLink.missingIceServersDetail(), CallLink.subtitle(warned, ""))
        assertFalse(CallLink.timedOut(5_000, CallLinkState.CONNECTING))
    }

    @Test
    fun ringTimesOutAndClockStaysBounded() {
        assertFalse(CallLink.ringTimedOut(44_999, CallPhase.RINGING_OUT, CallLinkState.RINGING))
        assertTrue(CallLink.ringTimedOut(45_000, CallPhase.RINGING_OUT, CallLinkState.RINGING))
        assertFalse(CallLink.ringTimedOut(45_000, CallPhase.RINGING_IN, CallLinkState.RINGING))
        assertFalse(CallLink.ringTimedOut(45_000, CallPhase.RINGING_OUT, CallLinkState.FAILED))
        assertEquals("12 с / 25 с", CallLink.clock(12_400, CallPhase.ACTIVE, CallLinkState.CONNECTING))
        assertEquals("5 с / 45 с", CallLink.clock(5_000, CallPhase.RINGING_OUT, CallLinkState.RINGING))
        assertNull(CallLink.clock(12_000, CallPhase.ACTIVE, CallLinkState.CONNECTED))
    }

    @Test
    fun applyInfoDoesNotClobberFailedOrConnected() {
        val failed = CallInfo(
            "1", "p", "Боб", true, CallPhase.RINGING_OUT,
            media = CallLink.noAnswerDetail(),
            link = CallLinkState.FAILED,
        )
        assertEquals(CallLink.noAnswerDetail(), CallLink.applyInfo("[]", failed).media)
        val connected = CallInfo(
            "1", "p", "Боб", true, CallPhase.ACTIVE,
            media = "WebRTC · через сервер",
            link = CallLinkState.CONNECTED,
            hasTurn = true,
        )
        assertEquals("WebRTC · через сервер", CallLink.applyInfo("", connected).media)
        val incoming = CallInfo("1", "p", "Анна", false, CallPhase.RINGING_IN, media = "один тап — ответить")
        val incomingWarned = CallLink.applyInfo("", incoming)
        assertTrue(CallLink.subtitle(incomingWarned, "").contains("один тап — ответить"))
        assertTrue(CallLink.subtitle(incomingWarned, "").contains("нет TURN"))
    }

    @Test
    fun overlayCopyIsLabelsNotLectures() {
        assertEquals("Входящий вызов", CallLink.heading(CallPhase.RINGING_IN, CallLinkState.RINGING))
        assertEquals("Вызов…", CallLink.heading(CallPhase.RINGING_OUT, CallLinkState.RINGING))
        assertEquals("Разговор", CallLink.heading(CallPhase.ACTIVE, CallLinkState.CONNECTED))
        val headings = listOf(
            CallLink.heading(CallPhase.RINGING_IN, CallLinkState.RINGING),
            CallLink.heading(CallPhase.RINGING_OUT, CallLinkState.RINGING),
            CallLink.heading(CallPhase.ACTIVE, CallLinkState.CONNECTING),
            CallLink.heading(CallPhase.ACTIVE, CallLinkState.CONNECTED),
            CallLink.heading(CallPhase.ENDED, CallLinkState.FAILED),
        )
        headings.forEach { h ->
            assertTrue(h, h.length <= 24)
            assertFalse(h, h.contains("конверт"))
            assertFalse(h, h.contains("шифр", ignoreCase = true))
            assertFalse(h, h.contains("архитектур"))
        }
        val ringing = CallInfo("1", "p", "Анна", false, CallPhase.RINGING_IN, media = "один тап — ответить")
        val sub = CallLink.subtitle(ringing, """[{"urls":["turn:vps:3478"],"username":"u","credential":"c"}]""")
        assertEquals("один тап — ответить", sub)
        assertFalse(sub.contains("конверт"))
        assertFalse(CallLink.connectedDetail(false).contains("конверт"))
    }
}
