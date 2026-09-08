package app.rope.android.provision

data class GitHubAssetRef(
    val owner: String,
    val repo: String,
    val assetName: String,
    val tag: String?, // null = latest
)

object GitHubRelease {
    private val latestDownload = Regex(
        """^https://github\.com/([^/]+)/([^/]+)/releases/latest/download/([^/?#]+)""",
    )
    private val taggedDownload = Regex(
        """^https://github\.com/([^/]+)/([^/]+)/releases/download/([^/]+)/([^/?#]+)""",
    )

    fun parseBrowserUrl(url: String): GitHubAssetRef? {
        latestDownload.matchEntire(url.trim())?.let { m ->
            return GitHubAssetRef(m.groupValues[1], m.groupValues[2], m.groupValues[3], null)
        }
        taggedDownload.matchEntire(url.trim())?.let { m ->
            return GitHubAssetRef(m.groupValues[1], m.groupValues[2], m.groupValues[4], m.groupValues[3])
        }
        return null
    }

    fun apiReleaseUrl(ref: GitHubAssetRef): String =
        if (ref.tag == null) {
            "https://api.github.com/repos/${ref.owner}/${ref.repo}/releases/latest"
        } else {
            "https://api.github.com/repos/${ref.owner}/${ref.repo}/releases/tags/${ref.tag}"
        }

    fun apiAssetUrl(owner: String, repo: String, assetId: Long): String =
        "https://api.github.com/repos/$owner/$repo/releases/assets/$assetId"
}
