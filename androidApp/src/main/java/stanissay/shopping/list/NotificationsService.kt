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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ServerClock
import stanissay.shopping.shared.ShoppingItem
import kotlin.time.Duration.Companion.milliseconds

class NotificationsService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var debounceJob: Job? = null
    private var listener: ChildEventListener? = null
    private var query: Query? = null
    private var myId: String? = null
    private var startFrom = 0L
    private var batchMax = 0L
    private var pending = 0
    private val seen = mutableMapOf<String, Long>()
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        when {
            uid == null -> { detachListener(); stopSelf() }
            uid != myId -> tryConnectNow()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == DataKeys.ACTION_CONNECT_NOW) {
            tryConnectNow()
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(DataKeys.HASH_ID, showFGSNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        Firebase.auth.addAuthStateListener(authListener)
        tryConnectNow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun tryConnectNow() {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) { stopSelf(); return }
        if (listener != null && uid == myId) return

        detachListener()
        myId = uid
        val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
        val lastSeen = prefs.getLong(DataKeys.LAST_SEEN, 0L)

        var from = lastSeen
        if (from == 0L) {
            from = ServerClock.now()
            prefs.edit {
                putLong(DataKeys.LAST_SEEN, from)
            }
        }
        startFrom = from
        seen.clear(); pending = 0; batchMax = 0

        val q = FirebaseDatabase.getInstance().reference.child("items")
            .orderByChild("activatedAt").startAt((from + 1).toDouble())

        val l = object : ChildEventListener {
            override fun onChildAdded(s: DataSnapshot, p: String?) = handle(s, uid)
            override fun onChildChanged(s: DataSnapshot, p: String?) = handle(s, uid)
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {
                if (listener !== this) return
                detachListener()
                if (e.code != DatabaseError.PERMISSION_DENIED) {
                    scope.launch {
                        delay(30000.milliseconds)
                        tryConnectNow()
                    }
                }
            }
        }
        q.addChildEventListener(l)
        query = q; listener = l
    }

    private fun handle(s: DataSnapshot, uid: String) {
        val item = s.getValue(ShoppingItem::class.java) ?: return
        val key = s.key ?: return
        val at = item.activatedAt ?: return
        if (at <= (seen[key] ?: startFrom)) return
        seen[key] = at
        if (item.activatedBy == uid) return
        batchMax = maxOf(batchMax, at)

        if (isAppInForeground()) {
            if (pending == 0) getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE).edit { putLong(DataKeys.LAST_SEEN, batchMax) }
            return
        }

        pending++
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(60000.milliseconds)
            val n = pending; pending = 0
            if (n == 0) return@launch
            val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
            if (!isAppInForeground()) showAlert(n)
            prefs.edit { putLong(DataKeys.LAST_SEEN, batchMax) }
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
            description = this@NotificationsService.getString(R.string.background_service_message)
        }

        val updatesChannel = NotificationChannel(
            DataKeys.NEW_ITEMS_CHANNEL,
            this.getString(R.string.new_items),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = this@NotificationsService.getString(R.string.new_items_message)
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

    private fun isAppInForeground() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun detachListener() {
        listener?.let { query?.removeEventListener(it) }
        listener = null; query = null
    }

    override fun onDestroy() {
        Firebase.auth.removeAuthStateListener(authListener)
        detachListener()
        scope.cancel()
        super.onDestroy()
    }
}