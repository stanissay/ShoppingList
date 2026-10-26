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
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.material3.ColorProviders
import stanissay.shopping.shared.MainColors

val ColorStyle = darkColorScheme(
    primary = MainColors.FIRST_ACCENT,
    secondary = MainColors.SECOND_ACCENT,

    background = MainColors.BACKGROUND,
    onBackground = MainColors.WHITE,

    surface = MainColors.GRAY,
    onSurface = MainColors.LIGHT_GRAY,

    outline = MainColors.THIN_BORDER
)

private val widgetColorScheme = darkColorScheme(
    primary = MainColors.FIRST_ACCENT,
    secondary = MainColors.SECOND_ACCENT,

    background = MainColors.BACKGROUND,
    onBackground = MainColors.WHITE,

    surface = MainColors.GRAY,
    onSurface = MainColors.LIGHT_GRAY,

    outline = MainColors.THIN_BORDER
)

val WidgetColorStyle = ColorProviders(
    light = widgetColorScheme,
    dark = widgetColorScheme
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
    val BASE_SIZE_L = 144.dp
    val BASE_SIZE = 64.dp
    val BASE_SIZE_S = 32.dp
    val BASE_SIZE_XS = 24.dp

    // Buttons
    val BUTTON_SIZE = 48.dp
    val BUTTON_SIZE_S = 32.dp

    // Elements
    val SCROLL_BAR = 4.dp       // Vertical scroll bar
    val DROPDOWN = 240.dp       // Dropdowns width
}