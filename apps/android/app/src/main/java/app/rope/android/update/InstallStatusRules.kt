package app.rope.android.update

/**
 * PackageInstaller STATUS_PENDING_USER_ACTION carries EXTRA_INTENT.
 * Only the system confirm-install / confirm-permissions activity may be
 * launched from it — never an arbitrary VIEW / SEND from another app.
 */
object InstallStatusRules {
    const val CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"
    const val CONFIRM_PERMISSIONS = "android.content.pm.action.CONFIRM_PERMISSIONS"

    private val installerPackages = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
    )

    fun allowConfirm(action: String?, componentPackage: String?): Boolean {
        val a = action.orEmpty()
        if (a != CONFIRM_INSTALL && a != CONFIRM_PERMISSIONS) return false
        val pkg = componentPackage.orEmpty()
        if (pkg.isEmpty()) return a == CONFIRM_INSTALL
        return pkg in installerPackages
    }

    /**
     * Signing-key conflict. APK may still be in Downloads; identity is not
     * ([PublicBackupRules.allowIdentityDump] is false).
     */
    fun signingKeyConflictMessage(): String =
        "Старая сборка подписана другим ключом CI. Удалите Rope и поставьте APK из Загрузок. " +
            "Ключ устройства туда больше не пишется — восстановите его из файла, который вы сохраняли сами."
}
