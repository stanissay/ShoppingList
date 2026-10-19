package stanissay.shopping.common

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItem(
    val id: String,
    var name: String,
    var quantity: String? = null,
    var isDone: Boolean = false,
    var order: Int,
    var categoryId: String? = null
)

@Serializable
data class ShoppingCategory(
    val id: String,
    var name: String,
    val order: Int
)