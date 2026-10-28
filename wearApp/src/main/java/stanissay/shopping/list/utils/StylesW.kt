/*
 * Shopping List For Android and Wear Os
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.shopping.list.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography
import androidx.wear.protolayout.types.argb
import stanissay.shopping.shared.MainColors

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
        fontSize = 12.sp
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
    val BASE_SIZE = 36.dp
    val BASE_SIZE_S = 18.dp

    // Buttons
    val BUTTON_SIZE = 28.dp
}

fun tileColorScheme(c: ColorScheme) = androidx.wear.protolayout.material3.ColorScheme(
    primary = c.primary.toArgb().argb,
    secondary = c.secondary.toArgb().argb,

    background = c.background.toArgb().argb,
    onBackground = c.onBackground.toArgb().argb,

    surfaceContainer = c.surfaceContainer.toArgb().argb,
    onSurface = c.onSurface.toArgb().argb,

    outline = c.outline.toArgb().argb
)