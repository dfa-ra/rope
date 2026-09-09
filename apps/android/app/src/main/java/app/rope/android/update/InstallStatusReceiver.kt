package app.rope.android.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import app.rope.android.RopeApp

/**
 * Non-exported callback for PackageInstaller. Do not handle INSTALL_STATUS
 * on the exported launcher activity — that is an intent-redirection hole.
 */
class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ApkInstaller.ACTION) return
        val repo = (context.applicationContext as? RopeApp)?.repo ?: return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> repo.onApkInstalled()
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                val pkg = confirm?.component?.packageName
                if (confirm != null && InstallStatusRules.allowConfirm(confirm.action, pkg)) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirm)
                } else {
                    repo.onApkInstallFailed("система не показала окно установки", status)
                }
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            -> repo.onApkInstallFailed(
                "Старая сборка подписана другим ключом CI. Ключ и логин лежат в Загрузках как ${DeviceBackup.FILE_NAME}. " +
                    "Удалите Rope, поставьте APK из Загрузок и на старте нажмите «Восстановить устройство».",
                status,
            )
            else -> repo.onApkInstallFailed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "установка не удалась ($status)",
                status,
            )
        }
    }
}
