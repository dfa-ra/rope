package app.rope.android.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.rope.android.MainActivity

class RopeNotifier(private val context: Context) {
    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(MSG, "Сообщения", NotificationManager.IMPORTANCE_DEFAULT),
            )
            mgr.createNotificationChannel(
                NotificationChannel(CALL, "Звонки", NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }

    fun message(title: String, body: String, notifyId: Int = body.hashCode()) {
        val intent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, MSG)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notifyId, n) }
    }

    fun cancel(notifyId: Int) {
        NotificationManagerCompat.from(context).cancel(notifyId)
    }

    fun incomingCall(name: String) {
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val intent = PendingIntent.getActivity(
            context,
            2,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CALL)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("Входящий вызов")
            .setContentText(name)
            .setContentIntent(intent)
            .setFullScreenIntent(intent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(CALL_ID, n) }
    }

    fun clearCall() {
        NotificationManagerCompat.from(context).cancel(CALL_ID)
    }

    companion object {
        private const val MSG = "rope-messages"
        private const val CALL = "rope-calls"
        private const val CALL_ID = 7102
    }
}
