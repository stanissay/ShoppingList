package stanissay.shopping.list.utils

import android.content.Context
import android.content.res.Configuration
import stanissay.shopping.shared.DeviceType

object DeviceInfo {
    lateinit var deviceType: DeviceType
        private set

    val isTablet: Boolean
        get() = deviceType == DeviceType.AndroidTablet

    fun init(context: Context) {
        deviceType = if (checkIfTablet(context)) DeviceType.AndroidTablet else DeviceType.AndroidPhone
    }

    private fun checkIfTablet(context: Context): Boolean {
        val screenLayout = context.resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}