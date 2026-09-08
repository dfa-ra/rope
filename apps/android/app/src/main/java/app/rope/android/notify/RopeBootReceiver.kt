package app.rope.android.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.rope.android.data.LocalStore

/** Restarts the keep-alive service after reboot when a signed-in profile exists. */
class RopeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED) return
        val hasProfile = runCatching {
            LocalStore(context.applicationContext).profile() != null
        }.getOrDefault(false)
        if (!hasProfile) return
        RopeConnectionService.start(context)
    }
}
