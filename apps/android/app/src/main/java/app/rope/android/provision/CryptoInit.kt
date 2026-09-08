package app.rope.android.provision

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.crypto.KeyAgreement

object CryptoInit {
    @Volatile
    private var ready = false

    fun ensureModernBc() {
        if (ready) return
        synchronized(this) {
            if (ready) return
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            try {
                KeyAgreement.getInstance("X25519", BouncyCastleProvider.PROVIDER_NAME)
            } catch (_: Exception) {
                // SSH will retry without curve25519.
            }
            ready = true
        }
    }
}
