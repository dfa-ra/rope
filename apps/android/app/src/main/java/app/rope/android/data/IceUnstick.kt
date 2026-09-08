package app.rope.android.data

/**
 * Relay-only + failed TURN allocate leaves ICE in CHECKING and the UI on
 * «ищем путь». These thresholds are the only clocks that may keep that copy.
 */
object IceUnstick {
    /** No relay candidate / no ICE progress → drop RELAY-only, try host/srflx. */
    const val NO_PROGRESS_MS = 4_000L

    /** Callee never sent answer (or offer never arrived) — signaling, not ICE. */
    const val ANSWER_FAIL_MS = 12_000L

    /** Loud fail. Must match [CallLink.CONNECT_TIMEOUT_MS]. */
    const val CONNECT_FAIL_MS = CallLink.CONNECT_TIMEOUT_MS

    data class Snapshot(
        val elapsedMs: Long,
        val ice: String = "",
        val hasRelayCandidate: Boolean = false,
        val preferRelay: Boolean = false,
        val alreadyFellBack: Boolean = false,
        val iceRestartUsed: Boolean = false,
        val remoteDescriptionReady: Boolean = false,
        val connected: Boolean = false,
        val failed: Boolean = false,
        val isOfferer: Boolean = false,
    )

    data class Decision(
        val fallbackDirect: Boolean = false,
        val restartIce: Boolean = false,
        val failSignal: Boolean = false,
        val failIce: Boolean = false,
        /** CallMachine maps [failIce] to WSS E2EE audio instead of a terminal fail. */
        val fallbackWss: Boolean = false,
    ) {
        val terminal: Boolean get() = failSignal || failIce
        val acted: Boolean get() = fallbackDirect || restartIce || terminal || fallbackWss
    }

    fun searchingPath(ice: String): Boolean {
        val name = ice.trim().uppercase()
        return name.isEmpty() ||
            name == "CHECKING" ||
            name == "CONNECTING" ||
            name == "NEW"
    }

    fun shouldFallbackRelay(
        elapsedMs: Long,
        preferRelay: Boolean,
        hasRelayCandidate: Boolean,
        alreadyFellBack: Boolean,
    ): Boolean = preferRelay &&
        !hasRelayCandidate &&
        !alreadyFellBack &&
        elapsedMs >= NO_PROGRESS_MS

    fun decide(s: Snapshot): Decision {
        if (s.connected || s.failed) return Decision()
        if (!s.remoteDescriptionReady && s.elapsedMs >= ANSWER_FAIL_MS) {
            return Decision(failSignal = true)
        }
        if (s.elapsedMs >= CONNECT_FAIL_MS) {
            return Decision(failIce = true)
        }
        if (s.elapsedMs < NO_PROGRESS_MS) return Decision()
        val fallback = shouldFallbackRelay(
            elapsedMs = s.elapsedMs,
            preferRelay = s.preferRelay,
            hasRelayCandidate = s.hasRelayCandidate,
            alreadyFellBack = s.alreadyFellBack,
        )
        val restart = s.isOfferer && !s.iceRestartUsed
        return Decision(fallbackDirect = fallback, restartIce = restart)
    }
}
