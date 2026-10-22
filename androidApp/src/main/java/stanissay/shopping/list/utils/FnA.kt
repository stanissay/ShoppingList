package stanissay.shopping.list.utils

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import stanissay.shopping.list.MainViewModelA
import stanissay.shopping.shared.StringProvider

class MainViewModelFactory(
    private val app: Application,
    private val strings: StringProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(MainViewModelA::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModelA(app, strings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}