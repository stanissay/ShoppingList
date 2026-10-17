package stanissay.shopping.list.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp

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