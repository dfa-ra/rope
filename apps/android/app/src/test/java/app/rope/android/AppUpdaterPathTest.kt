package app.rope.android

import app.rope.android.provision.GitHubAuth
import app.rope.android.update.AppUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AppUpdaterPathTest {
    @Test
    fun defaultOwnerRepoArePathTokens() {
        AppUpdater()
        assertEquals(true, GitHubAuth.pathToken("dfa-ra"))
    }

    @Test
    fun traversalOwnerRejected() {
        try {
            AppUpdater(owner = "../evil", repo = "rope")
            fail("expected bad github path")
        } catch (_: IllegalArgumentException) {
        }
    }
}
