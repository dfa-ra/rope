package app.rope.android.update

import org.json.JSONObject
import java.util.Base64

data class DeviceBackup(
    val identity: ByteArray,
    val profileJson: String,
    val githubToken: String = "",
    val sshJson: String = "",
) {
    /**
     * Inner v1 JSON, including [githubToken] in the clear.
     * Public and still used by tests / SAF legacy parse. Do not write this
     * to Downloads or any shared storage. There is no export writer yet.
     */
    fun toBytes(): ByteArray = JSONObject()
        .put("v", 1)
        .put("identity", Base64.getEncoder().encodeToString(identity))
        .put("profile", profileJson)
        .put("github_token", githubToken)
        .put("ssh", sshJson)
        .toString()
        .toByteArray()

    /**
     * Wrap format: `RODB` + Keystore AES-GCM of [toBytes] (same layout as
     * [app.rope.android.data.IdentityVault]). Not envelope crypto. Tests-only
     * until a writer exists — do not add a PublicDownloads path. Future export
     * must call this, never [toBytes].
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

        /**
         * True only for `RODB` blobs. Production skip of leftover Downloads
         * JSON is the 0.3.35 reader removal; wire this helper if auto-restore
         * returns.
         */
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

        fun isRobk(raw: ByteArray): Boolean =
            raw.size >= 4 &&
                raw[0] == 0x52.toByte() &&
                raw[1] == 0x4F.toByte() &&
                raw[2] == 0x42.toByte() &&
                raw[3] == 0x4B.toByte()

        fun open(raw: ByteArray, decrypt: (ByteArray) -> ByteArray): DeviceBackup {
            if (isRobk(raw)) error("это экспорт переписки, не ключ устройства")
            if (isSealed(raw)) {
                return parse(decrypt(raw.copyOfRange(MAGIC.size, raw.size)))
            }
            return parse(raw)
        }
    }
}
