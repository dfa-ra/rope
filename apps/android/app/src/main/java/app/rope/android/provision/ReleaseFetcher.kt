package app.rope.android.provision

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ReleaseFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .callTimeout(120, TimeUnit.SECONDS)
        .build(),
) {
    fun downloadTo(url: String, dest: File, githubToken: String?) {
        val token = githubToken?.trim().orEmpty().ifBlank { null }
        val gh = GitHubRelease.parseBrowserUrl(url)
        if (gh != null && token != null) {
            downloadPrivateAsset(gh, dest, token)
            return
        }
        if (!tryDirect(url, dest, token)) {
            if (gh != null && token == null) {
                error(
                    "GitHub вернул отказ. Для приватного репозитория нужен Personal Access Token " +
                        "(Contents: Read). Токен остаётся на телефоне и на VPS не копируется.",
                )
            }
            error("не удалось скачать $url")
        }
    }

    private fun tryDirect(url: String, dest: File, token: String?): Boolean {
        val req = Request.Builder().url(url).apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body ?: return false
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
            return dest.length() > 64
        }
    }

    private fun downloadPrivateAsset(ref: GitHubAssetRef, dest: File, token: String) {
        val metaReq = Request.Builder()
            .url(GitHubRelease.apiReleaseUrl(ref))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        val assetId = client.newCall(metaReq).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("GitHub API ${resp.code}: не удалось прочитать release (проверь token и доступ к репо)")
            }
            val assets = JSONObject(text).optJSONArray("assets")
                ?: error("в release нет assets")
            var id: Long? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name") == ref.assetName) {
                    id = a.getLong("id")
                    break
                }
            }
            id ?: error("asset ${ref.assetName} не найден в release")
        }
        val binReq = Request.Builder()
            .url(GitHubRelease.apiAssetUrl(ref.owner, ref.repo, assetId))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/octet-stream")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        client.newCall(binReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("GitHub asset ${resp.code}: скачивание запрещено")
            }
            val body = resp.body ?: error("empty asset body")
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        if (dest.length() < 64) error("скачанный бинарник слишком маленький")
    }
}
