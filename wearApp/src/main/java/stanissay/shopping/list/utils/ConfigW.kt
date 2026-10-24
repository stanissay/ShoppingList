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

import stanissay.shopping.shared.DeviceType

object ConfigW {
    const val APP_SETTINGS = "app_settings"
    const val NOTIFICATIONS_PERMISSION_ASKED = "notification_permission_asked"
    const val OUTGOING_CHANNEL_ID = "shopping_activity"
    const val STOP_AT = "stop_at"
    const val SHOPPING_ACTIVITY = "Shopping activity"
    const val OUTGOING_NOTIFICATION_ID = 1001
    const val STOP_DELAY = 3 * 60 * 1000L           // 3 minutes
}

object DeviceInfo {
    lateinit var deviceType: DeviceType
        private set

    fun init() {
        deviceType = DeviceType.AndroidWear
    }
}