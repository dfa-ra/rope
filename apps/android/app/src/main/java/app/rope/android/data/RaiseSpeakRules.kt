package app.rope.android.data

/**
 * Settings «Говорить, поднеся к уху»: raise the phone to the ear in an open
 * chat to record a voice note; lower it to send.
 *
 * Distinct from in-thread playback pause and from call earpiece routing.
 * Not CallVideoRenderer. Kv-only. LocalStore stays v6.
 */
enum class RaiseSpeakAction {
    NONE,
    START,
    SEND,
    CANCEL,
}

object RaiseSpeakRules {
    const val TITLE = "Говорить, поднеся к уху"
    const val SECTION = "Чат"
    const val KV = "raise_speak"
    const val NEAR_CM = 5f

    fun enabledFromKv(raw: String?): Boolean = raw != "0"

    fun persist(enabled: Boolean): String = if (enabled) "1" else "0"

    fun hint(): String =
        "Поднесите телефон к уху в чате — начнётся голосовое. Опустите — отправится. Не пауза воспроизведения."

    fun isNear(distance: Float, maxRange: Float): Boolean {
        if (distance < 0f) return false
        val cap = when {
            maxRange <= 0f -> NEAR_CM
            maxRange < NEAR_CM -> maxRange
            else -> NEAR_CM
        }
        return distance < cap
    }

    fun isRaisedPose(gx: Float, gy: Float, gz: Float): Boolean {
        val mag = kotlin.math.sqrt(gx * gx + gy * gy + gz * gz)
        if (mag < 4f) return false
        return kotlin.math.abs(gy) > kotlin.math.abs(gz) + 1f
    }

    fun atEar(near: Boolean, raised: Boolean?): Boolean = near && (raised ?: true)

    fun shouldListen(
        enabled: Boolean,
        inChat: Boolean,
        liveCall: Boolean,
        foreground: Boolean,
    ): Boolean = enabled && inChat && !liveCall && foreground

    fun action(
        enabled: Boolean,
        inChat: Boolean,
        foreground: Boolean,
        recording: Boolean,
        raiseSession: Boolean,
        recordingVideoNote: Boolean,
        liveCall: Boolean,
        atEar: Boolean,
        sawAway: Boolean,
        hasMic: Boolean,
    ): RaiseSpeakAction {
        if (recording && raiseSession &&
            (!enabled || !inChat || !foreground || liveCall || recordingVideoNote)
        ) {
            return RaiseSpeakAction.CANCEL
        }
        if (!enabled || !inChat || !foreground || liveCall || recordingVideoNote) {
            return RaiseSpeakAction.NONE
        }
        if (atEar && !recording && sawAway && hasMic) return RaiseSpeakAction.START
        if (!atEar && recording && raiseSession) return RaiseSpeakAction.SEND
        return RaiseSpeakAction.NONE
    }
}
