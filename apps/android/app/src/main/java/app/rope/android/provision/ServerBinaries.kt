package app.rope.android.provision

enum class ServerTarget(val title: String, val subtitle: String) {
    AUTO("Авто", "определить по SSH"),
    LINUX_AMD64("Linux x86_64", "большинство VPS"),
    LINUX_ARM64("Linux ARM64", "Oracle Ampere, Raspberry, Graviton"),
}

object ServerBinaries {
    const val DEFAULT_OWNER = "dfa-ra"
    const val DEFAULT_REPO = "rope"

    fun latestDownloadUrl(
        assetName: String,
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO,
    ): String = "https://github.com/$owner/$repo/releases/latest/download/$assetName"

    fun assetName(target: ServerTarget, unameMachine: String? = null): String = when (target) {
        ServerTarget.LINUX_AMD64 -> "rope-server-linux-amd64"
        ServerTarget.LINUX_ARM64 -> "rope-server-linux-arm64"
        ServerTarget.AUTO -> assetNameForUname(
            unameMachine ?: error("не удалось определить архитектуру VPS"),
        )
    }

    fun assetNameForUname(machine: String): String = when (normalizeUname(machine)) {
        "x86_64", "amd64" -> "rope-server-linux-amd64"
        "aarch64", "arm64" -> "rope-server-linux-arm64"
        else -> error(
            "нет готовой сборки для архитектуры «$machine». " +
                "Выберите Linux x86_64 или ARM64 либо укажите свой URL.",
        )
    }

    fun parseUnameMachine(output: String): String? =
        output.lineSequence()
            .map { it.trim() }
            .map(::normalizeUname)
            .lastOrNull { it.matches(Regex("[a-z0-9_]+")) }

    fun resolveDownloadUrl(form: ProvisionForm, unameOutput: String? = null): String {
        val custom = form.binaryUrl.trim()
        if (custom.isNotEmpty()) return custom
        val machine = if (form.target == ServerTarget.AUTO) {
            parseUnameMachine(unameOutput.orEmpty())
        } else {
            null
        }
        return latestDownloadUrl(assetName(form.target, machine))
    }

    private fun normalizeUname(machine: String): String = machine.trim().lowercase()
}
