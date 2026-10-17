package stanissay.shopping.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object PaddingStyle {
    val xs = PaddingValues(4.dp)
    val sm = PaddingValues(8.dp)
    val lg = PaddingValues(16.dp)

    val horizontalXs = PaddingValues(horizontal = 4.dp)
    val horizontalSm = PaddingValues(horizontal = 8.dp)
    val horizontalLg = PaddingValues(horizontal = 16.dp)
    val horizontalXl = PaddingValues(horizontal = 24.dp)

    val verticalXs = PaddingValues(vertical = 4.dp)

    val none = PaddingValues(0.dp)
    val innerText = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    val drawer = PaddingValues(start = 12.dp, bottom = 12.dp)
    val innerDrawer = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    val upBtn = PaddingValues(end = 24.dp, bottom = 24.dp)
    val quickAccess = PaddingValues(horizontal = 4.dp, vertical = 48.dp)
    val circularBar = PaddingValues(36.dp)
    val scrollBarV = PaddingValues(end = 2.dp)
    val scrollBarH = PaddingValues(bottom = 2.dp)
    val clickableBox = PaddingValues(bottom = 2.dp)
    val wearUpdate = PaddingValues(bottom = 32.dp)
    val wearTime = PaddingValues(top = 8.dp)
}

object MainConstants {
    val ELEVATION = 8.dp
    val SPACE = 4.dp
}