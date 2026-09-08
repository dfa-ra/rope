package app.rope.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/** Routes call audio to the earpiece in MODE_IN_COMMUNICATION. Speaker is opt-in. */
object CallAudio {
    @Volatile
    private var focus: AudioFocusRequest? = null

    @Volatile
    private var speakerOn = false

    fun apply(context: Context, on: Boolean) {
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
        if (on) {
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            requestFocus(am)
            if (speakerOn) routeSpeaker(am) else routeEarpiece(am)
        } else {
            speakerOn = false
            abandonFocus(am)
            am.mode = AudioManager.MODE_NORMAL
            am.isSpeakerphoneOn = false
            if (Build.VERSION.SDK_INT >= 31) {
                runCatching { am.clearCommunicationDevice() }
            }
        }
    }

    /** Loudspeaker toggle. Default remains earpiece until this is called with true. */
    fun setSpeaker(context: Context, on: Boolean) {
        speakerOn = on
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
        if (am.mode != AudioManager.MODE_IN_COMMUNICATION) return
        if (on) routeSpeaker(am) else routeEarpiece(am)
    }

    /** Re-assert call routing after a remote audio track arrives. */
    fun confirm(context: Context) = apply(context, true)

    private fun requestFocus(am: AudioManager) {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener { }
            .build()
        focus = req
        am.requestAudioFocus(req)
    }

    private fun abandonFocus(am: AudioManager) {
        val req = focus
        focus = null
        if (req != null) am.abandonAudioFocusRequest(req)
    }

    private fun routeEarpiece(am: AudioManager) {
        am.isSpeakerphoneOn = false
        if (Build.VERSION.SDK_INT >= 31) {
            val ear = am.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            if (ear != null) {
                am.setCommunicationDevice(ear)
                return
            }
        }
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = false
    }

    private fun routeSpeaker(am: AudioManager) {
        am.isSpeakerphoneOn = true
        if (Build.VERSION.SDK_INT >= 31) {
            val spk = am.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            if (spk != null) {
                am.setCommunicationDevice(spk)
                return
            }
        }
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = true
    }
}
