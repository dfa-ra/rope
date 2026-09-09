package app.rope.android.net

import okhttp3.OkHttpClient
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object PinnedClient {
    fun fingerprintHex(der: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(der).joinToString("") { "%02x".format(it) }
    }

    /**
     * Fail-closed: only a 64-char SHA-256 hex pin may allow TURNS/TLS.
     * Hex compare only — not a crypto implementation.
     */
    fun sha256HexPin(s: String): Boolean {
        val h = s.trim()
        if (h.length != 64) return false
        return h.all { ch -> ch in '0'..'9' || ch in 'a'..'f' || ch in 'A'..'F' }
    }

    fun tlsPinAllows(presentedHex: String, expectedHex: String): Boolean {
        val pin = expectedHex.trim()
        val got = presentedHex.trim()
        if (!sha256HexPin(pin) || !sha256HexPin(got)) return false
        return pin.equals(got, ignoreCase = true)
    }

    fun presentedLeafHex(chain: Array<X509Certificate>): String? {
        if (chain.isEmpty()) return null
        return fingerprintHex(chain[0].encoded)
    }

    fun http(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    fun pinned(expectedFp: String): OkHttpClient {
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                val presented = presentedLeafHex(chain)
                    ?: throw javax.net.ssl.SSLException("empty chain")
                if (!tlsPinAllows(presented, expectedFp)) {
                    throw javax.net.ssl.SSLException("fingerprint mismatch")
                }
            }
        }
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf(tm), java.security.SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(ctx.socketFactory, tm)
            .hostnameVerifier { _, _ -> true }
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
