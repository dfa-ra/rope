package app.rope.android

import app.rope.android.data.AlbumRules
import app.rope.android.data.AttachCameraRules
import app.rope.android.data.VideoCallRules
import app.rope.android.data.VideoNoteRules
import app.rope.android.data.VideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AttachCameraRulesTest {
    @Test
    fun maxHoldAndBackDefault() {
        assertEquals(240_000L, AttachCameraRules.MAX_VIDEO_MS)
        assertEquals(350L, AttachCameraRules.HOLD_VIDEO_MS)
        assertTrue(AttachCameraRules.DEFAULT_BACK)
        assertTrue(AttachCameraRules.isHoldVideo(350))
        assertFalse(AttachCameraRules.isHoldVideo(349))
        assertEquals(1, AttachCameraRules.pickBackCamera(2) { it == 1 })
        assertEquals(0, AttachCameraRules.pickBackCamera(2) { false })
        assertEquals(-1, AttachCameraRules.pickBackCamera(0) { true })
        assertEquals("attach-abc.jpg", AttachCameraRules.cacheFileName("abc", video = false))
        assertEquals("attach-abc.mp4", AttachCameraRules.cacheFileName("abc", video = true))
        assertEquals("app.rope.android.files", AttachCameraRules.fileProviderAuthority("app.rope.android"))
    }

    @Test
    fun previewPrefersSixteenNineNotSquare() {
        val square = AttachCameraRules.Size(480, 480)
        val wide = AttachCameraRules.Size(1280, 720)
        assertEquals(wide, AttachCameraRules.pickPreview(listOf(square, wide)))
        assertFalse(AttachCameraRules.isPreferredAspect(480, 480))
    }

    @Test
    fun mixedUriCapTen() {
        val gallery = (1..8).map { "content://media/gallery/$it" }
        val cam = "content://app.rope.android.files/attach-cam/attach-1.jpg"
        val extra = listOf(cam, "content://media/gallery/9", "content://media/gallery/10", "content://media/gallery/11")
        val merged = AttachCameraRules.mergeStaged(gallery, extra)
        assertEquals(AlbumRules.MAX_PHOTOS, merged.size)
        assertEquals(10, merged.size)
        assertTrue(merged.contains(cam))
        assertFalse(merged.contains("content://media/gallery/11"))
        assertFalse(AttachCameraRules.canCaptureMore(10))
        assertTrue(AttachCameraRules.canCaptureMore(9))
    }

    @Test
    fun videoNoteStillNotAlbumEligible() {
        assertTrue(AttachCameraRules.albumEligible(AttachCameraRules.KIND_IMAGE))
        assertTrue(AttachCameraRules.albumEligible(AttachCameraRules.KIND_VIDEO))
        assertTrue(VideoRules.albumEligible("image"))
        assertTrue(VideoRules.albumEligible("video"))
        assertFalse(VideoRules.albumEligible(VideoNoteRules.KIND))
        assertFalse(VideoNoteRules.albumEligible(VideoNoteRules.KIND))
        assertFalse(AttachCameraRules.albumEligible(VideoNoteRules.KIND))
    }

    @Test
    fun denyCopyEqualsCameraDeniedNotice() {
        assertEquals(VideoCallRules.cameraDeniedNotice(), AttachCameraRules.cameraDeniedNotice())
        assertEquals("Нет доступа к камере", AttachCameraRules.cameraDeniedNotice())
        assertEquals(VideoCallRules.micDeniedNotice(), AttachCameraRules.micDeniedNotice())
        assertEquals("Камера недоступна", AttachCameraRules.UNAVAILABLE_NOTICE)
        assertNotEquals(VideoCallRules.cameraFailedNotice(), AttachCameraRules.UNAVAILABLE_NOTICE)
        assertFalse(AttachCameraRules.UNAVAILABLE_NOTICE.contains("только звук"))
    }

    @Test
    fun fileProviderPathAttachCam() {
        assertEquals("attach-cam/", AttachCameraRules.FILE_PROVIDER_PATH)
        assertEquals("attach-cam", AttachCameraRules.CACHE_DIR)
        val xml = locateFilePathsXml().readText()
        assertTrue(xml.contains("path=\"attach-cam/\""))
        assertTrue(xml.contains("name=\"attach-cam\""))
        assertTrue(AttachCameraRules.isAttachCamPath("/attach-cam/attach-x.jpg"))
        assertTrue(AttachCameraRules.isAttachCamPath("attach-cam/attach-x.mp4"))
        assertFalse(AttachCameraRules.isAttachCamPath("/updates/app.apk"))
        assertFalse(AttachCameraRules.isAttachCamPath("/cache/note-1.mp4"))
    }

    @Test
    fun startVideoNoteNoOpsWhenAttachBusy() {
        assertTrue(AttachCameraRules.canStartVideoNote(false, false, false, false))
        assertFalse(AttachCameraRules.canStartVideoNote(false, false, false, true))
        assertFalse(AttachCameraRules.canStartVideoNote(true, false, false, false))
        assertFalse(AttachCameraRules.canOpen(false, false, false, true))
        assertFalse(AttachCameraRules.canStartVoice(false, false, true))
        assertFalse(AttachCameraRules.canStartCall(true))
        assertTrue(AttachCameraRules.canStartCall(false))
        assertTrue(AttachCameraRules.showTile(false, false, false))
        assertFalse(AttachCameraRules.showTile(true, false, false))
        assertFalse(AttachCameraRules.showTile(false, true, false))
        assertFalse(AttachCameraRules.showTile(false, false, true))
    }

    @Test
    fun attachCameraBackCancelsRecordingLayer() {
        val chat = UiState(screen = Screen.Chat, backStack = listOf(Screen.Chats, Screen.Chat))
        assertEquals(BackLayer.CancelRecording, BackStack.decide(chat.copy(attachCameraOpen = true)))
        assertEquals(BackLayer.Pop, BackStack.decide(chat))
    }

    @Test
    fun flashCyclesOffOnAuto() {
        val supported = listOf(AttachCameraRules.FLASH_OFF, AttachCameraRules.FLASH_ON, AttachCameraRules.FLASH_AUTO)
        assertEquals(AttachCameraRules.FLASH_ON, AttachCameraRules.cyclePhotoFlash(AttachCameraRules.FLASH_OFF, supported))
        assertEquals(AttachCameraRules.FLASH_AUTO, AttachCameraRules.cyclePhotoFlash(AttachCameraRules.FLASH_ON, supported))
        assertEquals(AttachCameraRules.FLASH_OFF, AttachCameraRules.cyclePhotoFlash(AttachCameraRules.FLASH_AUTO, supported))
        assertEquals(AttachCameraRules.FLASH_TORCH, AttachCameraRules.videoFlash(supported + AttachCameraRules.FLASH_TORCH))
    }

    private fun locateFilePathsXml(): File {
        val roots = listOf(
            File("src/main/res/xml/file_paths.xml"),
            File("app/src/main/res/xml/file_paths.xml"),
            File("apps/android/app/src/main/res/xml/file_paths.xml"),
        )
        return roots.firstOrNull { it.isFile }
            ?: error("file_paths.xml missing from ${File(".").canonicalPath}")
    }
}
