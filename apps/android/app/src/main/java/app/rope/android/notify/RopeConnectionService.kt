package app.rope.android.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.rope.android.MainActivity
import app.rope.android.RopeApp

/**
 * Sticky foreground keep-alive so the signed-in WSS is not killed in the background.
 * No FCM. Does not put plaintext on any new wire.
 */
class RopeConnectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!promoteForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        (application as? RopeApp)?.repo?.start(null)
        return START_STICKY
    }

    private fun promoteForeground(): Boolean {
        ensureChannel()
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Rope · на связи")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(launch)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            val both = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            if (runCatching { startForeground(ID, notification, both) }.isSuccess) return true
            if (runCatching {
                    startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                }.isSuccess
            ) {
                return true
            }
        } else if (Build.VERSION.SDK_INT >= 29) {
            if (runCatching {
                    startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                }.isSuccess
            ) {
                return true
            }
        }
        return runCatching { startForeground(ID, notification) }.isSuccess
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        val existing = mgr.getNotificationChannel(CHANNEL)
        if (existing != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL, "Связь", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
    }

    companion object {
        private const val CHANNEL = "rope-connection"
        private const val ID = 7100

        fun start(context: Context) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, RopeConnectionService::class.java)
            runCatching {
                ContextCompat.startForegroundService(appCtx, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, RopeConnectionService::class.java),
                )
            }
        }
    }
}
