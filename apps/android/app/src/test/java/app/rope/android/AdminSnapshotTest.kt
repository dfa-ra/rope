package app.rope.android

import app.rope.android.data.AdminSnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdminSnapshotTest {
    @Test
    fun listenCrlfFallsBackBeforeTrim() {
        val snap = AdminSnapshot.from(
            JSONObject()
                .put("listen", "0.0.0.0:8443\nHost: evil")
                .put("version", "0.3.50"),
        )
        val listen = snap.cards.first { it.label == "Listen" }
        assertEquals("—", listen.value)
        assertFalse(listen.value.contains('\n'))
        val version = snap.cards.first { it.label == "Версия ядра" }
        assertEquals("0.3.50", version.value)
    }

    @Test
    fun turnErrorCrlfIsDroppedFromHint() {
        val hint = AdminSnapshot.turnHint(
            JSONObject()
                .put("ice_enabled", true)
                .put("turn_running", false)
                .put("turn_error", "порт 443\nзанят"),
        )
        assertFalse(hint.contains('\n'))
        assertFalse(hint.contains("занят"))
        assertEquals("stun/turn 3478", hint)
    }

    @Test
    fun cleanListenStillShows() {
        val snap = AdminSnapshot.from(JSONObject().put("listen", "0.0.0.0:8443"))
        assertEquals("0.0.0.0:8443", snap.cards.first { it.label == "Listen" }.value)
    }
}
