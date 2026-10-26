package stanissay.shopping.list

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import stanissay.shopping.list.WidgetStore.DIR_KEY
import stanissay.shopping.list.WidgetStore.ITEM_KEY
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.WidgetColorStyle
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ShoppingCategory
import stanissay.shopping.shared.ShoppingItem
import kotlin.time.Duration.Companion.milliseconds

object WidgetStore {
    private val json = Json { ignoreUnknownKeys = true }
    private fun prefs(context: Context) = context.getSharedPreferences(DataKeys.WIDGET_PREFS, Context.MODE_PRIVATE)
    val DIR_KEY = ActionParameters.Key<Int>("dir")
    val ITEM_KEY = ActionParameters.Key<String>("item")

    fun saveItems(context: Context, items: List<ShoppingItem>) = prefs(context).edit {
        putString("items", json.encodeToString(ListSerializer(ShoppingItem.serializer()), items))
    }
    fun saveCategories(context: Context, cats: List<ShoppingCategory>) = prefs(context).edit {
        putString("categories", json.encodeToString(ListSerializer(ShoppingCategory.serializer()), cats))
    }
    fun items(context: Context): List<ShoppingItem> = prefs(context).getString("items", null)
        ?.let { json.decodeFromString(ListSerializer(ShoppingItem.serializer()), it) }.orEmpty()
    fun categories(context: Context): List<ShoppingCategory> = prefs(context).getString("categories", null)
        ?.let { json.decodeFromString(ListSerializer(ShoppingCategory.serializer()), it) }.orEmpty()
    fun remove(context: Context, id: String) = saveItems(context, items(context).filter { it.id != id })
}

class ShoppingWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cats = WidgetStore.categories(context).sortedBy { it.order }
        val items = WidgetStore.items(context)
        provideContent {
            GlanceTheme(colors = WidgetColorStyle) {
                val prefs = currentState<Preferences>()
                val selected = cats.firstOrNull { it.id == prefs[DataKeys.SELECTED_CATEGORY] } ?: cats.firstOrNull()
                val list = items.filter { it.categoryId == selected?.id }.sortedBy { it.order }
                WidgetContent(context, selected?.name.orEmpty(), list)
            }
        }
    }
}

class ShoppingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ShoppingWidget()
}

@Composable
private fun WidgetContent(context: Context, title: String, items: List<ShoppingItem>) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(MainConstants.SPACE)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(ImageProvider(R.drawable.arrow_left),
                contentDescription = context.getString(R.string.back),
                modifier = GlanceModifier.size(Constants.BUTTON_SIZE)
                    .clickable(actionRunCallback<ChangeCategoryAction>(actionParametersOf(DIR_KEY to -1))),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary)
            )
            Text(
                text = title,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
            )
            Image(ImageProvider(R.drawable.arrow_right),
                contentDescription = context.getString(R.string.forward),
                modifier = GlanceModifier.size(Constants.BUTTON_SIZE)
                    .clickable(actionRunCallback<ChangeCategoryAction>(actionParametersOf(DIR_KEY to 1))),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary)
            )
        }
        LazyColumn {
            items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                CheckBox(
                    checked = false,
                    onCheckedChange = actionRunCallback<ToggleItemAction>(
                        actionParametersOf(ITEM_KEY to item.id)),
                    text = listOfNotNull(item.name, item.quantity).joinToString(" - "),
                    style = TextStyle(color = GlanceTheme.colors.onBackground)
                )
            }
        }
    }
}

class ChangeCategoryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val dir = parameters[DIR_KEY] ?: return
        val cats = WidgetStore.categories(context).sortedBy { it.order }
        if (cats.isEmpty()) return
        updateAppWidgetState(context, glanceId) { prefs ->
            val cur = cats.indexOfFirst { it.id == prefs[DataKeys.SELECTED_CATEGORY] }.coerceAtLeast(0)
            prefs[DataKeys.SELECTED_CATEGORY] = cats[(cur + dir + cats.size) % cats.size].id
        }
        ShoppingWidget().update(context, glanceId)
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ITEM_KEY] ?: return
        WidgetStore.remove(context, id)
        ShoppingWidget().updateAll(context)
        withTimeoutOrNull(5000.milliseconds) {
            FirebaseDatabase.getInstance().reference
                .child("items/$id/done").setValue(true).await()
        }
    }
}