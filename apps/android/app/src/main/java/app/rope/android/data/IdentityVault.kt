package app.rope.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
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
