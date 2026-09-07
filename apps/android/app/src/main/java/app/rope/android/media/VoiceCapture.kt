package app.rope.android.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

data class VoiceTake(
    val file: File,
    val durationMs: Long,
)

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val recording: Boolean get() = recorder != null

    fun start(): File {
        cancel()
        val dest = File(context.cacheDir, "voice-${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(64_000)
        rec.setAudioSamplingRate(44_100)
        rec.setMaxDuration(MAX_MS.toInt())
        rec.setOutputFile(dest.absolutePath)
        rec.prepare()
        rec.start()
        recorder = rec
        file = dest
        startedAt = System.currentTimeMillis()
        return dest
    }

    fun stop(): VoiceTake? {
        val dest = file ?: return null
        return try {
            recorder?.stop()
            val ms = (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
            VoiceTake(dest, ms)
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
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        file = null
        startedAt = 0
    }

    companion object {
        const val MAX_MS = 10 * 60 * 1000L
        const val MIN_MS = 400L
    }
}

class VoicePlayer {
    private var player: MediaPlayer? = null
    var playingId: String? = null
        private set
    var loadedId: String? = null
        private set

    /** Playing now, or paused with a kept position. */
    val activeId: String? get() = playingId ?: loadedId.takeIf { player != null }

    fun positionMs(): Long = try {
        player?.currentPosition?.toLong()?.coerceAtLeast(0L) ?: 0L
    } catch (_: Exception) {
        0L
    }

    fun durationMs(): Long = try {
        val d = player?.duration ?: 0
        if (d > 0) d.toLong() else 0L
    } catch (_: Exception) {
        0L
    }

    private fun isPlayingNow(): Boolean = try {
        player?.isPlaying == true
    } catch (_: Exception) {
        false
    }

    fun toggle(id: String, path: String) {
        if (loadedId == id && player != null) {
            if (isPlayingNow()) {
                try {
                    player?.pause()
                } catch (_: Exception) {
                }
                playingId = null
            } else {
                try {
                    player?.start()
                    playingId = id
                } catch (_: Exception) {
                    stop()
                }
            }
            return
        }
        stop()
        val p = MediaPlayer()
        p.setDataSource(path)
        p.setOnCompletionListener { stop() }
        p.prepare()
        p.start()
        player = p
        loadedId = id
        playingId = id
    }

    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        playingId = null
        loadedId = null
    }
}
