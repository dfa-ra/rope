package app.rope.android.media

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 16 kHz mono PCM 16-bit frames over the live WSS call channel.
 * Crypto stays in UniFFI ([encryptTyped] / [decryptTyped]); this class only captures and plays.
 */
class WssAudioSession(
    context: Context,
    private val onFrame: (ByteArray) -> Unit,
) {
    private val app = context.applicationContext
    private val closed = AtomicBoolean(false)
    private val muted = AtomicBoolean(false)
    private val playLock = Any()
    private val recLock = Any()
    private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    private var recordThread: Thread? = null

    init {
        CallAudio.apply(app, true)
        openTrack()
        startRecord()
    }

    fun setMuted(on: Boolean) {
        muted.set(on)
    }

    fun play(pcm: ByteArray) {
        if (closed.get() || pcm.isEmpty()) return
        synchronized(playLock) {
            val t = track ?: return
            if (t.playState != AudioTrack.PLAYSTATE_PLAYING) {
                runCatching { t.play() }
            }
            t.write(pcm, 0, pcm.size)
        }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        recordThread?.interrupt()
        synchronized(recLock) {
            runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
            recorder = null
        }
        synchronized(playLock) {
            runCatching { track?.stop() }
            runCatching { track?.release() }
            track = null
        }
        recordThread = null
    }

    @SuppressLint("MissingPermission")
    private fun startRecord() {
        val min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(min, BYTES_PER_FRAME * 2)
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
            )
        } catch (_: Exception) {
            return
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return
        }
        synchronized(recLock) { recorder = rec }
        rec.startRecording()
        recordThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val buf = ByteArray(BYTES_PER_FRAME)
            while (!closed.get() && !Thread.currentThread().isInterrupted) {
                val n = try {
                    rec.read(buf, 0, buf.size)
                } catch (_: Exception) {
                    break
                }
                if (n != buf.size) continue
                if (muted.get()) continue
                onFrame(buf.copyOf(n))
            }
        }, "rope-wss-rec").also { it.start() }
    }

    private fun openTrack() {
        val min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(min, BYTES_PER_FRAME * 4)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val created = try {
            AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }
        synchronized(playLock) {
            track = created
            runCatching { created.play() }
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_MS = 60
        const val SAMPLES_PER_FRAME = SAMPLE_RATE * FRAME_MS / 1000
        const val BYTES_PER_FRAME = SAMPLES_PER_FRAME * 2
    }
}
