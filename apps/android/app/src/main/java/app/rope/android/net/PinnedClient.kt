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
     * Fail-closed: a blank expected pin never allows TURNS/TLS.
     * Hex compare only — not a crypto implementation.
     */
    fun tlsPinAllows(presentedHex: String, expectedHex: String): Boolean {
        val pin = expectedHex.trim()
        if (pin.isEmpty()) return false
        return presentedHex.trim().equals(pin, ignoreCase = true)
    }

    fun http(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    fun pinned(expectedFp: String): OkHttpClient {
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                val presented = fingerprintHex(chain[0].encoded)
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
