package stanissay.shopping.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class SharedViewModel(private val strings: StringProvider) {
    private val database = FirebaseDatabase.getInstance()
    val itemsRef = database.getReference("items")
    val categoriesRef = database.getReference("categories")
    val auth = FirebaseAuth.getInstance()
    var isLoggedIn by mutableStateOf(auth.currentUser != null)
        private set
    var userName by mutableStateOf(auth.currentUser?.displayName)
        private set
    var userEmail by mutableStateOf(auth.currentUser?.email)
        private set
    var userPhotoUrl by mutableStateOf(auth.currentUser?.photoUrl?.toString())
        private set
    val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        isLoggedIn = user != null
        userName = user?.displayName
        userEmail = user?.email
        userPhotoUrl = user?.photoUrl?.toString()
    }
    val isLoadingState = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = isLoadingState
    var shoppingItems by mutableStateOf(listOf<ShoppingItem>())
        private set
    var shoppingCategories by mutableStateOf(listOf<ShoppingCategory>())
        private set
    private var backupShoppingItems: List<ShoppingItem>? = null
    private var backupShoppingCategories: List<ShoppingCategory>? = null
    var editItem by mutableStateOf<ShoppingItem?>(null)
    var editCategory by mutableStateOf<ShoppingCategory?>(null)
    var showItemDialog by mutableStateOf(false)
    var showCategoryDialog by mutableStateOf(false)
    var showLogoutDialog by mutableStateOf(false)
    var expandedItemId by mutableStateOf<String?>(null)
        private set
    private var categoriesLoaded = false
    private var itemsLoaded = false
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

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
    private fun updateLoadingState() {
        isLoadingState.value = !(categoriesLoaded && itemsLoaded)
    }
    fun toggleItemExpanded(item: ShoppingItem?) {
        expandedItemId = if (item == null || expandedItemId == item.id) null else item.id
    }

    fun loadAll() {
        if (auth.currentUser == null) return
        categoriesLoaded = false
        itemsLoaded = false
        isLoadingState.value = true
        observeCategories()
        observeItems()
    }

    private val categoriesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            shoppingCategories = snapshot.children.mapNotNull { child ->
                val category = child.getValue(ShoppingCategory::class.java)
                category?.copy(id = child.key ?: return@mapNotNull null)
            }
            categoriesLoaded = true
            updateLoadingState()
            sortCategories()
        }

        override fun onCancelled(error: DatabaseError) {
            _messages.tryEmit(error.message)
        }
    }
    private val itemsListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            shoppingItems = snapshot.children.mapNotNull { child ->
                val item = child.getValue(ShoppingItem::class.java)
                item?.copy(id = child.key ?: return@mapNotNull null)
            }
            itemsLoaded = true
            updateLoadingState()
            sortItems()
        }

        override fun onCancelled(error: DatabaseError) {
            _messages.tryEmit(error.message)
        }
    }
    private fun observeCategories() {
        if (auth.currentUser == null) return
        categoriesRef.addValueEventListener(categoriesListener)
    }
    private fun observeItems() {
        if (auth.currentUser == null) return
        itemsRef.addValueEventListener(itemsListener)
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
                        compareBy<ShoppingItem> { it.done }
                            .thenBy { if (it.done) it.name.lowercase() else "" }
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
        isLoadingState.value = true
        val originalItem = editItem
        val normalizedName = item.name.trim().lowercase()
        val categoryOrder = shoppingItems.count { it.categoryId == categoryId }
        val existingByName = shoppingItems.firstOrNull {
            it.name.trim().lowercase() == normalizedName &&
                    (originalItem == null || it.id != originalItem.id)
        }

        fun mergeQuantity(existing: ShoppingItem): String? {
            val existingQuantity = existing.quantity
            val incomingQuantity = item.quantity

            return when {
                existingQuantity == null || existing.done || existing.categoryId != categoryId -> incomingQuantity
                incomingQuantity == null -> existingQuantity
                else -> {
                    val existingInt = existingQuantity.toIntOrNull()
                    val incomingInt = incomingQuantity.toIntOrNull()
                    if (existingInt != null && incomingInt != null) {
                        (existingInt + incomingInt).toString()
                    } else incomingQuantity
                }
            }
        }

        fun saveItem(updated: ShoppingItem, deleteId: String? = null) {
            val updates = mutableMapOf<String, Any?>()
            if (deleteId != null && deleteId != updated.id) {
                updates[deleteId] = null
            }
            updates[updated.id] = updated

            itemsRef.updateChildren(updates)
                .addOnSuccessListener {}.addOnFailureListener { error ->
                    _messages.tryEmit(error.message ?: strings.getString(R.string.error))
                }.addOnCompleteListener { isLoadingState.value = false }
        }

        if (originalItem == null) {
            if (existingByName != null) {
                saveItem(
                    existingByName.copy(
                        note = item.note,
                        quantity = mergeQuantity(existingByName),
                        done = false,
                        categoryId = categoryId
                    )
                )
                return
            }

            val uniqueId = if (item.id.isNotBlank() && shoppingItems.none { it.id == item.id }) item.id else generateUniqueId()

            saveItem(
                item.copy(
                    id = uniqueId,
                    name = item.name.trim(),
                    note = item.note,
                    done = false,
                    order = categoryOrder,
                    categoryId = categoryId
                )
            )
            return
        }

        val original = shoppingItems.firstOrNull { it.id == originalItem.id }
        if (original == null) {
            hideItemDialog()
            return
        }

        if (existingByName != null) {
            saveItem(
                existingByName.copy(
                    note = item.note,
                    quantity = mergeQuantity(existingByName),
                    done = false,
                    categoryId = categoryId
                ),
                deleteId = original.id
            )
            hideItemDialog()
            return
        }

        val updated = original.copy(
            name = item.name.trim(),
            note = item.note,
            quantity = item.quantity,
            order = item.order,
            done = false,
            categoryId = categoryId
        )

        saveItem(updated)
        hideItemDialog()
    }

    fun deleteItem(item: ShoppingItem) {
        isLoadingState.value = true
        itemsRef.child(item.id)
            .removeValue()
            .addOnFailureListener { error ->
                _messages.tryEmit(error.message ?: strings.getString(R.string.error))
            }.addOnCompleteListener { isLoadingState.value = false }
    }

    fun toggleItemDone(item: ShoppingItem) {
        itemsRef.child(item.id)
            .child("done")
            .setValue(!item.done)
            .addOnFailureListener { error ->
                _messages.tryEmit(error.message ?: strings.getString(R.string.error))
            }
    }

    fun updateQuantity(item: ShoppingItem, quantity: Int) {
        itemsRef.child(item.id)
            .child("quantity")
            .setValue(quantity.toString())
            .addOnFailureListener { error ->
                _messages.tryEmit(error.message ?: strings.getString(R.string.error))
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

    suspend fun updateItemsOrder() {
        val newOrder = shoppingItems
        val oldOrder = backupShoppingItems ?: shoppingItems

        try {
            isLoadingState.value = true

            val updates = newOrder.filter { newItem ->
                oldOrder.find { it.id == newItem.id }?.order != newItem.order
            }.associate { item ->
                "${item.id}/order" to item.order
            }
            if (updates.isNotEmpty()) itemsRef.updateChildren(updates).await()

            backupShoppingItems = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            shoppingItems = backupShoppingItems ?: oldOrder
            _messages.tryEmit(e.message ?: strings.getString(R.string.error))
        } finally {
            backupShoppingItems = null
            isLoadingState.value = false
        }
    }

    fun editCategory(category: ShoppingCategory) {
        isLoadingState.value = true
        val name = category.name.trim()
        if (name.isEmpty()) return

        if (shoppingCategories.any { it.id != category.id && it.name.trim().equals(name, ignoreCase = true) }) {
            _messages.tryEmit(strings.getString(R.string.duplicate_category))
            return
        }

        val existing = shoppingCategories.any { it.id == category.id }

        val updatedCategory = if (existing) { category.copy(name = name) } else {
            category.copy(
                id = generateUniqueId(),
                name = name,
                order = shoppingCategories.size
            )
        }

        categoriesRef.child(updatedCategory.id)
            .setValue(updatedCategory)
            .addOnSuccessListener {
                hideCategoryDialog()
            }.addOnFailureListener { error ->
                _messages.tryEmit(error.message ?: strings.getString(R.string.error))
            }.addOnCompleteListener { isLoadingState.value = false }
    }

    fun deleteCategory(id: String?) {
        isLoadingState.value = true
        if (id == null) return

        categoriesRef.child(id)
            .removeValue()
            .addOnSuccessListener { hideCategoryDialog() }
            .addOnFailureListener { error ->
                _messages.tryEmit(error.message ?: strings.getString(R.string.error))
            }.addOnCompleteListener { isLoadingState.value = false }
    }

    fun moveCategory(from: Int, to: Int) {
        val sorted = shoppingCategories.sortedBy { it.order }
        if (from !in sorted.indices || to !in sorted.indices || from == to) return
        if (backupShoppingCategories == null) backupShoppingCategories = shoppingCategories

        val reordered = sorted.toMutableList().apply { add(to, removeAt(from)) }

        shoppingCategories = reordered.mapIndexed { index, category ->
            category.copy(order = index)
        }
    }

    suspend fun updateCategoriesOrder() {
        val newOrder = shoppingCategories
        val oldOrder = backupShoppingCategories ?: shoppingCategories

        try {
            isLoadingState.value = true

            val updates = newOrder.filter { newCategory ->
                oldOrder.find { it.id == newCategory.id }?.order != newCategory.order
            }.associate { category ->
                "${category.id}/order" to category.order
            }
            if (updates.isNotEmpty()) categoriesRef.updateChildren(updates).await()

            backupShoppingCategories = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            shoppingCategories = backupShoppingCategories ?: oldOrder
            _messages.tryEmit(e.message ?: strings.getString(R.string.error))
        } finally {
            backupShoppingCategories = null
            isLoadingState.value = false
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

    fun logout() {
        categoriesRef.removeEventListener(categoriesListener)
        itemsRef.removeEventListener(itemsListener)
        shoppingCategories = emptyList()
        shoppingItems = emptyList()
        showLogoutDialog = false
        isLoadingState.value = false
    }

    fun init() {
        auth.addAuthStateListener(authListener)
        loadAll()
    }

    fun clear() {
        auth.removeAuthStateListener(authListener)
        categoriesRef.removeEventListener(categoriesListener)
        itemsRef.removeEventListener(itemsListener)
    }
}