package app.rope.android

import app.rope.android.data.NotifyRules
import app.rope.android.data.SettingsRules
import app.rope.android.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRulesTest {
    @Test
    fun fingerprintGroupsFourHexChars() {
        val raw = "AABBCCDDEEFF00112233445566778899aabbccddeeff00112233445566778899"
        val grouped = SettingsRules.formatHexGroups(raw)
        assertEquals("aabb ccdd eeff 0011 2233 4455 6677 8899 aabb ccdd eeff 0011 2233 4455 6677 8899", grouped)
        assertEquals(raw.lowercase(), SettingsRules.copyFingerprintValue(grouped))
        assertEquals("dead beef", SettingsRules.formatHexGroups("dead:beef"))
    }

    @Test
    fun endpointUsesTlsScheme() {
        assertEquals("https://vps.example:8443", SettingsRules.formatEndpoint("vps.example", 8443, true))
        assertEquals("http://10.0.2.2:8443", SettingsRules.formatEndpoint("10.0.2.2", 8443, false))
    }

    @Test
    fun roleLabelsStayShort() {
        assertEquals("owner", SettingsRules.roleLabel("OWNER"))
        assertEquals("owner", SettingsRules.roleLabel("owner"))
        assertEquals("гость", SettingsRules.roleLabel(null))
        assertEquals("гость", SettingsRules.roleLabel(""))
        assertEquals("member", SettingsRules.roleLabel("member"))
    }

    @Test
    fun copyNeverMentionsFcmAsAPath() {
        val hint = SettingsRules.notificationsHint().lowercase()
        assertTrue(hint.contains("fcm"))
        assertTrue(hint.contains("нет") || hint.contains("google"))
        assertTrue(hint.contains("звонок"))
        assertFalse(SettingsRules.aboutBody().contains("FCM"))
        assertTrue(SettingsRules.appearanceHint().contains("логотип"))
        assertTrue(SettingsRules.notifyPreviewHint().contains("шторке"))
        assertFalse(SettingsRules.notifyPreviewHint().contains("FCM"))
        assertTrue(SettingsRules.serverPinHint(true).contains("не видит"))
        assertTrue(SettingsRules.serverPinHint(false).contains("HTTP"))
    }

    @Test
    fun globalMuteSilencesMessagesNotWhenAlsoChatMuted() {
        assertFalse(NotifyRules.shouldAlert(false, false, muted = false, globalMuted = true))
        assertFalse(NotifyRules.shouldAlert(true, false, muted = false, globalMuted = true))
        assertTrue(NotifyRules.shouldAlert(false, false, muted = false, globalMuted = false))
        assertFalse(NotifyRules.shouldAlert(true, true, muted = false, globalMuted = false))
    }

    @Test
    fun themeModesAreOnlyLightAndDark() {
        assertEquals(2, ThemeMode.entries.size)
        assertTrue(ThemeMode.DARK in ThemeMode.entries)
        assertTrue(ThemeMode.LIGHT in ThemeMode.entries)
    }

    @Test
    fun settingsIsAHomeShortcut() {
        assertEquals(Screen.Settings, HomeCtas.settings.destination)
        assertTrue(HomeCtas.shortcuts.contains(HomeCtas.settings))
        assertEquals("Настройки", HomeCtas.settings.label)
    }
}
