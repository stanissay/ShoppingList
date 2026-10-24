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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ripple
import stanissay.shopping.shared.MainColors
import stanissay.shopping.shared.R

@Composable
fun BaseButton(
    painter: Painter,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    val gestureModifier = when {
        onLongClick == null && onDoubleClick == null -> {
            Modifier.clickable(
                onClick = onClick,
                indication = ripple(
                    bounded = false,
                    radius = size / 2,
                    color = tint
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
        }
        onLongClick == null && onDoubleClick != null -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDoubleClick()
                },
                indication = ripple(
                    bounded = false,
                    radius = size / 2,
                    color = tint
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
        }
        onLongClick != null && onDoubleClick == null -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                indication = ripple(
                    bounded = false,
                    radius = size / 2,
                    color = tint
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
        }
        else -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke()
                },
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDoubleClick?.invoke()
                },
                indication = ripple(
                    bounded = false,
                    radius = size / 2,
                    color = tint
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
        }
    }

    Image(
        painter = painter,
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
            .size(size)
            .then(gestureModifier)
    )
}

@Composable
fun GoogleBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    description: String = stringResource(R.string.login),
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.google)

    Image(
        painter = img,
        contentDescription = description,
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick)
    )
}

@Composable
fun DoneBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    tint: Color = MainColors.POSITIVE,
    description: String = stringResource(R.string.done),
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.done_btn)

    BaseButton(
        painter = img,
        description = description,
        modifier = modifier,
        size = size,
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun CloseBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    tint: Color = MainColors.NEGATIVE,
    description: String = stringResource(R.string.close),
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.close_btn)

    BaseButton(
        painter = img,
        description = description,
        modifier = modifier,
        size = size,
        tint = tint,
        onClick = onClick
    )
}