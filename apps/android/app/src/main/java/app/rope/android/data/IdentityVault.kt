package app.rope.android.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class IdentityVault(private val context: Context) {
    private val file = File(context.filesDir, "identity.ropi.enc")

    fun exists(): Boolean = file.exists()

    fun save(plain: ByteArray) {
        file.writeBytes(wrap(plain))
    }

    fun load(): ByteArray = unwrap(file.readBytes())

    /** Keystore AES-GCM wrap. At-rest only — not envelope crypto. */
    fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return pack(cipher.iv, cipher.doFinal(plain))
    }

    fun unwrap(blob: ByteArray): ByteArray {
        val (iv, ct) = unpack(blob)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    fun hmacPin(pin: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(getOrCreateHmacKey())
        return mac.doFinal(pin.toByteArray(Charsets.UTF_8))
    }

    /** Best-effort user-auth wrap. PIN HMAC still gates RAM even if this no-ops. */
    fun rotateWrapForLock(validitySeconds: Int) {
        val plain = runCatching { load() }.getOrNull() ?: return
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            ks.deleteEntry(ALIAS)
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val builder = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
            if (Build.VERSION.SDK_INT >= 30) {
                builder.setUserAuthenticationParameters(
                    validitySeconds.coerceAtLeast(0),
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(validitySeconds)
            }
            gen.init(builder.build())
            gen.generateKey()
            save(plain)
        } catch (_: Exception) {
            runCatching {
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                ks.deleteEntry(ALIAS)
            }
            save(plain)
        }
    }

    private fun getOrCreateHmacKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(HMAC_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(HMAC_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return gen.generateKey()
    }

    companion object {
        private const val ALIAS = "rope-identity-wrap"
        private const val HMAC_ALIAS = "rope-pin-hmac"

        /** On-disk layout: 1-byte IV length, IV, ciphertext. */
        fun pack(iv: ByteArray, ct: ByteArray): ByteArray {
            require(iv.isNotEmpty() && iv.size <= 255)
            return byteArrayOf(iv.size.toByte()) + iv + ct
        }

        fun unpack(blob: ByteArray): Pair<ByteArray, ByteArray> {
            if (blob.isEmpty()) error("empty wrap")
            val ivLen = blob[0].toInt() and 0xff
            if (ivLen < 1 || blob.size < 1 + ivLen) error("bad wrap")
            val iv = blob.copyOfRange(1, 1 + ivLen)
            val ct = blob.copyOfRange(1 + ivLen, blob.size)
            return iv to ct
        }
    }
}
