package app.rope.android.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.rope.android.MainActivity
import app.rope.android.data.InAppVibRules
import app.rope.android.data.NotifyRules

class RopeNotifier(private val context: Context) {
    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(MSG, "Сообщения", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    enableVibration(true)
                    vibrationPattern = InAppVibRules.pattern(true)
                },
            )
            mgr.createNotificationChannel(
                NotificationChannel(CALL, "Звонки", NotificationManager.IMPORTANCE_HIGH).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                },
            )
        }
    }

    fun message(title: String, body: String, notifyId: Int = body.hashCode(), vibrate: Boolean = true) {
        val intent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(MSG, "Сообщения", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    enableVibration(vibrate)
                    vibrationPattern = InAppVibRules.pattern(vibrate)
                },
            )
        }
        val n = NotificationCompat.Builder(context, MSG)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setVibrate(InAppVibRules.pattern(vibrate))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notifyId, n) }
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
        val copy = NotifyRules.incomingCallText(name)
        val publicN = NotificationCompat.Builder(context, CALL)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle(copy.title)
            .setContentText(copy.publicBody)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
        val n = NotificationCompat.Builder(context, CALL)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle(copy.title)
            .setContentText(copy.privateBody)
            .setContentIntent(intent)
            .setFullScreenIntent(intent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicN)
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
