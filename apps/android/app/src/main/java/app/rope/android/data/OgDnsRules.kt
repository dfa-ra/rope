package app.rope.android.data

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Phone-only OG OkHttp DNS. Drops loopback / RFC1918 / link-local / ULA so a
 * hostname cannot rebind the link-preview fetch onto an internal IP. Not crypto.
 */
object OgDnsRules {
    fun isBlocked(addr: InetAddress): Boolean {
        val effective = unwrapMapped(addr)
        if (effective.isLoopbackAddress || effective.isLinkLocalAddress) return true
        if (effective is Inet4Address && effective.isSiteLocalAddress) return true
        if (effective is Inet6Address) {
            if (effective.isLoopbackAddress || effective.isLinkLocalAddress || effective.isSiteLocalAddress) {
                return true
            }
            val b = effective.address
            if (b.size == 16 && (b[0].toInt() and 0xfe) == 0xfc) return true
        }
        return false
    }

    @Throws(UnknownHostException::class)
    fun lookup(hostname: String, resolve: (String) -> List<InetAddress>): List<InetAddress> {
        val kept = resolve(hostname).filterNot { isBlocked(it) }
        if (kept.isEmpty()) throw UnknownHostException(hostname)
        return kept
    }

    private fun unwrapMapped(addr: InetAddress): InetAddress {
        if (addr !is Inet6Address) return addr
        val b = addr.address
        if (b.size != 16) return addr
        val mapped = (0..9).all { b[it].toInt() == 0 } &&
            b[10] == 0xff.toByte() &&
            b[11] == 0xff.toByte()
        if (!mapped) return addr
        return InetAddress.getByAddress(b.copyOfRange(12, 16))
    }
}
