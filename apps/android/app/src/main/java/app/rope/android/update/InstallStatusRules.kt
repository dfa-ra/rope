package app.rope.android.update

/**
 * PackageInstaller STATUS_PENDING_USER_ACTION carries EXTRA_INTENT.
 * Only the system confirm-install activity may be launched from it —
 * never an arbitrary VIEW / SEND from another app.
 */
object InstallStatusRules {
    const val CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"

    fun allowConfirm(action: String?, componentPackage: String?): Boolean {
        val a = action.orEmpty()
        val pkg = componentPackage.orEmpty()
        if (a == CONFIRM_INSTALL) return true
        return a.startsWith("android.content.pm.") && pkg.contains("packageinstaller")
    }

    /**
     * Signing-key conflict. APK may still be in Downloads; identity is not
     * ([PublicBackupRules.allowIdentityDump] is false).
     */
    fun signingKeyConflictMessage(): String =
        "Старая сборка подписана другим ключом CI. Удалите Rope и поставьте APK из Загрузок. " +
            "Ключ устройства туда больше не пишется — восстановите его из файла, который вы сохраняли сами."
}
