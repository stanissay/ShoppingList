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