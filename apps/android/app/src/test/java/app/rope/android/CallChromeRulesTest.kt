package app.rope.android

import app.rope.android.data.CallControlKind
import app.rope.android.data.CallChromeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallChromeRulesTest {
    @Test
    fun ringingButtonsAreIconOnlyWithA11y() {
        val controls = CallChromeRules.ringingControls()
        assertEquals(
            listOf(CallControlKind.REJECT, CallControlKind.ACCEPT),
            controls.map { it.kind },
        )
        for (spec in controls) {
            assertTrue(spec.a11y.isNotBlank())
            assertTrue(CallChromeRules.iconOnly(spec))
            assertFalse(CallChromeRules.showVisibleCaption(spec.kind))
        }
        assertEquals("Отклонить", controls[0].a11y)
        assertEquals("Ответить", controls[1].a11y)
    }

    @Test
    fun audioInCallHasMuteSpeakerHangupNotCamera() {
        val controls = CallChromeRules.inCallControls(video = false, micMuted = false, speakerOn = false)
        assertEquals(
            listOf(CallControlKind.MUTE, CallControlKind.HANGUP, CallControlKind.SPEAKER),
            controls.map { it.kind },
        )
        assertFalse(controls.any { it.kind == CallControlKind.CAMERA || it.kind == CallControlKind.FLIP })
        for (spec in controls) {
            assertTrue(spec.a11y.isNotBlank())
            assertTrue(CallChromeRules.iconOnly(spec))
            assertEquals(null, spec.visibleCaption)
        }
        assertEquals("Микрофон", CallChromeRules.muteA11y(false))
        assertEquals("Микрофон выкл", CallChromeRules.muteA11y(true))
        assertEquals("Громкая связь", CallChromeRules.speakerA11y(false))
        assertEquals("Громкая связь вкл", CallChromeRules.speakerA11y(true))
    }

    @Test
    fun videoInCallHasCameraFlipNotSpeaker() {
        val controls = CallChromeRules.inCallControls(video = true, micMuted = true, camMuted = true)
        assertEquals(
            listOf(CallControlKind.MUTE, CallControlKind.HANGUP, CallControlKind.CAMERA, CallControlKind.FLIP),
            controls.map { it.kind },
        )
        assertFalse(controls.any { it.kind == CallControlKind.SPEAKER })
        for (spec in controls) {
            assertTrue(CallChromeRules.iconOnly(spec))
            assertFalse(CallChromeRules.showVisibleCaption(spec.kind))
        }
        assertEquals("Микрофон выкл", controls[0].a11y)
        assertEquals("Завершить", controls[1].a11y)
        assertEquals("Камера выкл", controls[2].a11y)
        assertEquals("Сменить камеру", controls[3].a11y)
        assertEquals("Камера", CallChromeRules.cameraA11y(false))
    }

    @Test
    fun noRoundControlShowsWrappingCaption() {
        val all = CallChromeRules.ringingControls() +
            CallChromeRules.inCallControls(video = false) +
            CallChromeRules.inCallControls(video = true)
        assertTrue(all.isNotEmpty())
        assertTrue(all.all { it.visibleCaption == null })
        assertTrue(all.all { it.a11y.isNotBlank() })
        for (kind in CallControlKind.entries) {
            assertFalse(CallChromeRules.showVisibleCaption(kind))
        }
    }
}
