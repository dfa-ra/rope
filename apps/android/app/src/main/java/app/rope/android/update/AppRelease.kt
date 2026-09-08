package app.rope.android.update

object AppRelease {
    private val apkName = Regex("""^rope-(\d+\.\d+\.\d+)(?:-debug)?\.apk$""")

    fun parseApkVersion(fileName: String): String? =
        apkName.matchEntire(fileName.trim())?.groupValues?.get(1)

    fun stripBuildSuffix(versionName: String): String =
        versionName.substringBefore("-")

    fun isNewer(remote: String, local: String): Boolean =
        compareSemver(remote, stripBuildSuffix(local)) > 0

    fun compareSemver(a: String, b: String): Int {
        val left = a.split('.').map { it.toIntOrNull() ?: 0 }
        val right = b.split('.').map { it.toIntOrNull() ?: 0 }
        val n = maxOf(left.size, right.size)
        for (i in 0 until n) {
            val d = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }

    fun pickApkAsset(names: List<String>, preferDebug: Boolean): String? {
        val apks = names.filter { parseApkVersion(it) != null }
        if (apks.isEmpty()) return null
        val debug = apks.filter { it.endsWith("-debug.apk") }
        val release = apks.filter { !it.endsWith("-debug.apk") }
        return if (preferDebug) debug.lastOrNull() ?: release.lastOrNull()
        else release.lastOrNull() ?: debug.lastOrNull()
    }
}
