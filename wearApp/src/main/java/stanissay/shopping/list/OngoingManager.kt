package stanissay.shopping.list

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import stanissay.shopping.list.utils.ConfigW
import stanissay.shopping.shared.R

class OngoingManager(private val context: Context) {
    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var stopRunnable: Runnable? = null
        private fun cancelScheduledStop() {
            stopRunnable?.let(handler::removeCallbacks)
            stopRunnable = null
        }
    }

    fun start() {
        cancelScheduledStop()

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ConfigW.OUTGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bag)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.shopping))
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        val ongoingActivity = OngoingActivity.Builder(context, ConfigW.OUTGOING_NOTIFICATION_ID, notification)
            .setStaticIcon(R.drawable.ic_bag).setTouchIntent(pendingIntent).build()

        ongoingActivity.apply(context)

        NotificationManagerCompat.from(context).notify(ConfigW.OUTGOING_NOTIFICATION_ID, notification.build())
    }

    fun stop() {
        cancelScheduledStop()

        stopRunnable = Runnable {
            NotificationManagerCompat.from(context).cancel(ConfigW.OUTGOING_NOTIFICATION_ID)
            stopRunnable = null
        }
        handler.postDelayed(stopRunnable!!, ConfigW.STOP_DELAY)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(ConfigW.OUTGOING_CHANNEL_ID, ConfigW.SHOPPING_ACTIVITY, NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.current_list)
        }

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}