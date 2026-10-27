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

import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ButtonGroupDefaults.DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.compactButton
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.serialization.json.Json
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.ListSnapshot
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.R
import stanissay.shopping.shared.SharedSyncService
import stanissay.shopping.shared.ShoppingItem

class TileStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(DataKeys.TILE_PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun snapshot(): ListSnapshot = sp.getString(DataKeys.SNAPSHOT, null)
        ?.let { runCatching { json.decodeFromString(ListSnapshot.serializer(), it) }.getOrNull() }
        ?: ListSnapshot()

    fun save(s: ListSnapshot) = sp.edit {
        putString(DataKeys.SNAPSHOT, json.encodeToString(ListSnapshot.serializer(), s))
    }

    var selectedCategory: String?
        get() = sp.getString(DataKeys.SELECTED_CATEGORY, null)
        set(v) = sp.edit { putString(DataKeys.SELECTED_CATEGORY, v) }

    fun remove(id: String) {
        val s = snapshot()
        save(s.copy(items = s.items.filter { it.id != id }))
    }
}

class ShoppingTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest) = scope.future {
        val store = TileStore(this@ShoppingTileService)
        handleClick(requestParams.currentState.lastClickableId, store)

        val snapshot = store.snapshot()
        val cats = snapshot.sortedCategories()
        val selected = cats.firstOrNull { it.id == store.selectedCategory } ?: cats.firstOrNull()
        val items = snapshot.itemsFor(selected?.id)

        val layout = shoppingTileLayout(
            context = this@ShoppingTileService,
            deviceParameters = requestParams.deviceConfiguration,
            categoryName = selected?.name.orEmpty(),
            items = items
        )
        TileBuilders.Tile.Builder()
            .setResourcesVersion(DataKeys.RESOURCES_VERSION)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

    private fun handleClick(id: String?, store: TileStore) {
        when {
            id.isNullOrEmpty() -> Unit
            id == DataKeys.ID_PREV || id == DataKeys.ID_NEXT -> {
                val dir = if (id == DataKeys.ID_NEXT) 1 else -1
                store.selectedCategory = store.snapshot().cycle(store.selectedCategory, dir)
            }
            id.startsWith(DataKeys.ID_DONE) -> {
                val itemId = id.removePrefix(DataKeys.ID_DONE)
                store.remove(itemId)
                WorkManager.getInstance(this).enqueueUniqueWork(
                    "done_$itemId", ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<MarkDoneWorker>()
                        .setInputData(workDataOf("id" to itemId))
                        .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                )
            }
        }
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest) = scope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion(DataKeys.RESOURCES_VERSION)
            .addIdToImageMapping(DataKeys.ICON_LEFT, androidImage(R.drawable.arrow_left))
            .addIdToImageMapping(DataKeys.ICON_RIGHT, androidImage(R.drawable.arrow_right))
            .build()
    }

    private fun androidImage(resId: Int) = ResourceBuilders.ImageResource.Builder()
        .setAndroidResourceByResId(
            ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(resId).build()
        ).build()

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

@Suppress("DEPRECATION")
fun shoppingTileLayout(
    context: Context,
    deviceParameters: DeviceParametersBuilders.DeviceParameters,
    categoryName: String,
    items: List<ShoppingItem>
) = materialScope(context, deviceParameters, allowDynamicTheme = false) {
    val rows = if (deviceParameters.screenWidthDp >= MainConstants.TILE_LARGE_SCREEN_DP) 3 else 2
    primaryLayout(
        titleSlot = { text(categoryName.layoutString) },
        mainSlot = {
            LayoutElementBuilders.Column.Builder().apply {
                setWidth(expand()); setHeight(expand())
                addContent(
                    buttonGroup(
                        width = wrap(),
                        height = wrap()
                    ) {
                        buttonGroupItem {
                            compactButton(
                                onClick = clickable(id = DataKeys.ID_PREV, action = loadAction()),
                                iconContent = { icon(DataKeys.ICON_LEFT) }
                            )
                        }
                        buttonGroupItem {
                            compactButton(
                                onClick = clickable(id = DataKeys.ID_NEXT, action = loadAction()),
                                iconContent = { icon(DataKeys.ICON_RIGHT) }
                            )
                        }
                    }
                )
                if (items.isEmpty()) {
                    addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                    addContent(text(context.getString(R.string.all_items_purchased).layoutString))
                }
                items.take(rows).forEach { item ->
                    addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                    addContent(
                        textButton(
                            onClick = clickable(
                                id = DataKeys.ID_DONE + item.id,
                                action = loadAction()
                            ),
                            labelContent = {
                                text(listOfNotNull(item.name, item.quantity).joinToString(" · ").layoutString)
                            },
                            width = expand()
                        )
                    )
                }
            }.build()
        },
        bottomSlot = {
            textEdgeButton(
                onClick = clickable(
                    id = "open",
                    action = launchAction(
                        ComponentName(context, MainActivity::class.java)
                    )
                ),
                labelContent = {
                    val more = (items.size - rows).coerceAtLeast(0)
                    text((if (more > 0) "${context.getString(R.string.more)} $more" else context.getString(R.string.open)).layoutString)
                }
            )
        }
    )
}

fun updateTileSnapshot(context: Context, snapshot: ListSnapshot) {
    TileStore(context).save(snapshot)
    TileService.getUpdater(context).requestUpdate(ShoppingTileService::class.java)
}

class MarkDoneWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.failure()
        return try {
            SharedSyncService().markDone(id)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}