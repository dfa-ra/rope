package app.rope.android.provision

import java.net.URI

/**
 * GitHub PAT may only go to github.com / api.github.com over HTTPS.
 * [ReleaseFetcher.tryDirect] used to attach Bearer to any custom binary URL.
 * Not envelope crypto.
 */
object GitHubAuth {
    fun bearerFor(url: String, token: String?): String? {
        val t = token?.trim().orEmpty()
        if (t.isEmpty()) return null
        val host = hostOf(url) ?: return null
        return if (isGithubHost(host)) t else null
    }

    fun isGithubHost(host: String): Boolean {
        val h = host.trim().lowercase().trimEnd('.')
        return h == "github.com" || h == "api.github.com"
    }

    fun hostOf(url: String): String? {
        return try {
            val uri = URI(url.trim())
            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            uri.host
        } catch (_: Exception) {
            null
        }
    }
}
