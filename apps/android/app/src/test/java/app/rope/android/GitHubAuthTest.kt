package app.rope.android

import app.rope.android.provision.GitHubAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAuthTest {
    private val token = "ghp_test_token"

    @Test
    fun bearerOnlyOnGithubHttps() {
        assertEquals(
            token,
            GitHubAuth.bearerFor(
                "https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64",
                token,
            ),
        )
        assertEquals(
            token,
            GitHubAuth.bearerFor("https://api.github.com/repos/dfa-ra/rope/releases/latest", token),
        )
    }

    @Test
    fun customBinaryUrlDoesNotGetPat() {
        assertNull(GitHubAuth.bearerFor("https://example.com/rope-server", token))
        assertNull(GitHubAuth.bearerFor("https://github.com.evil.example/rope", token))
        assertNull(GitHubAuth.bearerFor("http://github.com/dfa-ra/rope/releases/latest/download/x", token))
        assertNull(GitHubAuth.bearerFor("https://github.com.evil.example/x", "  $token  "))
    }

    @Test
    fun blankTokenNeverAttaches() {
        assertNull(GitHubAuth.bearerFor("https://github.com/dfa-ra/rope/releases/latest/download/x", ""))
        assertNull(GitHubAuth.bearerFor("https://github.com/dfa-ra/rope/releases/latest/download/x", "  "))
        assertNull(GitHubAuth.bearerFor("https://github.com/dfa-ra/rope/releases/latest/download/x", null))
    }

    @Test
    fun hostParser() {
        assertTrue(GitHubAuth.isGithubHost("GitHub.com"))
        assertTrue(GitHubAuth.isGithubHost("api.github.com."))
        assertFalse(GitHubAuth.isGithubHost("objects.githubusercontent.com"))
        assertEquals("github.com", GitHubAuth.hostOf("https://github.com/dfa-ra/rope"))
        assertNull(GitHubAuth.hostOf("http://github.com/dfa-ra/rope"))
        assertNull(GitHubAuth.hostOf("not a url"))
    }

    @Test
    fun hostOfUsesOkHttpNotJavaUri() {
        assertNull(GitHubAuth.hostOf("https://github.com/dfa-ra/rope\nhttps://evil.example/x"))
        assertNull(GitHubAuth.hostOf("https://github.com/dfa-ra/rope http://evil.example"))
        assertNull(GitHubAuth.bearerFor("https://github.com.evil.example/x", token))
        assertNull(GitHubAuth.bearerFor("https://github.com:443@evil.example/x", token))
        assertEquals("github.com", GitHubAuth.hostOf("HTTPS://GitHub.com/dfa-ra/rope"))
    }

    @Test
    fun pathTokenRejectsTraversal() {
        assertTrue(GitHubAuth.pathToken("dfa-ra"))
        assertTrue(GitHubAuth.pathToken("rope"))
        assertFalse(GitHubAuth.pathToken("."))
        assertFalse(GitHubAuth.pathToken(".."))
        assertFalse(GitHubAuth.pathToken("../evil"))
        assertFalse(GitHubAuth.pathToken("dfa/ra"))
        assertFalse(GitHubAuth.pathToken(""))
        assertFalse(GitHubAuth.pathToken("x".repeat(129)))
    }
}
