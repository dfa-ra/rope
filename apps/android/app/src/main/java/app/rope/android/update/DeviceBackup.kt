package app.rope.android.update

import org.json.JSONObject
import java.util.Base64

data class DeviceBackup(
    val identity: ByteArray,
    val profileJson: String,
    val githubToken: String = "",
    val sshJson: String = "",
) {
    /** Inner JSON. Do not write this to shared storage — use [toSealedBytes]. */
    fun toBytes(): ByteArray = JSONObject()
        .put("v", 1)
        .put("identity", Base64.getEncoder().encodeToString(identity))
        .put("profile", profileJson)
        .put("github_token", githubToken)
        .put("ssh", sshJson)
        .toString()
        .toByteArray()

    /**
     * At-rest wrap: `RODB` + Keystore AES-GCM of [toBytes].
     * Not envelope crypto — same wrap as [app.rope.android.data.IdentityVault].
     */
    fun toSealedBytes(encrypt: (ByteArray) -> ByteArray): ByteArray = MAGIC + encrypt(toBytes())

    companion object {
        const val FILE_NAME = "rope-device.backup"
        val MAGIC = byteArrayOf(0x52, 0x4F, 0x44, 0x42) // RODB

        fun isSealed(raw: ByteArray): Boolean =
            raw.size > MAGIC.size &&
                raw[0] == MAGIC[0] &&
                raw[1] == MAGIC[1] &&
                raw[2] == MAGIC[2] &&
                raw[3] == MAGIC[3]

        /** Cold-start auto-restore must not apply leftover cleartext v1 JSON. */
        fun allowAutoRestore(raw: ByteArray): Boolean = isSealed(raw)

        fun parse(raw: ByteArray): DeviceBackup {
            val o = JSONObject(String(raw, Charsets.UTF_8))
            if (o.optInt("v") != 1) error("непонятный файл восстановления")
            val id = o.optString("identity")
            if (id.isBlank()) error("в файле нет ключа устройства")
            return DeviceBackup(
                identity = Base64.getDecoder().decode(id),
                profileJson = o.optString("profile"),
                githubToken = o.optString("github_token"),
                sshJson = o.optString("ssh"),
            )
        }

        fun open(raw: ByteArray, decrypt: (ByteArray) -> ByteArray): DeviceBackup {
            if (isSealed(raw)) {
                return parse(decrypt(raw.copyOfRange(MAGIC.size, raw.size)))
            }
            return parse(raw)
        }
    }
}
