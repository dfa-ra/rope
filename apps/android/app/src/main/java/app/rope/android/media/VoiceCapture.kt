package app.rope.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import app.rope.android.data.VoicePlayback
import app.rope.android.data.VoiceSpeakerRules
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class VoiceTake(
    val file: File,
    val durationMs: Long,
    val waveform: List<Int> = emptyList(),
)

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private val amplitudes = mutableListOf<Int>()

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
        amplitudes.clear()
        return dest
    }

    fun amplitude(): Int {
        val v = try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
        if (v > 0) amplitudes += v
        return v
    }

    fun stop(): VoiceTake? {
        val dest = file ?: return null
        val samples = amplitudes.toList()
        return try {
            recorder?.stop()
            val ms = (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
            val bars = VoicePlayback.barsFromAmplitudes(samples)
            VoiceTake(dest, ms, VoicePlayback.encodeWaveform(bars).takeIf { bars.isNotEmpty() }.orEmpty())
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
        amplitudes.clear()
    }

    companion object {
        const val MAX_MS = 10 * 60 * 1000L
        const val MIN_MS = 400L
    }
}

class VoicePlayer {
    private var player: MediaPlayer? = null
    private var currentPath: String? = null
    private var speakerOn: Boolean = VoiceSpeakerRules.DEFAULT_SPEAKER
    var playingId: String? = null
        private set
    var loadedId: String? = null
        private set
    var speed: Float = VoicePlayback.SPEED_1X
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
                    applySpeed()
                    player?.start()
                    playingId = id
                } catch (_: Exception) {
                    stop()
                }
            }
            return
        }
        prepare(id, path, start = true)
    }

    fun seek(id: String, path: String, positionMs: Long) {
        if (loadedId != id || player == null) {
            prepare(id, path, start = true)
        }
        val p = player ?: return
        val dur = durationMs()
        val target = positionMs.coerceIn(0L, if (dur > 0L) dur else positionMs.coerceAtLeast(0L))
        try {
            p.seekTo(target.toInt())
        } catch (_: Exception) {
        }
    }

    fun setSpeed(next: Float) {
        speed = VoicePlayback.clampSpeed(next)
        applySpeed()
    }

    fun cycleSpeed(): Float {
        setSpeed(VoicePlayback.nextSpeed(speed))
        return speed
    }

    fun setSpeakerOn(on: Boolean) {
        speakerOn = on
        val id = loadedId ?: return
        val path = currentPath ?: return
        val pos = positionMs()
        val resume = isPlayingNow()
        prepare(id, path, start = resume, seekMs = pos)
    }

    private fun applyRoute(p: MediaPlayer) {
        val usage = if (speakerOn) {
            AudioAttributes.USAGE_MEDIA
        } else {
            AudioAttributes.USAGE_VOICE_COMMUNICATION
        }
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    private fun prepare(id: String, path: String, start: Boolean, seekMs: Long = 0L) {
        stop()
        val p = MediaPlayer()
        applyRoute(p)
        p.setDataSource(path)
        p.setOnCompletionListener { stop() }
        p.prepare()
        player = p
        loadedId = id
        currentPath = path
        applySpeed()
        if (seekMs > 0L) {
            try {
                p.seekTo(seekMs.toInt())
            } catch (_: Exception) {
            }
        }
        if (start) {
            p.start()
            playingId = id
        }
    }

    private fun applySpeed() {
        val p = player ?: return
        try {
            val params = try {
                p.playbackParams
            } catch (_: Exception) {
                PlaybackParams()
            }
            p.playbackParams = params.setSpeed(speed)
        } catch (_: Exception) {
        }
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
        currentPath = null
    }
}

/** Decode AAC/M4A to RMS bars. Failures fall back to a seeded waveform. */
object VoiceWaveform {
    fun extract(path: String, bars: Int = VoicePlayback.BARS): List<Int> {
        val file = File(path)
        if (!file.isFile || file.length() < 16) return emptyList()
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(path)
            val track = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")
            } ?: return emptyList()
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            val pcm = ArrayList<Short>(16_384)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone && pcm.size < 2_000_000) {
                if (!inputDone) {
                    val inIx = codec.dequeueInputBuffer(8_000)
                    if (inIx >= 0) {
                        val buf = codec.getInputBuffer(inIx) ?: break
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIx = codec.dequeueOutputBuffer(info, 8_000)
                if (outIx >= 0) {
                    val out = codec.getOutputBuffer(outIx)
                    if (out != null && info.size > 0) {
                        appendPcm(out, info, pcm)
                    }
                    codec.releaseOutputBuffer(outIx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
            codec.stop()
            codec.release()
            if (pcm.isEmpty()) return emptyList()
            val samples = ShortArray(pcm.size)
            pcm.forEachIndexed { i, s -> samples[i] = s }
            VoicePlayback.encodeWaveform(rmsBars(samples, bars), bars)
        } catch (_: Exception) {
            emptyList()
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun appendPcm(out: ByteBuffer, info: MediaCodec.BufferInfo, pcm: MutableList<Short>) {
        out.position(info.offset)
        out.limit(info.offset + info.size)
        val slice = out.slice().order(ByteOrder.LITTLE_ENDIAN)
        while (slice.remaining() >= 2) {
            pcm += slice.short
        }
    }

    private fun rmsBars(samples: ShortArray, bars: Int): List<Float> {
        if (samples.isEmpty() || bars <= 0) return emptyList()
        val window = (samples.size / bars).coerceAtLeast(1)
        return List(bars) { i ->
            val start = i * window
            val end = if (i == bars - 1) samples.size else (start + window).coerceAtMost(samples.size)
            if (start >= end) 0.18f else {
                var acc = 0.0
                var n = 0
                var j = start
                while (j < end) {
                    val v = samples[j].toInt()
                    acc += v * v
                    n++
                    j++
                }
                val rms = kotlin.math.sqrt(acc / n.coerceAtLeast(1)).toFloat() / 32768f
                rms.coerceIn(0f, 1f)
            }
        }
    }
}
