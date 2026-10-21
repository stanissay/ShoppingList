package stanissay.shopping.common

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
    var categoryId: String? = null
)

@Serializable
data class ShoppingCategory(
    val id: String = "",
    var name: String = "",
    val order: Int = 0
)