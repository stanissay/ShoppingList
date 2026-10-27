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
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import stanissay.shopping.list.utils.isAppInForeground
import stanissay.shopping.list.utils.watchHasApp
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ServerClock
import stanissay.shopping.shared.SharedSyncService
import stanissay.shopping.shared.SyncCancelledException
import stanissay.shopping.shared.WearRoutes
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SyncService : Service() {
    private val service = SharedSyncService()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var authJob: Job? = null
    private var syncJob: Job? = null
    private var notificationsJob: Job? = null
    private var widgetJob: Job? = null
    private var watchJob: Job? = null
    private var myId: String? = null
    private var batchMax = 0L
    private var pending = 0
    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener {
        Firebase.auth.currentUser?.uid?.let(::syncFeatures)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == DataKeys.ACTION_CONNECT_NOW) {
            Firebase.auth.currentUser?.uid?.let { syncFeatures(it) }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(DataKeys.HASH_ID, showFGSNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        Wearable.getCapabilityClient(this).addListener(capabilityListener, WearRoutes.CAPABILITY)
        authJob = scope.launch {
            service.userId().collect { uid ->
                if (uid == null) { detachAll(); stopSelf() }
                else syncFeatures(uid)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun syncFeatures(uid: String) {
        syncJob?.cancel()
        syncJob = scope.launch {
            val wantNotifications = NotificationManagerCompat.from(this@SyncService).areNotificationsEnabled()
            val wantWidget = GlanceAppWidgetManager(this@SyncService)
                .getGlanceIds(ShoppingWidget::class.java).isNotEmpty()
            val wantWatch = applicationContext.watchHasApp()

            if (!wantNotifications && !wantWidget && !wantWatch) {
                detachAll(); stopSelf(); return@launch
            }
            if (wantNotifications) startNotifications(uid) else stopNotifications()
            if (wantWidget) startWidget() else stopWidget()
            if (wantWatch) startWatch() else stopWatch()
        }
    }

    private fun startWidget() {
        if (widgetJob?.isActive == true) return
        widgetJob = scope.launch {
            snapshotFlow.collect { snap ->
                WidgetStore.save(applicationContext, snap)
                refreshWidgets(applicationContext)
            }
        }
    }

    private fun stopWidget() {
        widgetJob?.cancel()
        widgetJob = null
    }

    @OptIn(FlowPreview::class)
    private fun startNotifications(uid: String) {
        if (notificationsJob?.isActive == true && uid == myId) return
        notificationsJob?.cancel()
        myId = uid
        notificationsJob = scope.launch {
            flow {
                pending = 0; batchMax = 0
                emitAll(service.activations(loadLastSeen(), uid))
            }
                .retryUnlessDenied()
                .catch { it.printStackTrace() }
                .filter { at ->
                    batchMax = maxOf(batchMax, at)
                    if (isAppInForeground()) {
                        if (pending == 0) saveLastSeen(batchMax)
                        false
                    } else {
                        pending++
                        true
                    }
                }
                .debounce(60.seconds)
                .collect {
                    val n = pending; pending = 0
                    if (n > 0 && !isAppInForeground()) showAlert(n)
                    saveLastSeen(batchMax)
                }
        }
    }

    private fun stopNotifications() {
        notificationsJob?.cancel()
        notificationsJob = null
        pending = 0
    }

    private fun startWatch() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            snapshotFlow
                .distinctUntilChanged()
                .conflate()
                .collect { snap ->
                    withContext(Dispatchers.IO) {
                        WearManagerA(applicationContext).sendMessageToWatch(Json.encodeToString(snap), WearRoutes.UPDATE)
                    }
                }
        }
    }

    private fun stopWatch() { watchJob?.cancel(); watchJob = null }

    private val snapshotFlow by lazy {
        service.snapshots()
            .retryWhen { cause, _ ->
                if ((cause as? SyncCancelledException)?.isPermissionDenied == true) false
                else { delay(30000.milliseconds); true }
            }
            .catch { it.printStackTrace() }
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)
    }

    private fun loadLastSeen(): Long {
        val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
        var from = prefs.getLong(DataKeys.LAST_SEEN, 0L)
        if (from == 0L) {
            from = ServerClock.now()
            prefs.edit { putLong(DataKeys.LAST_SEEN, from) }
        }
        return from
    }

    private fun saveLastSeen(value: Long) {
        val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
        if (value > prefs.getLong(DataKeys.LAST_SEEN, 0L)) {
            prefs.edit { putLong(DataKeys.LAST_SEEN, value) }
        }
    }

    private fun showFGSNotification(): Notification {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_CHANNEL_ID, DataKeys.FGS_CHANNEL)
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, DataKeys.FGS_CHANNEL)
            .setContentTitle(this.getString(R.string.background_service))
            .setContentText(this.getString(R.string.background_service_message))
            .setSmallIcon(R.drawable.ic_bag)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val foregroundChannel = NotificationChannel(
            DataKeys.FGS_CHANNEL,
            this.getString(R.string.background_service),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = this@SyncService.getString(R.string.background_service_message)
        }

        val updatesChannel = NotificationChannel(
            DataKeys.NEW_ITEMS_CHANNEL,
            this.getString(R.string.new_items),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = this@SyncService.getString(R.string.new_items_message)
        }

        notificationManager.createNotificationChannel(foregroundChannel)
        notificationManager.createNotificationChannel(updatesChannel)
    }

    private fun showAlert(count: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = NotificationCompat.Builder(this, DataKeys.NEW_ITEMS_CHANNEL)
            .setSmallIcon(R.drawable.ic_bag)
            .setContentTitle(getString(R.string.new_items))
            .setContentText(resources.getQuantityString(R.plurals.new_items_added, count, count))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(DataKeys.NEW_ITEMS_NOTIFICATION_ID, n)
    }

    private fun <T> Flow<T>.retryUnlessDenied() = retryWhen { cause, _ ->
        if ((cause as? SyncCancelledException)?.isPermissionDenied == true) false
        else { delay(30000.milliseconds); true }
    }

    private fun detachAll() {
        stopNotifications()
        stopWidget()
        stopWatch()
    }

    override fun onDestroy() {
        Wearable.getCapabilityClient(this).removeListener(capabilityListener)
        scope.cancel()
        super.onDestroy()
    }
}