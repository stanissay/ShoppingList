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

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.ListSnapshot
import stanissay.shopping.shared.R

object TileStore {
    private fun prefs(context: Context) = context.getSharedPreferences(DataKeys.TILE_PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, snapshot: ListSnapshot) = prefs(context).edit {
        putInt(DataKeys.TILE_COUNT, snapshot.items.count { !it.done })
    }

    fun count(context: Context): Int = prefs(context)
        .getInt(DataKeys.TILE_COUNT, 0)

    fun clear(context: Context) = prefs(context).edit { clear() }
}

class TileServiceA : TileService() {
    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        startActivityAndCollapse(pendingIntent)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val count = TileStore.count(this)

        tile.label = getString(R.string.app_name)
        tile.state = Tile.STATE_INACTIVE
        tile.subtitle = count.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.items, it, it) }

        tile.updateTile()
    }
}

fun clearTile(context: Context) {
    TileStore.clear(context)

    TileService.requestListeningState(
        context,
        ComponentName(context, TileServiceA::class.java)
    )
}