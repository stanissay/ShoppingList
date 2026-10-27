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
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

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