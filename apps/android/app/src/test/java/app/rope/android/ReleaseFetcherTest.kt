package app.rope.android

import app.rope.android.provision.ReleaseFetcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseFetcherTest {
    @Test
    fun isHttpsUrlAcceptsHttps() {
        assertTrue(ReleaseFetcher.isHttpsUrl("https://example.com/rope"))
        assertTrue(ReleaseFetcher.isHttpsUrl("HTTPS://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64"))
    }

    @Test
    fun isHttpsUrlRejectsNonHttpsAndControl() {
        assertFalse(ReleaseFetcher.isHttpsUrl("http://example.com/rope"))
        assertFalse(ReleaseFetcher.isHttpsUrl("file:///data/local/tmp/rope"))
        assertFalse(ReleaseFetcher.isHttpsUrl("https://example.com/rope\nhttp://evil"))
        assertFalse(ReleaseFetcher.isHttpsUrl("https://example.com/rope\r\nX: y"))
        assertFalse(ReleaseFetcher.isHttpsUrl("https://example.com/rope http://evil"))
        assertFalse(ReleaseFetcher.isHttpsUrl(""))
    }
}
