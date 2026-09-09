package app.rope.android.media

import android.content.Context
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.view.SurfaceHolder
import app.rope.android.data.VideoNoteRules
import java.io.File

data class VideoNoteTake(
    val file: File,
    val durationMs: Long,
)

/**
 * Hold-to-record round video note. Uses Camera1 + MediaRecorder so the
 * in-call WebRTC camera path stays untouched.
 */
@Suppress("DEPRECATION")
class VideoNoteRecorder(private val context: Context) {
    private var camera: Camera? = null
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private var cameraId = -1
    private var facingFront = true
    private var cameraOrientation = 270

    val recording: Boolean get() = recorder != null

    fun start(holder: SurfaceHolder, displayRotationDeg: Int): File {
        cancel()
        val dest = File(context.cacheDir, "note-${System.currentTimeMillis()}.mp4")
        val id = VideoNoteRules.pickFrontCamera(Camera.getNumberOfCameras()) { i ->
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
        }
        if (id < 0) error("нет камеры")
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(id, info)
        cameraId = id
        facingFront = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
        cameraOrientation = info.orientation
        val cam = Camera.open(id)
        val params = cam.parameters
        pickSquarePreview(params)?.let { params.setPreviewSize(it.width, it.height) }
        pickSquarePicture(params)?.let { params.setPictureSize(it.width, it.height) }
        if (params.supportedFocusModes?.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) == true) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        cam.parameters = params
        cam.setDisplayOrientation(
            VideoNoteRules.previewOrientation(facingFront, cameraOrientation, displayRotationDeg),
        )
        cam.setPreviewDisplay(holder)
        cam.startPreview()
        cam.unlock()
        val rec = MediaRecorder()
        rec.setCamera(cam)
        rec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        rec.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        val profile = try {
            val quality = if (CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_480P)) {
                CamcorderProfile.QUALITY_480P
            } else {
                CamcorderProfile.QUALITY_LOW
            }
            CamcorderProfile.get(id, quality)
        } catch (_: Exception) {
            CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
        }
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setVideoFrameRate(profile.videoFrameRate.coerceIn(15, VideoNoteRules.FPS))
        rec.setVideoEncodingBitRate(VideoNoteRules.VIDEO_BITRATE_BPS)
        rec.setAudioEncodingBitRate(VideoNoteRules.AUDIO_BITRATE_BPS)
        rec.setAudioSamplingRate(44_100)
        rec.setOrientationHint(VideoNoteRules.recordingHint(facingFront, cameraOrientation))
        rec.setMaxDuration(VideoNoteRules.MAX_MS.toInt())
        rec.setOutputFile(dest.absolutePath)
        val sizes = listOf(
            VideoNoteRules.SIZE_PX to VideoNoteRules.SIZE_PX,
            profile.videoFrameWidth to profile.videoFrameHeight,
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
                    rec.setVideoFrameRate(profile.videoFrameRate.coerceIn(15, VideoNoteRules.FPS))
                    rec.setVideoEncodingBitRate(VideoNoteRules.VIDEO_BITRATE_BPS)
                    rec.setAudioEncodingBitRate(VideoNoteRules.AUDIO_BITRATE_BPS)
                    rec.setAudioSamplingRate(44_100)
                    rec.setOrientationHint(VideoNoteRules.recordingHint(facingFront, cameraOrientation))
                    rec.setMaxDuration(VideoNoteRules.MAX_MS.toInt())
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
            cam.release()
            throw last ?: IllegalStateException("не удалось начать кружок")
        }
        camera = cam
        recorder = rec
        file = dest
        startedAt = System.currentTimeMillis()
        return dest
    }

    fun stop(): VideoNoteTake? {
        val dest = file ?: return null
        return try {
            recorder?.stop()
            val ms = (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
            VideoNoteTake(dest, ms)
        } catch (_: Exception) {
            dest.delete()
            null
        } finally {
            release()
        }
    }

    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        file?.delete()
        release()
    }

    private fun release() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        try {
            camera?.reconnect()
        } catch (_: Exception) {
        }
        try {
            camera?.stopPreview()
        } catch (_: Exception) {
        }
        try {
            camera?.release()
        } catch (_: Exception) {
        }
        camera = null
        file = null
        startedAt = 0
        cameraId = -1
    }

    private fun pickSquarePreview(params: Camera.Parameters): Camera.Size? =
        params.supportedPreviewSizes
            ?.filter { it.width >= 240 && it.height >= 240 }
            ?.minByOrNull { kotlin.math.abs(it.width - VideoNoteRules.SIZE_PX) + kotlin.math.abs(it.height - VideoNoteRules.SIZE_PX) }

    private fun pickSquarePicture(params: Camera.Parameters): Camera.Size? =
        params.supportedPictureSizes
            ?.filter { it.width >= 240 && it.height >= 240 }
            ?.minByOrNull { kotlin.math.abs(it.width - VideoNoteRules.SIZE_PX) + kotlin.math.abs(it.height - VideoNoteRules.SIZE_PX) }
}
