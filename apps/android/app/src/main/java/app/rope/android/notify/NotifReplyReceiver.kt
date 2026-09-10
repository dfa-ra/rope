package app.rope.android.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import app.rope.android.RopeApp
import app.rope.android.data.NotifReplyRules

/** Shade RemoteInput «Ответить». Explicit, not exported. No FCM. */
class NotifReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotifReplyRules.ACTION) return
        val chatId = NotifReplyRules.chatId(intent.getStringExtra(NotifReplyRules.EXTRA_CHAT_ID)) ?: return
        if (!NotifReplyRules.allowsReply(chatId)) return
        val typed = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotifReplyRules.REMOTE_KEY)
            ?.toString()
        val text = NotifReplyRules.text(typed) ?: return
        RopeConnectionService.start(context)
        val app = context.applicationContext as? RopeApp ?: return
        app.repo.sendFromNotification(chatId, text)
        val notifyId = intent.getIntExtra(NotifReplyRules.EXTRA_NOTIFY_ID, 0)
        if (notifyId != 0) {
            NotificationManagerCompat.from(context).cancel(notifyId)
        }
    }
}
