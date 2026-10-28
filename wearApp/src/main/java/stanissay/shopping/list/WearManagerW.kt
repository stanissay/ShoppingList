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

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import stanissay.shopping.shared.ListSnapshot
import stanissay.shopping.shared.R
import stanissay.shopping.shared.WearRoutes
import kotlin.time.Duration.Companion.milliseconds

class MessageServiceW : WearableListenerService() {
    private val wearManager by lazy { WearManagerW(application) }
    private val serviceScope
        get() = (application as MyApplication).appScope

    @SuppressLint("WearRecents")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            WearRoutes.VERSION -> {
                serviceScope.launch {
                    val currentVersion = application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: applicationContext.getString(
                        R.string.unknown_version)
                    wearManager.sendMessageToPhone(currentVersion, WearRoutes.VERSION)
                }
            }
            else -> {}
        }
    }
}

class WearManagerW(private val context: Context) {
    suspend fun sendMessageToPhone(message: String, route: String): Boolean {
        return try {
            withTimeout(5000.milliseconds) {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) return@withTimeout false
                val messageClient = Wearable.getMessageClient(context)

                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, route, message.toByteArray()).await()
                }

                true
            }
        } catch (_: TimeoutCancellationException) { false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

class WearDataListener : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.dataItem.uri.path != WearRoutes.SNAPSHOT) continue
            if(Firebase.auth.currentUser == null) return
            when (event.type) {
                DataEvent.TYPE_CHANGED -> {
                    val json = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("json") ?: continue
                    val snap = runCatching { Json.decodeFromString(ListSnapshot.serializer(), json) }.getOrNull() ?: continue
                    updateTileSnapshot(applicationContext, snap)
                }
                DataEvent.TYPE_DELETED -> updateTileSnapshot(applicationContext, ListSnapshot())
            }
        }
    }
}