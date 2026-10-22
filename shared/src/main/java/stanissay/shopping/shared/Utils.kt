package stanissay.shopping.shared

import android.content.Context
import android.widget.Toast
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
    var categoryId: String? = null
)

@Serializable
data class ShoppingCategory(
    val id: String = "",
    var name: String = "",
    val order: Int = 0
)

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