package stanissay.shopping.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object PaddingStyle {
    val xs = PaddingValues(4.dp)
    val xl = PaddingValues(16.dp)

    val horizontalS = PaddingValues(horizontal = 8.dp)
    val horizontalXl = PaddingValues(horizontal = 16.dp)

    val verticalXs = PaddingValues(vertical = 4.dp)

    val none = PaddingValues(0.dp)
    val circularBar = PaddingValues(36.dp)
    val scrollBarV = PaddingValues(end = 2.dp)
}

object MainConstants {
    val ELEVATION = 8.dp
    val SPACE = 4.dp
    val THICKNESS = 1.dp
    val LINE = 4.dp
    val NULL = 0.dp

    const val ANIMATION_MS = 300
}