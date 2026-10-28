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

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ShoppingCategory

fun updateShortcuts(context: Context, categories: List<ShoppingCategory>) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)

    val shortcuts = buildList {
        addAll(
            categories.mapIndexed { index, category ->
                ShortcutInfo.Builder(context, "categories_$index")
                    .setShortLabel(category.name)
                    .setLongLabel(category.name)
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher))
                    .setIntent(
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(DataKeys.CATEGORY_ID, category.id)
                        }
                    )
                    .build()
            }
        )
    }

    shortcutManager?.dynamicShortcuts = shortcuts
}

fun clearShortcuts(context: Context) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    shortcutManager?.removeAllDynamicShortcuts()
}