package stanissay.shopping.list

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import stanissay.shopping.common.MainStrings
import stanissay.shopping.common.ShoppingItem
import stanissay.shopping.list.utils.AppEventManager
import stanissay.shopping.list.utils.UiEvent
import kotlin.coroutines.cancellation.CancellationException

class MainViewModelA(app: Application) : AndroidViewModel(app) {
    val isLoadingState = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = isLoadingState
    fun sendUiEvent(event: UiEvent) { AppEventManager.sendEvent(event) }
    var shoppingItems by mutableStateOf(listOf<ShoppingItem>())
        private set
    var backupShoppingItems: List<ShoppingItem>? = null
    var editItem by mutableStateOf<ShoppingItem?>(null)
    var showItemDialog by mutableStateOf(false)
    var expandedItemId by mutableStateOf<String?>(null)
        private set

    fun showNewItemDialog() {
        editItem = null
        showItemDialog = true
    }
    fun hideItemDialog() {
        editItem = null
        showItemDialog = false
    }
    fun showEditItemDialog(item: ShoppingItem) {
        editItem = item
        showItemDialog = true
    }

    fun toggleItemExpanded(item: ShoppingItem?) {
        expandedItemId = if (item == null || expandedItemId == item.id) null else item.id
    }

    fun sortItems() {
        shoppingItems = shoppingItems
            .sortedBy { it.order }
            .mapIndexed { index, item ->
                item.copy(order = index)
            }
            .sortedWith(
                compareBy<ShoppingItem> { it.isDone }
                    .thenBy { if (it.isDone) it.name.lowercase() else "" }
                    .thenBy { it.order }
            )
    }

    fun editItem(item: ShoppingItem) {
        val originalItem = editItem
        val normalizedName = item.name.trim().lowercase()
        val existingByName = shoppingItems.firstOrNull {
            it.name.trim().lowercase() == normalizedName &&
                    (originalItem == null || it.id != originalItem.id)
        }

        if (originalItem == null) {
            if (existingByName != null) {
                val existingQuantity = existingByName.quantity
                val incomingQuantity = item.quantity
                val mergedQuantity = when {
                    (existingQuantity == null || existingByName.isDone) -> incomingQuantity
                    incomingQuantity == null -> existingQuantity
                    else -> {
                        val existingInt = existingQuantity.toIntOrNull()
                        val incomingInt = incomingQuantity.toIntOrNull()

                        if (existingInt != null && incomingInt != null) {
                            (existingInt + incomingInt).toString()
                        } else incomingQuantity
                    }
                }

                shoppingItems = shoppingItems.map {
                    if (it.id == existingByName.id) {
                        it.copy(
                            quantity = mergedQuantity,
                            order = shoppingItems.size,
                            isDone = false
                        )
                    } else it
                }

                sortItems()
                return
            }

            val uniqueId = if (item.id.isNotBlank() &&
                shoppingItems.none { it.id == item.id }
            ) item.id else generateUniqueId()

            shoppingItems = shoppingItems + item.copy(
                id = uniqueId,
                name = item.name.trim(),
                isDone = false,
                order = shoppingItems.size
            )

            sortItems()
            return
        }

        val originalIndex = shoppingItems.indexOfFirst { it.id == originalItem.id }

        if (originalIndex == -1) {
            hideItemDialog()
            return
        }

        val original = shoppingItems[originalIndex]

        if (existingByName != null) {
            val existingQuantity = existingByName.quantity
            val incomingQuantity = item.quantity
            val mergedQuantity = when {
                (existingQuantity == null || existingByName.isDone) -> incomingQuantity
                incomingQuantity == null -> existingQuantity
                else -> {
                    val existingInt = existingQuantity.toIntOrNull()
                    val incomingInt = incomingQuantity.toIntOrNull()

                    if (existingInt != null && incomingInt != null) {
                        (existingInt + incomingInt).toString()
                    } else incomingQuantity
                }
            }

            shoppingItems = shoppingItems.filterNot { it.id == original.id }
                .map {
                    if (it.id == existingByName.id) {
                        it.copy(
                            quantity = mergedQuantity,
                            order = shoppingItems.size,
                            isDone = false
                        )
                    } else it
                }

            sortItems()
            hideItemDialog()
            return
        }

        val uniqueId = if (item.id.isNotBlank() &&
            shoppingItems.none { it.id == item.id && it.id != original.id }
        ) item.id else original.id

        shoppingItems = shoppingItems.map {
            if (it.id == original.id) {
                original.copy(
                    id = uniqueId,
                    name = item.name.trim(),
                    quantity = item.quantity,
                    order = shoppingItems.size,
                    isDone = false
                )
            } else it
        }

        sortItems()
        hideItemDialog()
    }

    fun pasteItems(clipboardText: String) {
        clipboardText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { line ->
                val quantityStart = line.indexOfFirst { it.isDigit() }

                val name = if (quantityStart >= 0) {
                    line.substring(0, quantityStart).trim().trimEnd('-', ':', ' ')
                } else line

                val quantity = if (quantityStart >= 0) {
                    line.substring(quantityStart).trim()
                } else null

                if (name.isNotEmpty()) {
                    editItem(
                        ShoppingItem(
                            id = "",
                            name = name,
                            quantity = quantity,
                            order = shoppingItems.size
                        )
                    )
                }
            }
    }

    fun deleteItem(item: ShoppingItem) {
        shoppingItems = shoppingItems.filterNot { it.id == item.id }
        sortItems()
    }

    fun toggleItemDone(item: ShoppingItem) {
        shoppingItems = shoppingItems.map {
            if (it.id == item.id) it.copy(isDone = !it.isDone) else it
        }
        sortItems()
    }

    fun updateQuantity(item: ShoppingItem, quantity: Int) {
        shoppingItems = shoppingItems.map {
            if (it.id == item.id) {
                it.copy(quantity = quantity.toString())
            } else it
        }
    }

    fun moveItem(from: Int, to: Int) {
        if (from !in shoppingItems.indices || to !in shoppingItems.indices) return
        if (from == to) return
        if (backupShoppingItems == null) backupShoppingItems = shoppingItems
        val items = shoppingItems.toMutableList()
        items.add(to, items.removeAt(from))
        shoppingItems = items.mapIndexed { index, item ->
            item.copy(order = index)
        }
        sortItems()
    }
    fun onDragEnd() {
        updateOrder(
            newOrder = shoppingItems,
            oldOrder = backupShoppingItems ?: shoppingItems
        )
        backupShoppingItems = null
    }
    fun updateOrder(newOrder: List<ShoppingItem>, oldOrder: List<ShoppingItem>) {
        viewModelScope.launch {
            try {
                isLoadingState.value = true
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                e.printStackTrace()
                shoppingItems = backupShoppingItems ?: emptyList()
                sendUiEvent(UiEvent.ShowToast(MainStrings.ERROR))
            } finally { isLoadingState.value = false }
        }
    }

    private fun generateUniqueId(): String {
        var id: String
        do { id = java.util.UUID.randomUUID().toString()
        } while (shoppingItems.any { it.id == id })
        return id
    }

    fun getNameSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return shoppingItems.map { it.name }
            .distinctBy { it.lowercase() }.filter { it.startsWith(query, ignoreCase = true) }.take(5)
    }
}