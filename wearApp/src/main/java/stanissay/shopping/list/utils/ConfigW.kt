package stanissay.shopping.list.utils

import stanissay.shopping.shared.DeviceType

object DeviceInfo {
    lateinit var deviceType: DeviceType
        private set

    fun init() {
        deviceType = DeviceType.AndroidWear
    }
}