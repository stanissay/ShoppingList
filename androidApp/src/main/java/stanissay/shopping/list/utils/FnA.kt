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

package stanissay.shopping.list.utils

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import stanissay.shopping.list.MainViewModelA
import stanissay.shopping.list.SyncService
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.StringProvider
import stanissay.shopping.shared.WearRoutes

class MainViewModelFactory(
    private val app: Application,
    private val strings: StringProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(MainViewModelA::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModelA(app, strings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (Firebase.auth.currentUser == null) return

        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SyncService::class.java).setAction(DataKeys.ACTION_CONNECT_NOW)
            )
        }
    }
}

fun isAppInForeground() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

suspend fun Context.watchHasApp(): Boolean {
    val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
    val result = runCatching {
        Wearable.getCapabilityClient(this)
            .getCapability(WearRoutes.CAPABILITY, CapabilityClient.FILTER_ALL)
            .await().nodes.isNotEmpty()
    }.getOrNull()
    if (result != null) prefs.edit { putBoolean(DataKeys.WATCH_HAS_APP, result) }
    return result ?: prefs.getBoolean(DataKeys.WATCH_HAS_APP, false)
}