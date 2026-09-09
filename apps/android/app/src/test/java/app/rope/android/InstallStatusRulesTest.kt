package app.rope.android

import app.rope.android.update.InstallStatusRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallStatusRulesTest {
    @Test
    fun confirmInstallActionIsAllowed() {
        assertTrue(
            InstallStatusRules.allowConfirm(InstallStatusRules.CONFIRM_INSTALL, null),
        )
        assertTrue(
            InstallStatusRules.allowConfirm(
                InstallStatusRules.CONFIRM_INSTALL,
                "com.google.android.packageinstaller",
            ),
        )
        assertTrue(
            InstallStatusRules.allowConfirm(
                "android.content.pm.action.CONFIRM_PERMISSIONS",
                "com.android.packageinstaller",
            ),
        )
    }

    @Test
    fun arbitraryRedirectsAreRejected() {
        assertFalse(InstallStatusRules.allowConfirm("android.intent.action.VIEW", "com.android.chrome"))
        assertFalse(InstallStatusRules.allowConfirm("android.intent.action.SEND", "com.evil"))
        assertFalse(InstallStatusRules.allowConfirm(null, "com.google.android.packageinstaller"))
        assertFalse(InstallStatusRules.allowConfirm("android.intent.action.VIEW", "com.android.packageinstaller"))
        assertFalse(InstallStatusRules.allowConfirm("", ""))
        assertFalse(
            InstallStatusRules.allowConfirm(
                "android.intent.action.MAIN",
                "com.google.android.packageinstaller",
            ),
        )
    }
}
