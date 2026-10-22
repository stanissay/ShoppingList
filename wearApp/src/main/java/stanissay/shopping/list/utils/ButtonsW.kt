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
fun CheckBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val img =  painterResource(id = if(checked) R.drawable.check_btn else R.drawable.box_btn)
    val description = if(checked) stringResource(R.string.not_purchased) else stringResource(R.string.purchased)
    val tint = if(checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary

    BaseButton(
        painter = img,
        description = description,
        modifier = modifier,
        size = size,
        tint = tint,
        onClick = onClick
    )
}