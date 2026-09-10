package app.rope.android.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.rope.android.RopeApp
import app.rope.android.data.NotifReadRules

/** Shade «Прочитать». Explicit, not exported. No FCM. */
class NotifReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotifReadRules.ACTION) return
        val chatId = NotifReadRules.chatId(intent.getStringExtra(NotifReadRules.EXTRA_CHAT_ID)) ?: return
        if (!NotifReadRules.allows(chatId)) return
        RopeConnectionService.start(context)
        val app = context.applicationContext as? RopeApp ?: return
        app.repo.markChatReadFromNotification(chatId)
        val notifyId = intent.getIntExtra(NotifReadRules.EXTRA_NOTIFY_ID, 0)
        if (notifyId != 0) {
            NotificationManagerCompat.from(context).cancel(notifyId)
        }
    }
}
