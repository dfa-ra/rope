package app.rope.android.data

/**
 * Pin without notifying the peer. Local pin bar still updates.
 * Unpin still syncs CLEAR. Not envelope crypto.
 */
object PinSilentRules {
    const val LABEL = "Закрепить без звука"

    fun notifyOthers(pinning: Boolean, silent: Boolean): Boolean {
        if (!pinning) return true
        return !silent
    }
}
