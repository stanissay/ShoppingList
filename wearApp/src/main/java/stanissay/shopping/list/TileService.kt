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

@file:Suppress("DEPRECATION")

package stanissay.shopping.list

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import stanissay.shopping.list.utils.ColorStyle
import stanissay.shopping.list.utils.tileColorScheme
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.ListSnapshot
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ShoppingItem
import stanissay.shopping.shared.WearRoutes
import stanissay.shopping.shared.displayLabel
import kotlin.time.Duration.Companion.milliseconds

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
}

class ShoppingTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest) = scope.future {
        val store = TileStore(this@ShoppingTileService)
        val snapshot = store.snapshot()
        if (snapshot.items.isEmpty() && snapshot.categories.isEmpty()) {
            withTimeoutOrNull(2000.milliseconds) { readSnapshotFromDataLayer(this@ShoppingTileService) }?.let { store.save(it) }
        }
        handleClick(requestParams.currentState.lastClickableId, store)
        val cats = snapshot.sortedCategories()
        val selected = cats.firstOrNull { it.id == store.selectedCategory } ?: cats.firstOrNull()
        val items = snapshot.itemsFor(selected?.id)

        val layout = shoppingTileLayout(
            context = this@ShoppingTileService,
            deviceParameters = requestParams.deviceConfiguration,
            categoryName = selected?.name.orEmpty(),
            prevCategoryId = snapshot.cycle(selected?.id, -1),
            nextCategoryId = snapshot.cycle(selected?.id, 1),
            items = items
        )
        TileBuilders.Tile.Builder()
            .setResourcesVersion(DataKeys.RESOURCES_VERSION)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

    private fun handleClick(id: String?, store: TileStore) {
        if (id != null && id.startsWith(DataKeys.CATEGORY_ID)) {
            store.selectedCategory = id.removePrefix(DataKeys.CATEGORY_ID)
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
    prevCategoryId: String?,
    nextCategoryId: String?,
    items: List<ShoppingItem>
) = materialScope(
    context = context,
    deviceConfiguration = deviceParameters,
    allowDynamicTheme = false,
    defaultColorScheme = tileColorScheme(ColorStyle)
) {
    val rows = if (deviceParameters.screenWidthDp >= MainConstants.TILE_LARGE_SCREEN_DP) 5 else 4
    val more = (items.size - rows).coerceAtLeast(0)

    primaryLayout(
        titleSlot = {
            text(
                categoryName.layoutString,
                typography = Typography.TITLE_SMALL,
                color = colorScheme.primary,
                maxLines = 1
            )
        },
        mainSlot = {
            LayoutElementBuilders.Row.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(arrow(DataKeys.ICON_LEFT, DataKeys.CATEGORY_ID + prevCategoryId.orEmpty(), colorScheme.secondary.prop))
                .addContent(
                    listBox(
                        backgroundColor = colorScheme.background.prop,
                        borderColor = colorScheme.outline.prop,
                        onClick = clickable(
                            id = DataKeys.ID_OPEN,
                            action = launchAction(ComponentName(context, MainActivity::class.java))
                        ),
                        content = LayoutElementBuilders.Column.Builder().apply {
                            setWidth(expand())
                            setHorizontalAlignment(
                                if (items.isEmpty()) LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
                                else LayoutElementBuilders.HORIZONTAL_ALIGN_START
                            )
                            if (items.isEmpty()) {
                                addContent(
                                    text(
                                        text = context.getString(R.string.all_items_purchased).layoutString,
                                        typography = Typography.BODY_SMALL,
                                        color = colorScheme.onBackground,
                                        maxLines = 2
                                    )
                                )
                            }
                            items.take(rows).forEach { item ->
                                addContent(
                                    text(
                                        text = item.displayLabel().layoutString,
                                        typography = Typography.BODY_SMALL,
                                        color = colorScheme.onBackground,
                                        maxLines = 1
                                    )
                                )
                            }
                            if (more > 0) {
                                addContent(
                                    text(
                                        text = "${R.string.more} $more".layoutString,
                                        typography = Typography.LABEL_SMALL,
                                        color = colorScheme.onSurface
                                    )
                                )
                            }
                        }.build()
                    )
                )
                .addContent(arrow(DataKeys.ICON_RIGHT, DataKeys.CATEGORY_ID + nextCategoryId.orEmpty(), colorScheme.secondary.prop))
                .build()
        }
    )
}

private fun listBox(
    backgroundColor: ColorBuilders.ColorProp,
    borderColor: ColorBuilders.ColorProp,
    onClick: ModifiersBuilders.Clickable,
    content: LayoutElementBuilders.LayoutElement
): LayoutElementBuilders.LayoutElement =
    LayoutElementBuilders.Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setBackground(
                    ModifiersBuilders.Background.Builder()
                        .setColor(backgroundColor)
                        .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(4f)).build())
                        .build()
                )
                .setBorder(
                    ModifiersBuilders.Border.Builder()
                        .setWidth(dp(1f))
                        .setColor(borderColor)
                        .build()
                )
                .setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(2f)).build())
                .setClickable(onClick)
                .build()
        )
        .addContent(content)
        .build()

private fun arrow(resId: String, clickId: String, tint: ColorBuilders.ColorProp): LayoutElementBuilders.LayoutElement =
    LayoutElementBuilders.Box.Builder()
        .setWidth(dp(32f)).setHeight(dp(32f))
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setClickable(
                    ModifiersBuilders.Clickable.Builder()
                        .setId(clickId)
                        .setOnClick(ActionBuilders.LoadAction.Builder().build())
                        .build()
                ).build()
        )
        .addContent(
            LayoutElementBuilders.Image.Builder()
                .setResourceId(resId)
                .setWidth(dp(16f)).setHeight(dp(16f))
                .setColorFilter(
                    LayoutElementBuilders.ColorFilter.Builder()
                        .setTint(tint)
                        .build()
                )
                .build()
        )
        .build()

fun updateTileSnapshot(context: Context, snapshot: ListSnapshot) {
    TileStore(context).save(snapshot)
    TileService.getUpdater(context).requestUpdate(ShoppingTileService::class.java)
}

suspend fun readSnapshotFromDataLayer(context: Context): ListSnapshot? = runCatching {
    val uri = Uri.Builder()
        .scheme(PutDataRequest.WEAR_URI_SCHEME)
        .authority("*")
        .path(WearRoutes.SNAPSHOT)
        .build()
    val buffer = Wearable.getDataClient(context).getDataItems(uri).await()
    try {
        buffer.firstOrNull()
            ?.let { DataMapItem.fromDataItem(it).dataMap.getString("json") }
            ?.let { Json.decodeFromString(ListSnapshot.serializer(), it) }
    } finally {
        buffer.release()
    }
}.getOrNull()