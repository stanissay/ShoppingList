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
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import stanissay.shopping.shared.ListSnapshot
import stanissay.shopping.shared.R
import stanissay.shopping.shared.WearRoutes

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
                    val currentVersion = application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: applicationContext.getString(R.string.unknown_version)
                    wearManager.sendMessageToPhone(currentVersion, WearRoutes.VERSION)
                }
            }
            WearRoutes.UPDATE -> {
                if (FirebaseAuth.getInstance().currentUser == null) return
                val json = String(messageEvent.data)
                val snapshot = Json.decodeFromString<ListSnapshot>(json)
                updateTileSnapshot(applicationContext, snapshot)
            }
            else -> {}
        }
    }
}