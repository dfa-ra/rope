package app.rope.android

import app.rope.android.data.CallSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallSignalEventTest {
    @Test
    fun parseEventRejectsControlBeforeTrim() {
        assertEquals(CallSignal.HANGUP, CallSignal.parseEvent("hangup"))
        assertEquals(CallSignal.HANGUP, CallSignal.parseEvent(" bye "))
        assertEquals(CallSignal.RING, CallSignal.parseEvent("RING"))
        assertEquals(CallSignal.AUDIO, CallSignal.parseEvent("AUDIO"))
        assertNull(CallSignal.parseEvent("hangup\n"))
        assertNull(CallSignal.parseEvent("\nring"))
        assertNull(CallSignal.parseEvent("accept\r"))
        assertNull(CallSignal.parseEvent("ring\u0000"))
        assertNull(CallSignal.parseEvent(""))
        assertNull(CallSignal.parseEvent(null))
        assertNull(CallSignal.parseEvent("   "))
    }
}
