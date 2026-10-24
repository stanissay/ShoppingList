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

package stanissay.shopping.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MainColors {
    val BACKGROUND = Color(0xFF000000)
    val TRANSPARENT = Color(0x00000000)

    val FIRST_ACCENT = Color(0xFFFFA866)
    val SECOND_ACCENT = Color(0xFF6DADA0)
    val THIN_BORDER = Color(0xFF665F59)

    val WHITE = Color(0xFFCECECE)
    val GRAY = Color(0xFF424242)
    val LIGHT_GRAY = Color(0xFF85817D)

    val POSITIVE = Color(0xFF66BB8A)
    val NEGATIVE = Color(0xFFB23A48)
}

object PaddingStyle {
    val xs = PaddingValues(4.dp)
    val xl = PaddingValues(16.dp)

    val horizontalXs = PaddingValues(horizontal = 6.dp)
    val horizontalS = PaddingValues(horizontal = 8.dp)
    val horizontalXl = PaddingValues(horizontal = 16.dp)

    val verticalXxs = PaddingValues(vertical = 2.dp)
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