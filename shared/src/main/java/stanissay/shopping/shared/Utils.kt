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

package stanissay.shopping.shared

import android.content.Context
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class GoogleUser(
    val uid: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?
)

@Serializable
data class ShoppingItem(
    val id: String = "",
    var name: String = "",
    var note: String = "",
    var quantity: String? = null,
    var done: Boolean = false,
    var order: Int = 0,
    var categoryId: String? = null,
    var activatedBy: String? = null,
    var activatedAt: Long? = null
)

@Serializable
data class ShoppingCategory(
    val id: String = "",
    var name: String = "",
    val order: Int = 0
)

@Serializable
data class ListSnapshot(
    val categories: List<ShoppingCategory> = emptyList(),
    val items: List<ShoppingItem> = emptyList(),
    val version: Long = 0
) {
    fun sortedCategories() = categories.sortedBy { it.order }

    fun itemsFor(categoryId: String?) =
        items.filter { !it.done && it.categoryId == categoryId }.sortedBy { it.order }

    fun cycle(currentId: String?, dir: Int): String? {
        val cats = sortedCategories()
        if (cats.isEmpty()) return null
        val cur = cats.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        return cats[(cur + dir + cats.size) % cats.size].id
    }
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
}

object AppEventManager {
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _uiEvents = MutableSharedFlow<UiEvent>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val uiEvents = _uiEvents.asSharedFlow()

    fun sendEvent(event: UiEvent) {
        managerScope.launch {
            _uiEvents.emit(event)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun resetReplayCache() {
        _uiEvents.resetReplayCache()
    }
}

fun showCustomToast(text: String, context: Context) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }

interface StringProvider {
    fun getString(resId: Int): String
}

class AndroidStringProvider(private val context: Context) : StringProvider {
    override fun getString(resId: Int): String = context.getString(resId)
}

internal fun parseItems(s: DataSnapshot): List<ShoppingItem> = s.children.mapNotNull { child ->
    val key = child.key ?: return@mapNotNull null
    runCatching { child.getValue(ShoppingItem::class.java) }.getOrNull()?.copy(id = key)
}
internal fun parseCategories(s: DataSnapshot): List<ShoppingCategory> = s.children.mapNotNull { child ->
    val key = child.key ?: return@mapNotNull null
    runCatching { child.getValue(ShoppingCategory::class.java) }.getOrNull()?.copy(id = key)
}

class SyncCancelledException(val code: Int, message: String) : Exception(message) {
    val isPermissionDenied get() = code == DatabaseError.PERMISSION_DENIED
}

class ActivationTracker(private val startFrom: Long, private val myId: String) {
    private val seen = mutableMapOf<String, Long>()
    var batchMax = 0L; private set

    fun accept(key: String, item: ShoppingItem): Boolean {
        val at = item.activatedAt ?: return false
        if (at <= (seen[key] ?: startFrom)) return false
        seen[key] = at
        batchMax = maxOf(batchMax, at)
        return item.activatedBy != myId
    }
}