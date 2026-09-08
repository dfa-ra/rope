package app.rope.android

import app.rope.android.provision.GitHubRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleaseTest {
    @Test
    fun parseLatestDownload() {
        val ref = GitHubRelease.parseBrowserUrl(
            "https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64",
        )!!
        assertEquals("dfa-ra", ref.owner)
        assertEquals("rope", ref.repo)
        assertEquals("rope-server-linux-amd64", ref.assetName)
        assertNull(ref.tag)
        assertEquals(
            "https://api.github.com/repos/dfa-ra/rope/releases/latest",
            GitHubRelease.apiReleaseUrl(ref),
        )
    }

    @Test
    fun parseTaggedDownload() {
        val ref = GitHubRelease.parseBrowserUrl(
            "https://github.com/dfa-ra/rope/releases/download/v0.1.0/rope-server-linux-amd64",
        )!!
        assertEquals("v0.1.0", ref.tag)
        assertEquals("rope-server-linux-amd64", ref.assetName)
        assertEquals(
            "https://api.github.com/repos/dfa-ra/rope/releases/tags/v0.1.0",
            GitHubRelease.apiReleaseUrl(ref),
        )
    }

    @Test
    fun rejectNonGithub() {
        assertNull(GitHubRelease.parseBrowserUrl("https://example.com/rope-server"))
    }
}
