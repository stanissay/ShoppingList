package stanissay.shopping.list.utils

import stanissay.shopping.common.DeviceType

object DeviceInfo {
    lateinit var deviceType: DeviceType
        private set

    fun init() {
        deviceType = DeviceType.AndroidWear
    }
}