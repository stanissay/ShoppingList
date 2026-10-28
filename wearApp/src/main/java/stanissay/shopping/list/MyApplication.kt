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

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import stanissay.shopping.list.utils.DeviceInfo
import stanissay.shopping.shared.ServerClock

class MyApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        DeviceInfo.init()
        OngoingManager.cancelIfExpired(applicationContext)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        ServerClock.start()
        appScope.launch {
            val fresh = readSnapshotFromDataLayer(this@MyApplication) ?: return@launch
            if (fresh != TileStore(this@MyApplication).snapshot()) {
                updateTileSnapshot(this@MyApplication, fresh)
            }
        }
    }
}