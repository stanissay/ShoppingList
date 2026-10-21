package stanissay.shopping.list.utils

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

fun showCustomToast(text: String, context: Context) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }

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