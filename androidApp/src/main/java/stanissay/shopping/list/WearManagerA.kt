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

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import stanissay.shopping.shared.Config
import stanissay.shopping.shared.WearRoutes
import kotlin.time.Duration.Companion.milliseconds

class MessageServiceA : WearableListenerService() {
    private val wearManager by lazy { WearManagerA(application) }
    private val serviceScope
        get() = (application as MyApplication).appScope

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            WearRoutes.VERSION -> {
                val currentVersion = String(messageEvent.data)
                Config.wearVersion = currentVersion
            }
            else -> {}
        }
    }
}

class WearManagerA(private val context: Context) {
    suspend fun sendMessageToWatch(message: String, route: String): Boolean {
        return try {
            withTimeout(5000.milliseconds) {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = nodeClient.connectedNodes.await()

                if (nodes.isEmpty()) { return@withTimeout false }

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

    suspend fun putSnapshot(json: String) {
        val request = PutDataMapRequest.create(WearRoutes.SNAPSHOT).apply {
            dataMap.putString("json", json)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request).await()
    }
}