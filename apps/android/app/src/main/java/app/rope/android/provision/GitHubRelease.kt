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

    /** Owner / repo / tag / asset interpolated into api.github.com paths with a PAT. */
    fun pathToken(s: String): Boolean {
        if (s.isEmpty() || s == "." || s == ".." || s.length > 128) return false
        return s.all { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-' }
    }

    fun parseBrowserUrl(url: String): GitHubAssetRef? {
        latestDownload.matchEntire(url.trim())?.let { m ->
            return refOrNull(m.groupValues[1], m.groupValues[2], m.groupValues[3], null)
        }
        taggedDownload.matchEntire(url.trim())?.let { m ->
            return refOrNull(m.groupValues[1], m.groupValues[2], m.groupValues[4], m.groupValues[3])
        }
        return null
    }

    fun apiReleaseUrl(ref: GitHubAssetRef): String {
        require(pathToken(ref.owner) && pathToken(ref.repo) && (ref.tag == null || pathToken(ref.tag))) {
            "bad github path"
        }
        return if (ref.tag == null) {
            "https://api.github.com/repos/${ref.owner}/${ref.repo}/releases/latest"
        } else {
            "https://api.github.com/repos/${ref.owner}/${ref.repo}/releases/tags/${ref.tag}"
        }
    }

    fun apiAssetUrl(owner: String, repo: String, assetId: Long): String {
        require(pathToken(owner) && pathToken(repo) && assetId > 0) { "bad github path" }
        return "https://api.github.com/repos/$owner/$repo/releases/assets/$assetId"
    }

    private fun refOrNull(owner: String, repo: String, asset: String, tag: String?): GitHubAssetRef? {
        if (!pathToken(owner) || !pathToken(repo) || !pathToken(asset)) return null
        if (tag != null && !pathToken(tag)) return null
        return GitHubAssetRef(owner, repo, asset, tag)
    }
}
