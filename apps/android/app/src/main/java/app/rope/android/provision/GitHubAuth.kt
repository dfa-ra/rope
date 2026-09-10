package app.rope.android.provision

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * GitHub PAT may only go to github.com / api.github.com over HTTPS.
 * Host checks use OkHttp [HttpUrl] — the same parser [ReleaseFetcher] uses
 * to connect — so Java URI / OkHttp disagreements cannot attach Bearer
 * to the wrong host. Not envelope crypto.
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

    /** Owner / repo interpolated into PAT-bearing api.github.com paths. */
    fun pathToken(s: String): Boolean {
        if (s.isEmpty() || s == "." || s == ".." || s.length > 128) return false
        return s.all { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-' }
    }

    fun hostOf(url: String): String? {
        val t = url.trim()
        if (t.isEmpty()) return null
        if (t.any { it.isWhitespace() || it.isISOControl() }) return null
        val http = t.toHttpUrlOrNull() ?: return null
        if (!http.isHttps) return null
        return http.host
    }
}
