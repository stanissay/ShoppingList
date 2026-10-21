package stanissay.shopping.list.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography
import stanissay.shopping.common.MainColors

val ColorStyle = ColorScheme(
    primary = MainColors.FIRST_ACCENT,
    secondary = MainColors.SECOND_ACCENT,

    background = MainColors.BACKGROUND,
    onBackground = MainColors.WHITE,

    surfaceContainer = MainColors.GRAY,
    onSurface = MainColors.LIGHT_GRAY,

    outline = MainColors.THIN_BORDER
)

val ShapesStyle = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

val TypographyStyle = Typography(
    arcMedium = CurvedTextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 8.sp
    ),

    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp
    ),

    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp
    ),

    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 10.sp
    )
)

object Constants {
    // Base
    val BASE_SIZE = 28.dp

    // Buttons
    val BUTTON_SIZE = 22.dp
}