package stanissay.shopping.list.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import stanissay.shopping.list.MainViewModelW
import stanissay.shopping.list.OngoingManager.Companion.cancelNotification
import stanissay.shopping.shared.StringProvider

@SuppressLint("ModifierFactoryExtensionFunction")
fun TransformingLazyColumnItemScope.transformedItem(
    transformationSpec: TransformationSpec
): Modifier =
    Modifier
        .transformedHeight(this, transformationSpec)
        .graphicsLayer {
            with(transformationSpec) {
                applyContainerTransformation(scrollProgress)
            }
        }

class MainViewModelFactory(
    private val app: Application,
    private val strings: StringProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(MainViewModelW::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModelW(app, strings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = cancelNotification(context)
}