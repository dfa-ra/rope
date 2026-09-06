package app.rope.android.provision

import android.content.Context
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.io.File
import java.util.concurrent.TimeUnit

data class ProvisionResult(
    val host: String,
    val port: Int,
    val serverId: String,
    val fingerprint: String,
    val setupToken: String,
)

class SshProvisioner(private val context: Context) {
    fun install(
        sshHost: String,
        sshPort: Int,
        sshUser: String,
        sshPassword: String?,
        sshKeyPem: String?,
        listenPort: Int,
        binaryUrl: String,
        upgrade: Boolean,
    ): ProvisionResult {
        val ssh = SSHClient()
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        ssh.connect(sshHost, sshPort)
        try {
            when {
                !sshKeyPem.isNullOrBlank() -> {
                    val keyFile = File(context.cacheDir, "ssh-key.pem")
                    keyFile.writeText(sshKeyPem)
                    val keys: KeyProvider = ssh.loadKeys(keyFile.absolutePath)
                    ssh.authPublickey(sshUser, keys)
                }
                !sshPassword.isNullOrBlank() -> ssh.authPassword(sshUser, sshPassword)
                else -> error("SSH password or key required")
            }
            val script = context.assets.open("install.sh").bufferedReader().use { it.readText() }
            ssh.newSFTPClient().use { sftp ->
                val remote = "/tmp/rope-install.sh"
                sftp.put(object : net.schmizz.sshj.xfer.InMemorySourceFile() {
                    override fun getName() = "rope-install.sh"
                    override fun getLength() = script.toByteArray().size.toLong()
                    override fun getInputStream() = script.byteInputStream()
                }, remote)
            }
            val sudo = if (sshUser == "root") "" else "sudo "
            val upgradeFlag = if (upgrade) " --upgrade" else ""
            val cmd = """
                set -euo pipefail
                ${sudo}chmod +x /tmp/rope-install.sh
                curl -fsSL -o /tmp/rope-server "$binaryUrl"
                ${sudo}chmod +x /tmp/rope-server
                ${sudo}/tmp/rope-install.sh --binary /tmp/rope-server --host $sshHost --port $listenPort$upgradeFlag
            """.trimIndent()
            val output = exec(ssh, cmd)
            if (!output.contains("ROPE_INSTALL_OK") && !output.contains("upgraded binary")) {
                error("installer failed:\n$output")
            }
            if (output.contains("upgraded binary")) {
                return ProvisionResult(sshHost, listenPort, "", "", "")
            }
            fun field(name: String) = Regex("$name=(\\S+)").find(output)?.groupValues?.get(1)
                ?: error("missing $name in installer output")
            return ProvisionResult(
                host = field("HOST"),
                port = field("PORT").toInt(),
                serverId = field("SERVER_ID"),
                fingerprint = field("FINGERPRINT"),
                setupToken = field("SETUP_TOKEN"),
            )
        } finally {
            ssh.disconnect()
        }
    }

    private fun exec(ssh: SSHClient, command: String): String {
        val session: Session = ssh.startSession()
        session.allocateDefaultPTY()
        try {
            val cmd = session.exec(command)
            cmd.join(180, TimeUnit.SECONDS)
            val stdout = cmd.inputStream.bufferedReader().readText()
            val stderr = cmd.errorStream.bufferedReader().readText()
            return stdout + "\n" + stderr
        } finally {
            session.close()
        }
    }
}
