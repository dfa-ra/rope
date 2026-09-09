package app.rope.android

import app.rope.android.provision.GitHubAssetRef
import app.rope.android.provision.GitHubRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun rejectPathMetacharacters() {
        assertNull(
            GitHubRelease.parseBrowserUrl(
                "https://github.com/dfa-ra/rope/releases/download/../rope-server-linux-amd64",
            ),
        )
        assertNull(
            GitHubRelease.parseBrowserUrl(
                "https://github.com/dfa-ra/rope/releases/download/v0.1.0%3Ffoo/rope-server-linux-amd64",
            ),
        )
        assertNull(
            GitHubRelease.parseBrowserUrl(
                "https://github.com/evil.com%2F..%2Fdfa-ra/rope/releases/latest/download/rope-server-linux-amd64",
            ),
        )
        assertFalse(GitHubRelease.pathToken(".."))
        assertFalse(GitHubRelease.pathToken("v0.1.0?x=1"))
        assertFalse(GitHubRelease.pathToken("dfa-ra/../other"))
        assertTrue(GitHubRelease.pathToken("dfa-ra"))
        assertTrue(GitHubRelease.pathToken("v0.3.48"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun apiReleaseUrlRejectsDotDotTag() {
        GitHubRelease.apiReleaseUrl(GitHubAssetRef("dfa-ra", "rope", "asset", ".."))
    }
}
