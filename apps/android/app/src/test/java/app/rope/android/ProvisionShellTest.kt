package app.rope.android

import app.rope.android.provision.ProvisionShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvisionShellTest {
    @Test
    fun quoteWrapsAndEscapesSingleQuotes() {
        assertEquals("'vps.example'", ProvisionShell.quote("vps.example"))
        assertEquals("'a'\\''b'", ProvisionShell.quote("a'b"))
    }

    @Test
    fun hostAcceptsDnsAndIp() {
        assertTrue(ProvisionShell.hostOK("vps.example.com"))
        assertTrue(ProvisionShell.hostOK("10.0.0.8"))
        assertTrue(ProvisionShell.hostOK("2001:db8::1"))
        assertTrue(ProvisionShell.hostOK("[2001:db8::1]"))
    }

    @Test
    fun hostRejectsShellMetacharacters() {
        assertFalse(ProvisionShell.hostOK(""))
        assertFalse(ProvisionShell.hostOK("vps.example.com; id"))
        assertFalse(ProvisionShell.hostOK("vps.example.com && reboot"))
        assertFalse(ProvisionShell.hostOK("vps\$HOST"))
        assertFalse(ProvisionShell.hostOK("vps`id`"))
        assertFalse(ProvisionShell.hostOK("vps example"))
        assertFalse(ProvisionShell.hostOK("vps\nexample"))
    }

    @Test
    fun portRange() {
        assertTrue(ProvisionShell.portOK(8443))
        assertFalse(ProvisionShell.portOK(0))
        assertFalse(ProvisionShell.portOK(65536))
    }
}
