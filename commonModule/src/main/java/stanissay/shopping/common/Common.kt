package stanissay.shopping.common

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItem(
    val id: String,
    var name: String,
    var quantity: String? = null,
    var order: Int,
    var isDone: Boolean = false
)