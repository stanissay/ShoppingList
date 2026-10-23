package stanissay.shopping.list

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import stanissay.shopping.shared.AppEventManager
import stanissay.shopping.shared.R
import stanissay.shopping.shared.SharedViewModel
import stanissay.shopping.shared.ShoppingCategory
import stanissay.shopping.shared.StringProvider
import stanissay.shopping.shared.UiEvent

class MainViewModelW (app: Application, val strings: StringProvider) : AndroidViewModel(app) {
    val sharedViewModel = SharedViewModel(strings)

    fun sendUiEvent(event: UiEvent) { AppEventManager.sendEvent(event) }

    fun login() {
        sharedViewModel.loadAll()
        viewModelScope.launch {
            sendUiEvent(UiEvent.ShowSnackbar("${strings.getString(R.string.hello)}, ${sharedViewModel.userName}"))
        }
    }
    fun logout() {
        sharedViewModel.logout()
        viewModelScope.launch {
            sendUiEvent(UiEvent.ShowSnackbar("${strings.getString(R.string.goodbye)}, ${sharedViewModel.userName}"))
        }
    }

    init {
        viewModelScope.launch {
            sharedViewModel.messages.collect { message ->
                sendUiEvent(UiEvent.ShowToast(message))
            }
        }
        sharedViewModel.init()
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        sharedViewModel.clear()
    }
}