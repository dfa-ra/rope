package app.rope.android

import app.rope.android.provision.ProvisionForm
import app.rope.android.provision.ServerBinaries
import app.rope.android.provision.ServerTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerBinariesTest {
    @Test
    fun latestUrlForAmd64() {
        assertEquals(
            "https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64",
            ServerBinaries.latestDownloadUrl("rope-server-linux-amd64"),
        )
    }

    @Test
    fun mapsUnameToAssets() {
        assertEquals("rope-server-linux-amd64", ServerBinaries.assetNameForUname("x86_64"))
        assertEquals("rope-server-linux-amd64", ServerBinaries.assetNameForUname("AMD64"))
        assertEquals("rope-server-linux-arm64", ServerBinaries.assetNameForUname("aarch64"))
        assertEquals("rope-server-linux-arm64", ServerBinaries.assetNameForUname("arm64"))
    }

    @Test
    fun parseUnameIgnoresPtyNoise() {
        val out = """
            Last login: today
            x86_64
        """.trimIndent()
        assertEquals("x86_64", ServerBinaries.parseUnameMachine(out))
    }

    @Test
    fun parseUnameEmpty() {
        assertNull(ServerBinaries.parseUnameMachine("\n\n"))
    }

    @Test
    fun resolveExplicitTargetIgnoresUname() {
        val form = sampleForm(target = ServerTarget.LINUX_ARM64, binaryUrl = "")
        assertEquals(
            "https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-arm64",
            ServerBinaries.resolveDownloadUrl(form, "x86_64"),
        )
    }

    @Test
    fun resolveAutoUsesUname() {
        val form = sampleForm(target = ServerTarget.AUTO, binaryUrl = "")
        assertEquals(
            "https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64",
            ServerBinaries.resolveDownloadUrl(form, "x86_64\n"),
        )
    }

    @Test
    fun customUrlWins() {
        val form = sampleForm(target = ServerTarget.LINUX_AMD64, binaryUrl = "https://example.com/rope")
        assertEquals("https://example.com/rope", ServerBinaries.resolveDownloadUrl(form, "aarch64"))
        assertTrue(ServerBinaries.isHttpsDownload("https://example.com/rope"))
        assertTrue(ServerBinaries.isHttpsDownload("HTTPS://example.com/rope"))
    }

    @Test(expected = IllegalStateException::class)
    fun customHttpUrlRejected() {
        val form = sampleForm(target = ServerTarget.LINUX_AMD64, binaryUrl = "http://example.com/rope")
        ServerBinaries.resolveDownloadUrl(form, "aarch64")
    }

    @Test
    fun customNonHttpsRejected() {
        assertFalse(ServerBinaries.isHttpsDownload("http://evil/x"))
        assertFalse(ServerBinaries.isHttpsDownload("file:///data/local/tmp/rope"))
        assertFalse(ServerBinaries.isHttpsDownload("https://example.com/rope\nhttp://evil"))
        assertFalse(ServerBinaries.isHttpsDownload("https://example.com/rope http://evil"))
        assertFalse(ServerBinaries.isHttpsDownload(""))
    }

    @Test(expected = IllegalStateException::class)
    fun unknownUnameFails() {
        ServerBinaries.assetNameForUname("ppc64le")
    }

    private fun sampleForm(target: ServerTarget, binaryUrl: String) = ProvisionForm(
        host = "1.2.3.4",
        sshPort = 22,
        user = "root",
        password = "x",
        keyPem = "",
        listenPort = 8443,
        target = target,
        binaryUrl = binaryUrl,
        displayName = "owner",
        githubToken = "",
        upgrade = false,
    )
}
