package app.rope.android.media

import android.content.Context
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import app.rope.android.data.AttachCameraRules
import app.rope.android.data.VideoNoteRules
import java.io.File
import java.util.UUID

/**
 * Rectangular still + file video for the attach sheet.
 * Camera1 only. Not WebRTC, not кружок.
 */
@Suppress("DEPRECATION")
class AttachCamera(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var camera: Camera? = null
    private var recorder: MediaRecorder? = null
    private var holder: SurfaceHolder? = null
    private var displayRotation = 0
    private var cameraId = -1
    private var facingBack = AttachCameraRules.DEFAULT_BACK
    private var cameraOrientation = 90
    private var videoFile: File? = null
    private var videoStartedAt = 0L
    private var flashMode = AttachCameraRules.FLASH_OFF

    val recording: Boolean get() = recorder != null
    val isOpen: Boolean get() = camera != null
    val usingBack: Boolean get() = facingBack

    fun hardwareCount(): Int = Camera.getNumberOfCameras()

    fun supportedFlash(): List<String> =
        camera?.parameters?.supportedFlashModes.orEmpty()

    fun destDir(): File = File(context.cacheDir, AttachCameraRules.CACHE_DIR).apply { mkdirs() }

    fun newStillFile(): File =
        File(destDir(), AttachCameraRules.cacheFileName(UUID.randomUUID().toString(), video = false))

    fun newVideoFile(): File =
        File(destDir(), AttachCameraRules.cacheFileName(UUID.randomUUID().toString(), video = true))

    fun open(surface: SurfaceHolder, displayRotationDeg: Int, preferBack: Boolean = AttachCameraRules.DEFAULT_BACK) {
        release()
        holder = surface
        displayRotation = displayRotationDeg
        val count = Camera.getNumberOfCameras()
        val id = AttachCameraRules.pickBackCamera(count) { i ->
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            val back = info.facing == Camera.CameraInfo.CAMERA_FACING_BACK
            if (preferBack) back else !back
        }
        if (id < 0) error("нет камеры")
        openId(id, surface, displayRotationDeg)
    }

    fun flip() {
        val surface = holder ?: return
        val preferBack = !facingBack
        open(surface, displayRotation, preferBack)
    }

    fun setFlash(mode: String) {
        val cam = camera ?: return
        val supported = cam.parameters.supportedFlashModes.orEmpty()
        if (mode !in supported) return
        flashMode = mode
        try {
            val params = cam.parameters
            params.flashMode = mode
            cam.parameters = params
        } catch (_: Exception) {
        }
    }

    fun takePicture(dest: File, onDone: (Boolean) -> Unit) {
        val cam = camera
        if (cam == null || recording) {
            onDone(false)
            return
        }
        try {
            cam.takePicture(null, null) { data, c ->
                val ok = try {
                    dest.writeBytes(data)
                    true
                } catch (_: Exception) {
                    dest.delete()
                    false
                }
                try {
                    c.startPreview()
                } catch (_: Exception) {
                }
                main.post { onDone(ok) }
            }
        } catch (_: Exception) {
            dest.delete()
            onDone(false)
        }
    }

    fun startVideo(dest: File) {
        val cam = camera ?: error("нет камеры")
        if (recorder != null) return
        stopPreviewForRecord(cam)
        cam.unlock()
        val rec = MediaRecorder()
        rec.setCamera(cam)
        rec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        rec.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        val profile = videoProfile(cameraId)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setVideoFrameRate(profile.videoFrameRate.coerceIn(15, 30))
        rec.setVideoEncodingBitRate(2_000_000)
        rec.setAudioEncodingBitRate(VideoNoteRules.AUDIO_BITRATE_BPS)
        rec.setAudioSamplingRate(44_100)
        rec.setOrientationHint(VideoNoteRules.recordingHint(!facingBack, cameraOrientation))
        rec.setMaxDuration(AttachCameraRules.MAX_VIDEO_MS.toInt())
        rec.setOutputFile(dest.absolutePath)
        rec.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                main.post { /* overlay observes recording flag via stop */ }
            }
        }
        val sizes = listOf(
            profile.videoFrameWidth to profile.videoFrameHeight,
            1280 to 720,
            720 to 480,
            640 to 480,
        )
        var started = false
        var last: Exception? = null
        for ((w, h) in sizes) {
            try {
                rec.setVideoSize(w, h)
                rec.prepare()
                rec.start()
                started = true
                break
            } catch (e: Exception) {
                last = e
                try {
                    rec.reset()
                    rec.setCamera(cam)
                    rec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                    rec.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    rec.setVideoFrameRate(profile.videoFrameRate.coerceIn(15, 30))
                    rec.setVideoEncodingBitRate(2_000_000)
                    rec.setAudioEncodingBitRate(VideoNoteRules.AUDIO_BITRATE_BPS)
                    rec.setAudioSamplingRate(44_100)
                    rec.setOrientationHint(VideoNoteRules.recordingHint(!facingBack, cameraOrientation))
                    rec.setMaxDuration(AttachCameraRules.MAX_VIDEO_MS.toInt())
                    rec.setOutputFile(dest.absolutePath)
                } catch (_: Exception) {
                }
            }
        }
        if (!started) {
            try {
                cam.lock()
            } catch (_: Exception) {
            }
            try {
                cam.startPreview()
            } catch (_: Exception) {
            }
            rec.release()
            throw last ?: IllegalStateException("не удалось начать видео")
        }
        recorder = rec
        videoFile = dest
        videoStartedAt = System.currentTimeMillis()
    }

    fun stopVideo(): Pair<File, Long>? {
        val dest = videoFile ?: return null
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        val ms = (System.currentTimeMillis() - videoStartedAt).coerceAtLeast(0)
        finishRecorder()
        if (!dest.isFile || dest.length() < 32) {
            dest.delete()
            return null
        }
        if (ms < AttachCameraRules.MIN_VIDEO_MS && dest.length() < 8_000) {
            dest.delete()
            return null
        }
        return dest to ms
    }

    fun cancelVideo() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        videoFile?.delete()
        finishRecorder()
    }

    fun release() {
        cancelVideo()
        try {
            camera?.stopPreview()
        } catch (_: Exception) {
        }
        try {
            camera?.release()
        } catch (_: Exception) {
        }
        camera = null
        cameraId = -1
        holder = null
    }

    private fun openId(id: Int, surface: SurfaceHolder, displayRotationDeg: Int) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(id, info)
        cameraId = id
        facingBack = info.facing == Camera.CameraInfo.CAMERA_FACING_BACK
        cameraOrientation = info.orientation
        val cam = Camera.open(id)
        val params = cam.parameters
        val preview = AttachCameraRules.pickPreview(
            params.supportedPreviewSizes.orEmpty().map { AttachCameraRules.Size(it.width, it.height) },
        )
        if (preview != null) params.setPreviewSize(preview.width, preview.height)
        val picture = AttachCameraRules.pickPicture(
            params.supportedPictureSizes.orEmpty().map { AttachCameraRules.Size(it.width, it.height) },
        )
        if (picture != null) params.setPictureSize(picture.width, picture.height)
        val focus = params.supportedFocusModes.orEmpty()
        when {
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in focus ->
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO in focus ->
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        val flashes = params.supportedFlashModes.orEmpty()
        if (flashMode in flashes) params.flashMode = flashMode
        cam.parameters = params
        cam.setDisplayOrientation(
            VideoNoteRules.previewOrientation(!facingBack, cameraOrientation, displayRotationDeg),
        )
        cam.setPreviewDisplay(surface)
        cam.startPreview()
        camera = cam
    }

    private fun stopPreviewForRecord(cam: Camera) {
        try {
            cam.stopPreview()
        } catch (_: Exception) {
        }
    }

    private fun finishRecorder() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        videoFile = null
        videoStartedAt = 0
        try {
            camera?.reconnect()
        } catch (_: Exception) {
        }
        try {
            camera?.lock()
        } catch (_: Exception) {
        }
        try {
            camera?.startPreview()
        } catch (_: Exception) {
        }
    }

    private fun videoProfile(id: Int): CamcorderProfile = try {
        val quality = when {
            CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_720P) -> CamcorderProfile.QUALITY_720P
            CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_480P) -> CamcorderProfile.QUALITY_480P
            else -> CamcorderProfile.QUALITY_LOW
        }
        CamcorderProfile.get(id, quality)
    } catch (_: Exception) {
        CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
    }
}
