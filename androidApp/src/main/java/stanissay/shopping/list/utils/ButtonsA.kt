package stanissay.shopping.list.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import stanissay.shopping.common.MainColors
import stanissay.shopping.common.MainStrings
import stanissay.shopping.list.R

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
fun AddBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    tint: Color = MaterialTheme.colorScheme.secondary,
    description: String = MainStrings.ADD,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val img = painterResource(id = R.drawable.plus_btn)

    Box(
        modifier = Modifier.size(size)
            .background(
                color = tint.copy(alpha = 0.25f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        BaseButton(
            painter = img,
            description = description,
            modifier = modifier,
            size = size,
            tint = tint,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

@Composable
fun PasteBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    tint: Color = MaterialTheme.colorScheme.secondary,
    description: String = MainStrings.PASTE,
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.paste_btn)

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
fun GoogleBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    description: String = MainStrings.LOGIN,
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
fun PlusBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE_S,
    tint: Color = MaterialTheme.colorScheme.primary,
    description: String = MainStrings.ADD,
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.plus_btn)

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
fun MinusBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE_S,
    tint: Color = MaterialTheme.colorScheme.primary,
    description: String = MainStrings.ADD,
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.minus_btn)

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
fun DoneBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE,
    tint: Color = MainColors.POSITIVE,
    description: String = MainStrings.DONE,
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
    description: String = MainStrings.CLOSE,
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

@Composable
fun EditBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE_S,
    tint: Color = MaterialTheme.colorScheme.secondary,
    description: String = MainStrings.EDIT,
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.edit_btn)

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
fun DeleteBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE_S,
    tint: Color = MainColors.NEGATIVE,
    description: String = MainStrings.DELETE,
    onClick: () -> Unit
) {
    val img = painterResource(id = R.drawable.delete_btn)

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
fun CheckBtn(
    modifier: Modifier = Modifier,
    size: Dp = Constants.BUTTON_SIZE_S,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val img =  painterResource(id = if(checked) R.drawable.check_btn else R.drawable.box_btn)
    val description = if(checked) MainStrings.NO_BUYING else MainStrings.BUYING
    val tint = if(checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary

    BaseButton(
        painter = img,
        description = description,
        modifier = modifier,
        size = size,
        tint = tint,
        onClick = onClick
    )
}