package app.rope.android.provision

import android.content.Context
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.xfer.FileSystemFile
import java.io.File
import java.util.concurrent.TimeUnit

data class ProvisionResult(
    val host: String,
    val port: Int,
    val serverId: String,
    val fingerprint: String,
    val setupToken: String,
)

class SshProvisioner(
    private val context: Context,
    private val fetcher: ReleaseFetcher = ReleaseFetcher(),
) {
    fun install(form: ProvisionForm): ProvisionResult {
        val localBin = File(context.cacheDir, "rope-server-linux")
        fetcher.downloadTo(form.binaryUrl, localBin, form.githubToken.ifBlank { null })
        localBin.setExecutable(true)

        val ssh = SSHClient()
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        ssh.connect(form.host, form.sshPort)
        try {
            when {
                form.keyPem.isNotBlank() -> {
                    val keyFile = File(context.cacheDir, "ssh-key.pem")
                    keyFile.writeText(form.keyPem)
                    val keys: KeyProvider = ssh.loadKeys(keyFile.absolutePath)
                    ssh.authPublickey(form.user, keys)
                }
                form.password.isNotBlank() -> ssh.authPassword(form.user, form.password)
                else -> error("SSH password or key required")
            }
            val script = context.assets.open("install.sh").bufferedReader().use { it.readText() }
            ssh.newSFTPClient().use { sftp ->
                sftp.put(object : net.schmizz.sshj.xfer.InMemorySourceFile() {
                    override fun getName() = "rope-install.sh"
                    override fun getLength() = script.toByteArray().size.toLong()
                    override fun getInputStream() = script.byteInputStream()
                }, "/tmp/rope-install.sh")
                sftp.put(FileSystemFile(localBin), "/tmp/rope-server")
            }
            val sudo = if (form.user == "root") "" else "sudo "
            val upgradeFlag = if (form.upgrade) " --upgrade" else ""
            val cmd = """
                set -euo pipefail
                ${sudo}chmod +x /tmp/rope-install.sh /tmp/rope-server
                ${sudo}/tmp/rope-install.sh --binary /tmp/rope-server --host ${form.host} --port ${form.listenPort}$upgradeFlag
            """.trimIndent()
            val output = exec(ssh, cmd)
            if (!output.contains("ROPE_INSTALL_OK") && !output.contains("upgraded binary")) {
                error("installer failed:\n$output")
            }
            if (output.contains("upgraded binary")) {
                return ProvisionResult(form.host, form.listenPort, "", "", "")
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
            localBin.delete()
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
