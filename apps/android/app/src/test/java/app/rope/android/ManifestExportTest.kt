package app.rope.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestExportTest {
    @Test
    fun bootReceiverNotExported() {
        val xml = manifestText()
        val block = Regex(
            """<receiver\s+android:name="\.notify\.RopeBootReceiver"[^>]*>""",
            RegexOption.DOT_MATCHES_ALL,
        ).find(xml)?.value ?: error("RopeBootReceiver missing from manifest")
        assertTrue(block.contains("""android:exported="false""""))
        assertFalse(block.contains("""android:exported="true""""))
    }

    private fun manifestText(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("AndroidManifest.xml not found from ${File(".").canonicalPath}")
        return file.readText()
    }
}
