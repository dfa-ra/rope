package app.rope.android.provision

data class ProvisionForm(
    val host: String,
    val sshPort: Int,
    val user: String,
    val password: String,
    val keyPem: String,
    val listenPort: Int,
    val binaryUrl: String,
    val displayName: String,
    val githubToken: String,
    val upgrade: Boolean,
)
