package app.rope.android.update

import app.rope.android.BuildConfig
import app.rope.android.provision.GitHubAuth
import app.rope.android.provision.GitHubRelease
import app.rope.android.provision.ReleaseFetcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class LatestApk(
    val tag: String,
    val version: String,
    val assetName: String,
    val downloadUrl: String,
)

class AppUpdater(
    private val owner: String = "dfa-ra",
    private val repo: String = "rope",
    private val fetcher: ReleaseFetcher = ReleaseFetcher(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    init {
        require(GitHubAuth.pathToken(owner) && GitHubAuth.pathToken(repo)) { "bad github path" }
    }
    fun latestApk(githubToken: String?, preferDebug: Boolean = BuildConfig.DEBUG): LatestApk {
        val token = githubToken?.trim().orEmpty().ifBlank { null }
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("не удалось проверить обновление (GitHub ${resp.code})")
            }
            val obj = JSONObject(text)
            val tag = obj.optString("tag_name").ifBlank { error("в release нет tag") }
            val assets = obj.optJSONArray("assets") ?: error("в release нет APK")
            val names = buildList {
                for (i in 0 until assets.length()) add(assets.getJSONObject(i).getString("name"))
            }
            val name = AppRelease.pickApkAsset(names, preferDebug)
                ?: error("в последнем release нет APK")
            val version = AppRelease.parseApkVersion(name) ?: error("непонятная версия $name")
            val url = "https://github.com/$owner/$repo/releases/latest/download/$name"
            return LatestApk(tag, version, name, url)
        }
    }

    fun download(latest: LatestApk, dest: File, githubToken: String?) {
        GitHubRelease.parseBrowserUrl(latest.downloadUrl)
            ?: error("некорректный URL ${latest.downloadUrl}")
        fetcher.downloadTo(latest.downloadUrl, dest, githubToken)
        if (dest.length() < 1024) error("скачанный APK слишком маленький")
    }
}
