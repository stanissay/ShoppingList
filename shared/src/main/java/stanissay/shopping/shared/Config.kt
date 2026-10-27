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

package stanissay.shopping.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlin.math.absoluteValue

enum class DeviceType { AndroidPhone, AndroidTablet, AndroidWear }

object Config {
    var wearVersion: String? by mutableStateOf(null)
}

object DataKeys {
    const val APP_SETTINGS = "app_settings"
    const val WIDGET_PREFS = "widget_prefs"
    const val TILE_PREFS = "tile_prefs"
    const val NOTIFICATIONS_PERMISSION_ASKED = "notifications_permission_asked"
    const val BATTERY_EXEMPTION_ASKED = "battery_exemption_asked"
    const val LAST_SEEN = "last_seen"
    const val ACTION_CONNECT_NOW = "CONNECT_NOW"
    const val OUTGOING_CHANNEL_ID = "shopping_activity"
    const val STOP_AT = "stop_at"
    const val SNAPSHOT = "snapshot"
    const val WATCH_HAS_APP = "watch_has_app"
    const val SHOPPING_ACTIVITY = "Shopping activity"
    const val CATEGORY_ID = "category_id"
    val HASH_ID = "StanissayShoppingList".hashCode().absoluteValue
    const val SNAPSHOT_VERSION = "snapshot_version"
    const val SELECTED_CATEGORY = "selected_category"
    val SELECTED_CATEGORY_KEY = stringPreferencesKey(SELECTED_CATEGORY)
    val SNAPSHOT_VERSION_KEY = longPreferencesKey(SNAPSHOT_VERSION)
    const val FGS_CHANNEL = "foreground_channel"
    const val NEW_ITEMS_CHANNEL = "new_items_notification_channel"
    const val OUTGOING_NOTIFICATION_ID = 1001
    const val NEW_ITEMS_NOTIFICATION_ID = 1002
    const val STOP_DELAY = 3 * 60 * 1000L           // 3 minutes
    const val RESOURCES_VERSION = "1"
    const val ID_PREV = "prev"
    const val ID_NEXT = "next"
    const val ID_DONE = "done"
    const val ICON_LEFT = "arrow_left"
    const val ICON_RIGHT = "arrow_right"
}

object WearRoutes {
    const val VERSION = "/version"
    const val UPDATE = "/update"
    const val CAPABILITY = "shopping_watch"
}

object ServerClock {
    @Volatile private var offset = 0L

    fun start() {
        Firebase.database.getReference(".info/serverTimeOffset")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    offset = s.getValue(Long::class.java) ?: 0L
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    fun now() = System.currentTimeMillis() + offset
}