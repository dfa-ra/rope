package app.rope.android.provision

import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.PublicKey

/**
 * First key in an install session is accepted; later keys must match.
 * Not persistent known_hosts — first-connect MITM is unchanged.
 */
object HostKeyPin {
    fun accept(pinned: ByteArray?, presented: ByteArray): Pair<Boolean, ByteArray?> {
        if (presented.isEmpty()) return false to pinned
        if (pinned == null) return true to presented.copyOf()
        return pinned.contentEquals(presented) to pinned
    }
}

class PinningHostKeyVerifier : HostKeyVerifier {
    private var pinned: ByteArray? = null

    override fun verify(hostname: String?, port: Int, key: PublicKey?): Boolean {
        val presented = key?.encoded ?: return false
        synchronized(this) {
            val (ok, next) = HostKeyPin.accept(pinned, presented)
            pinned = next
            return ok
        }
    }

    override fun findExistingAlgorithms(hostname: String?, port: Int): List<String> = emptyList()
}
