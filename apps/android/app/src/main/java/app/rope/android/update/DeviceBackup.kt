package app.rope.android.update

import org.json.JSONObject
import java.util.Base64

data class DeviceBackup(
    val identity: ByteArray,
    val profileJson: String,
    val githubToken: String = "",
    val sshJson: String = "",
) {
    fun toBytes(): ByteArray = JSONObject()
        .put("v", 1)
        .put("identity", Base64.getEncoder().encodeToString(identity))
        .put("profile", profileJson)
        .put("github_token", githubToken)
        .put("ssh", sshJson)
        .toString()
        .toByteArray()

    companion object {
        const val FILE_NAME = "rope-device.backup"

        fun parse(raw: ByteArray): DeviceBackup {
            val o = JSONObject(String(raw))
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
    }
}
