package stanissay.shopping.list.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import stanissay.shopping.common.MainColors

val ColorStyle = darkColorScheme(
    primary = MainColors.FIRST_ACCENT,
    onPrimary = MainColors.LIGHT_GRAY,
    onPrimaryContainer = MainColors.WHITE,
    secondary = MainColors.SECOND_ACCENT,
    onSecondary = MainColors.LIGHT_GRAY,
    onSecondaryContainer = MainColors.WHITE,

    background = MainColors.BACKGROUND,
    onBackground = MainColors.WHITE,

    surface = MainColors.BACKGROUND,
    onSurface = MainColors.GRAY,
    surfaceVariant = MainColors.GRAY,
    onSurfaceVariant = MainColors.LIGHT_GRAY,

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
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp
    ),

    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 20.sp
    ),

    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp
    ),

    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp
    )
)

object Constants {
    // Base
    val BASE_SIZE_XXL = 240.dp
    val BASE_SIZE_XL = 96.dp
    val BASE_SIZE_L = 72.dp
    val BASE_SIZE = 64.dp
    val BASE_SIZE_S = 48.dp
    val BASE_SIZE_XS = 36.dp

    // Buttons
    val BUTTON_SIZE = 36.dp
    val BUTTON_SIZE_S = 28.dp
    val BUTTON_SIZE_XS = 24.dp

    // Chips
    val CHIP_HEIGHT = BUTTON_SIZE
    val CHIP_WIDTH = BUTTON_SIZE * 3

    // Elements
    val NULL = 0.dp
    val SPACE_S = 2.dp                                      // Tools materials indicator
    val SPACE = 4.dp                                        // Spacers
    val THICKNESS = 1.dp                                    // Divider
    val LINEAR_INDICATOR = 2.dp                             // Download indicator
    val SCROLL_BAR = BASE_SIZE_XS / 4                       // Vertical scroll bar
    val CIRCLE = BASE_SIZE_S / 3                            // Circle indicators
    val SMALL_CIRCLE = BASE_SIZE_S / 4                      // Small circle indicators
    val TEXT_LINE = BASE_SIZE_S / 4                         // Main Text Field
    val DROPDOWN = BASE_SIZE_XXL                            // Dropdowns width
    val MAIN_LOG_SIZE = BASE_SIZE_XXL / 1.5f                // Log box
    val MAIN_BOX_SIZE = BASE_SIZE_XXL                       // Main text box
    val DATE_DIALOG = BASE_SIZE_XXL * 2.5f                  // Date dialog
    val TECHNOLOGY_LIST_HEIGHT = BASE_SIZE_XXL              // Page technology
    val LAUNCH_LIST_HEIGHT = BASE_SIZE_XXL * 0.75f          // Page launches
    val PDF_HEIGHT = BASE_SIZE_XXL                          // Page PDF
    val QUICK_ACCESS = BASE_SIZE * 2f                       // Quick access width
    val TOOLTIP = -BASE_SIZE                                // Tooltip offset
}