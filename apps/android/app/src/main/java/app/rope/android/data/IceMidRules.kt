package app.rope.android.data

/**
 * ICE sdpMid on the local candidate and after wire parse.
 * Fail closed on CR/LF/NUL before trim so a newline prefix cannot become
 * a live mid after trim. Spaces still trim. Empty becomes "0".
 * Not CallSignal.parseEvent and not VideoCallRules ICE state names.
 */
object IceMidRules {
    fun localMid(raw: String?): String? {
        if (raw.isNullOrEmpty()) return "0"
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        return raw.trim().ifEmpty { "0" }
    }
}
