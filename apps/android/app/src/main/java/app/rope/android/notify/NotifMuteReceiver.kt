package app.rope.android.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.rope.android.RopeApp
import app.rope.android.data.NotifMuteRules

/** Shade «Без звука». Explicit, not exported. No FCM. */
class NotifMuteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotifMuteRules.ACTION) return
        val chatId = NotifMuteRules.chatId(intent.getStringExtra(NotifMuteRules.EXTRA_CHAT_ID)) ?: return
        if (!NotifMuteRules.allows(chatId)) return
        RopeConnectionService.start(context)
        val app = context.applicationContext as? RopeApp ?: return
        app.repo.muteChatFromNotification(chatId)
        val notifyId = intent.getIntExtra(NotifMuteRules.EXTRA_NOTIFY_ID, 0)
        if (notifyId != 0) {
            NotificationManagerCompat.from(context).cancel(notifyId)
        }
    }
}
