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
import stanissay.shopping.common.ShoppingCategory
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
    var shoppingCategories by mutableStateOf(listOf<ShoppingCategory>())
        private set
    var backupShoppingItems: List<ShoppingItem>? = null
    var editItem by mutableStateOf<ShoppingItem?>(null)
    var editCategory by mutableStateOf<ShoppingCategory?>(null)
    var showItemDialog by mutableStateOf(false)
    var showCategoryDialog by mutableStateOf(false)
    var expandedItemId by mutableStateOf<String?>(null)
        private set

    fun showNewItemDialog() {
        editItem = null
        showItemDialog = true
    }
    fun showEditItemDialog(item: ShoppingItem) {
        editItem = item
        showItemDialog = true
    }
    fun hideItemDialog() {
        editItem = null
        showItemDialog = false
    }
    fun showNewCategoryDialog() {
        editCategory = null
        showCategoryDialog = true
    }
    fun showEditCategoryDialog(category: ShoppingCategory) {
        editCategory = category
        showCategoryDialog = true
    }
    fun hideCategoryDialog() {
        editCategory = null
        showCategoryDialog = false
    }
    fun toggleItemExpanded(item: ShoppingItem?) {
        expandedItemId = if (item == null || expandedItemId == item.id) null else item.id
    }

    fun sortItems() {
        shoppingItems = shoppingItems
            .groupBy { it.categoryId }
            .values
            .flatMap { categoryItems ->
                categoryItems
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
    }

    fun sortCategories() {
        shoppingCategories = shoppingCategories
            .sortedBy { it.order }
            .mapIndexed { index, category ->
                category.copy(order = index)
            }
    }

    fun editItem(item: ShoppingItem, categoryId: String?) {
        val originalItem = editItem
        val normalizedName = item.name.trim().lowercase()
        val existingByName = shoppingItems.firstOrNull {
            it.name.trim().lowercase() == normalizedName &&
                    (originalItem == null || it.id != originalItem.id)
        }
        val categoryOrder = shoppingItems.filter { it.categoryId == categoryId }.size

        // New element
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
                            order = existingByName.order,
                            isDone = false,
                            categoryId = categoryId
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
                order = categoryOrder,
                categoryId = categoryId
            )

            sortItems()
            return
        }


        // Edit element
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
                            order = existingByName.order,
                            isDone = false,
                            categoryId = categoryId
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
                    order = item.order,
                    isDone = false,
                    categoryId = categoryId
                )
            } else it
        }

        sortItems()
        hideItemDialog()
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

    fun moveItem(categoryId: String, from: Int, to: Int) {
        val categoryItems = shoppingItems
            .filter { it.categoryId == categoryId }

        if (from !in categoryItems.indices || to !in categoryItems.indices) return
        if (from == to) return

        if (backupShoppingItems == null) backupShoppingItems = shoppingItems

        val reorderedItems = categoryItems.toMutableList().apply {
            add(to, removeAt(from))
        }.mapIndexed { index, item ->
            item.copy(order = index)
        }

        val reorderedById = reorderedItems.associateBy { it.id }

        shoppingItems = shoppingItems.map { item ->
            reorderedById[item.id] ?: item
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
                sortItems()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                e.printStackTrace()
                shoppingItems = backupShoppingItems ?: emptyList()
                sendUiEvent(UiEvent.ShowToast(MainStrings.ERROR))
            } finally { isLoadingState.value = false }
        }
    }

    fun editCategory(category: ShoppingCategory) {
        val name = category.name.trim()
        if (name.isEmpty()) return

        if (shoppingCategories.any {
                it.id != category.id && it.name.trim().equals(name, ignoreCase = true)
            }) {
            viewModelScope.launch {
                sendUiEvent(UiEvent.ShowToast(MainStrings.DUPLICATE_CATEGORY))
            }
            return
        }

        val existing = shoppingCategories.any { it.id == category.id }

        shoppingCategories = if (existing) {
            shoppingCategories.map {
                if (it.id == category.id) it.copy(name = name) else it
            }
        } else {
            shoppingCategories + category.copy(
                id = generateUniqueId(),
                name = name,
                order = shoppingCategories.size
            )
        }

        hideCategoryDialog()
    }

    fun deleteCategory(id: String?) {
        shoppingCategories = shoppingCategories.filterNot { it.id == id }
        sortCategories()
    }

    fun moveCategory(from: Int, to: Int) {
        if (from !in shoppingCategories.indices || to !in shoppingCategories.indices || from == to) return
        val sorted = shoppingCategories.sortedBy { it.order }.toMutableList()
        sorted.add(to, sorted.removeAt(from))

        shoppingCategories = sorted.mapIndexed { index, item ->
            item.copy(order = index)
        }
    }

    fun pasteItems(clipboardText: String, categoryId: String?) {
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
                            order = shoppingItems.size,
                        ),
                        categoryId = categoryId
                    )
                }
            }
    }

    private fun generateUniqueId(): String {
        var id: String
        do { id = java.util.UUID.randomUUID().toString()
        } while (
            shoppingItems.any { it.id == id } ||
            shoppingCategories.any { it.id == id }
        )
        return id
    }

    fun getNameSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return shoppingItems.map { it.name }
            .distinctBy { it.lowercase() }.filter { it.startsWith(query, ignoreCase = true) }.take(5)
    }

    init {
        if(shoppingCategories.isEmpty()) {
            val initialCategory = ShoppingCategory(
                id = generateUniqueId(),
                name = MainStrings.HEAD,
                order = 0
            )
            editCategory(initialCategory)
        }
    }
}