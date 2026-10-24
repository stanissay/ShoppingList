package stanissay.shopping.list.utils

import stanissay.shopping.shared.DeviceType

object ConfigW {
    const val APP_SETTINGS = "app_settings"
    const val NOTIFICATIONS_PERMISSION_ASKED = "notification_permission_asked"
    const val OUTGOING_CHANNEL_ID = "shopping_activity"
    const val STOP_AT = "stop_at"
    const val SHOPPING_ACTIVITY = "Shopping activity"
    const val OUTGOING_NOTIFICATION_ID = 1001
    const val STOP_DELAY = 5 * 60 * 1000L           // 5 minutes
}

object DeviceInfo {
    lateinit var deviceType: DeviceType
        private set

    fun init() {
        deviceType = DeviceType.AndroidWear
    }
}