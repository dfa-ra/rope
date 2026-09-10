package app.rope.android.data

/**
 * Raise-to-speak: live audio call + proximity near → earpiece.
 * Video calls stay on the current speaker setting (you are looking at the screen).
 * Does not play chat sounds and does not land in-thread video. LocalStore stays v6.
 */
object RaiseSpeakRules {
    fun listen(call: CallInfo?): Boolean {
        if (call == null || call.video) return false
        return call.phase == CallPhase.ACTIVE
    }

    fun isNear(distance: Float, maxRange: Float): Boolean {
        val far = if (maxRange > 0f) maxRange else 5f
        return distance < far
    }

    /** User speaker preference, overridden to earpiece while the phone is at the ear. */
    fun speakerOn(userSpeakerOn: Boolean, proximityNear: Boolean, listening: Boolean): Boolean {
        if (listening && proximityNear) return false
        return userSpeakerOn
    }
}
