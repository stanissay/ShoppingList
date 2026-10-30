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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import stanissay.shopping.list.WidgetStore.ADD_KEY
import stanissay.shopping.list.WidgetStore.CATEGORY_KEY
import stanissay.shopping.list.WidgetStore.DIR_KEY
import stanissay.shopping.list.WidgetStore.ITEM_KEY
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.WidgetColorStyle
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.ListSnapshot
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.R
import stanissay.shopping.shared.SharedSyncService
import stanissay.shopping.shared.ShoppingItem
import stanissay.shopping.shared.displayLabel
import kotlin.time.Duration.Companion.milliseconds

object WidgetStore {
    private val json = Json { ignoreUnknownKeys = true }
    private fun prefs(context: Context) = context.getSharedPreferences(DataKeys.WIDGET_PREFS, Context.MODE_PRIVATE)
    val DIR_KEY = ActionParameters.Key<Int>("dir")
    val ITEM_KEY = ActionParameters.Key<String>("item")
    val CATEGORY_KEY = ActionParameters.Key<String>(DataKeys.CATEGORY_ID)
    val ADD_KEY = ActionParameters.Key<Boolean>(DataKeys.ADD_ITEM)

    fun save(context: Context, snapshot: ListSnapshot) = prefs(context).edit {
        putString(DataKeys.SNAPSHOT, json.encodeToString(ListSnapshot.serializer(), snapshot))
    }

    fun load(context: Context): ListSnapshot =
        prefs(context).getString(DataKeys.SNAPSHOT, null)
            ?.let { runCatching { json.decodeFromString(ListSnapshot.serializer(), it) }.getOrNull() } ?: ListSnapshot()

    fun remove(context: Context, id: String) {
        val s = load(context)
        save(context, s.copy(items = s.items.filter { it.id != id }))
    }

    fun clear(context: Context) = prefs(context).edit { clear() }
}

class ShoppingWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(colors = WidgetColorStyle) {
                val prefs = currentState<Preferences>()
                val version = prefs[DataKeys.SNAPSHOT_VERSION_KEY]
                val snapshot = remember(version) { WidgetStore.load(context) }
                val cats = snapshot.sortedCategories()
                val selected = cats.firstOrNull { it.id == prefs[DataKeys.SELECTED_CATEGORY_KEY] } ?: cats.firstOrNull()
                WidgetContent(context, selected?.name.orEmpty(), selected?.id, snapshot.itemsFor(selected?.id))
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, title: String, id: String?, items: List<ShoppingItem>) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(MainConstants.SPACE)
    ) {
        if(Firebase.auth.currentUser != null) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.size(Constants.BUTTON_SIZE))
                Image(
                    ImageProvider(R.drawable.arrow_left),
                    contentDescription = context.getString(R.string.back),
                    modifier = GlanceModifier.size(Constants.BUTTON_SIZE)
                        .clickable(
                            actionRunCallback<ChangeCategoryAction>(
                                actionParametersOf(
                                    DIR_KEY to -1
                                )
                            )
                        ),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary)
                )
                Box(
                    modifier = GlanceModifier.defaultWeight().padding(MainConstants.SPACE).clickable(
                        if (id != null)
                            actionStartActivity<MainActivity>(actionParametersOf(CATEGORY_KEY to id))
                        else actionStartActivity<MainActivity>()
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = Constants.WIDGET_TITLE,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Image(
                    ImageProvider(R.drawable.arrow_right),
                    contentDescription = context.getString(R.string.forward),
                    modifier = GlanceModifier.size(Constants.BUTTON_SIZE)
                        .clickable(
                            actionRunCallback<ChangeCategoryAction>(
                                actionParametersOf(
                                    DIR_KEY to 1
                                )
                            )
                        ),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary)
                )
                Image(
                    ImageProvider(R.drawable.plus_btn),
                    contentDescription = context.getString(R.string.add_item),
                    modifier = GlanceModifier.size(Constants.BUTTON_SIZE)
                        .clickable(
                            if (id != null)
                                actionStartActivity<MainActivity>(actionParametersOf(CATEGORY_KEY to id, ADD_KEY to true))
                            else actionStartActivity<MainActivity>(actionParametersOf(ADD_KEY to true))
                        ),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary)
                )
            }
            if (items.isNotEmpty()) {
                LazyColumn {
                    items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                        CheckBox(
                            checked = false,
                            onCheckedChange = actionRunCallback<ToggleItemAction>(
                                actionParametersOf(ITEM_KEY to item.id)
                            ),
                            text = item.displayLabel(),
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = Constants.WIDGET_TEXT
                            )
                        )
                    }
                }
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(
                        if (id != null)
                            actionStartActivity<MainActivity>(actionParametersOf(CATEGORY_KEY to id))
                        else actionStartActivity<MainActivity>()
                    ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = context.getString(R.string.all_items_purchased),
                        style = TextStyle(
                            color = GlanceTheme.colors.onBackground,
                            fontSize = Constants.WIDGET_TITLE,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        } else {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(
                    if (id != null)
                        actionStartActivity<MainActivity>(actionParametersOf(CATEGORY_KEY to id))
                    else actionStartActivity<MainActivity>()
                ),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = context.getString(R.string.log_in_to_use_widget),
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = Constants.WIDGET_TITLE,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

class ShoppingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ShoppingWidget()
}

class ChangeCategoryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val dir = parameters[DIR_KEY] ?: return
        val snapshot = WidgetStore.load(context)
        updateAppWidgetState(context, glanceId) { prefs ->
            snapshot.cycle(prefs[DataKeys.SELECTED_CATEGORY_KEY], dir)?.let {
                prefs[DataKeys.SELECTED_CATEGORY_KEY] = it
            }
        }
        ShoppingWidget().update(context, glanceId)
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ITEM_KEY] ?: return
        WidgetStore.remove(context, id)
        refreshWidgets(context)
        withTimeoutOrNull(5000.milliseconds) {
            SharedSyncService().markDone(id)
        }
    }
}

suspend fun refreshWidgets(context: Context) {
    val ids = GlanceAppWidgetManager(context).getGlanceIds(ShoppingWidget::class.java)
    ids.forEach { id ->
        updateAppWidgetState(context, id) { prefs ->
            prefs[DataKeys.SNAPSHOT_VERSION_KEY] = (prefs[DataKeys.SNAPSHOT_VERSION_KEY] ?: 0L) + 1
        }
    }
    ShoppingWidget().updateAll(context)
}

suspend fun clearWidgets(context: Context) {
    WidgetStore.clear(context)
    GlanceAppWidgetManager(context).getGlanceIds(ShoppingWidget::class.java).forEach { id ->
        updateAppWidgetState(context, id) { prefs ->
            prefs.remove(DataKeys.SELECTED_CATEGORY_KEY)
            prefs[DataKeys.SNAPSHOT_VERSION_KEY] = (prefs[DataKeys.SNAPSHOT_VERSION_KEY] ?: 0L) + 1
        }
    }
    ShoppingWidget().updateAll(context)
}