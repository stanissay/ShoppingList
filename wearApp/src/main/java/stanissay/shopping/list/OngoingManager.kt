/*
 * Shopping List For Android and Wear Os
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.shopping.list

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.wear.ongoing.OngoingActivity
import stanissay.shopping.list.utils.ConfigW
import stanissay.shopping.list.utils.StopReceiver
import stanissay.shopping.shared.R

class OngoingManager(context: Context) {
    private val context = context.applicationContext

    companion object {
        private fun prefs(c: Context) =
            c.getSharedPreferences(ConfigW.APP_SETTINGS, Context.MODE_PRIVATE)

        fun cancelNotification(c: Context) {
            NotificationManagerCompat.from(c).cancel(ConfigW.OUTGOING_NOTIFICATION_ID)
            prefs(c).edit { remove(ConfigW.STOP_AT) }
        }

        fun cancelIfExpired(c: Context) {
            val stopAt = prefs(c).getLong(ConfigW.STOP_AT, 0L)
            if (stopAt != 0L && System.currentTimeMillis() >= stopAt) cancelNotification(c)
        }
    }

    @SuppressLint("WearRecents")
    fun start() {
        cancelScheduledStop()

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(ConfigW.OUTGOING_CHANNEL_ID, ConfigW.SHOPPING_ACTIVITY, NotificationManager.IMPORTANCE_LOW
        ).apply { description = context.getString(R.string.current_list) }

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun stop() {
        cancelScheduledStop()
        prefs(context).edit {
            putLong(ConfigW.STOP_AT, System.currentTimeMillis() + ConfigW.STOP_DELAY)
        }
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + ConfigW.STOP_DELAY,
            stopPendingIntent()!!
        )
    }

    fun cancelScheduledStop() {
        prefs(context).edit { remove(ConfigW.STOP_AT) }
        stopPendingIntent(create = false)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    private fun stopPendingIntent(create: Boolean = true): PendingIntent? = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, StopReceiver::class.java),
        (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or PendingIntent.FLAG_IMMUTABLE
    )
}