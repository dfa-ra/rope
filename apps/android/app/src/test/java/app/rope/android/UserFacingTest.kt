package app.rope.android

import app.rope.android.data.UserFacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFacingTest {
    @Test
    fun loginCopyPassesThrough() {
        assertEquals(UserFacing.LOGIN, UserFacing.of(UserFacing.LOGIN))
        assertEquals(UserFacing.FILE_TOO_BIG, UserFacing.of("Файл больше 25 МБ"))
        assertEquals("Название группы", UserFacing.of("Название группы"))
    }

    @Test
    fun webrtcAndIceBecomeShortPathCopy() {
        assertEquals(UserFacing.NO_PATH, UserFacing.of("WebRTC: IceConnectionState.FAILED"))
        assertEquals(UserFacing.NO_PATH, UserFacing.of("ICE failed · нет пути · через сервер · TURN не соединил"))
        assertEquals(UserFacing.NO_PATH, UserFacing.of("нет пути за 25 с · TURN не выделил / ICE не соединил · проверьте 3478 / 443"))
    }

    @Test
    fun tracesCollapseToGeneric() {
        val trace = "java.lang.IllegalStateException: PeerConnectionFactory\n\tat org.webrtc.PeerConnection.<init>(PeerConnection.java:12)"
        assertEquals(UserFacing.GENERIC, UserFacing.of(trace))
        assertEquals(UserFacing.GENERIC, UserFacing.of("kotlin.IllegalStateException: boom"))
        assertEquals(UserFacing.GENERIC, UserFacing.of(""))
        assertEquals(UserFacing.GENERIC, UserFacing.of(null as String?))
    }

    @Test
    fun networkMapsToNoNetwork() {
        assertEquals(UserFacing.NO_NETWORK, UserFacing.of("UnknownHostException: vps.example"))
        assertEquals(UserFacing.NO_NETWORK, UserFacing.of("failed to connect"))
        assertEquals(UserFacing.NO_NETWORK, UserFacing.of(IllegalStateException("not connected")))
    }

    @Test
    fun neverLeaksExceptionClassNamesForUnknownLongText() {
        val long = "something exploded in the native layer with a lot of internal words ".repeat(3)
        assertEquals(UserFacing.GENERIC, UserFacing.of(long))
        assertFalse(UserFacing.of(Exception("org.webrtc.IceCandidate")).contains("org.webrtc"))
        assertTrue(UserFacing.of("Скопировано") == "Скопировано")
    }
}
