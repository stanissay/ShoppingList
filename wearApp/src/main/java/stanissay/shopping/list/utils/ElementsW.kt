package stanissay.shopping.list.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorColors
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ripple
import stanissay.shopping.shared.MainColors
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.PaddingStyle

@Composable
fun MainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorStyle,
        typography = TypographyStyle,
        shapes = ShapesStyle
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { content() }
    }
}

@Composable
fun CircularBar() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingStyle.circularBar)
            .background(MainColors.TRANSPARENT),
        contentAlignment = Alignment.BottomCenter
    ) {
        CircularProgressIndicator(
            colors = ProgressIndicatorColors(
                indicatorBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainer),
                trackBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                overflowTrackBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                disabledIndicatorBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainer),
                disabledTrackBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                disabledOverflowTrackBrush = SolidColor(MaterialTheme.colorScheme.secondary)
            )
        )
    }
}

@Composable
fun MainCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color = MainColors.TRANSPARENT,
    contentPadding: PaddingValues = PaddingStyle.none,
    containerPadding: PaddingValues = PaddingStyle.none,
    shape: Shape = ShapesStyle.medium,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(containerPadding)
            .shadow(
                elevation = MainConstants.ELEVATION,
                shape = shape
            )
            .clip(shape)
            .background(containerColor)
            .border(
                width = MainConstants.THICKNESS,
                color = borderColor,
                shape = shape
            )
    ) { Box(modifier = Modifier.padding(contentPadding)) { content() } }
}

@Composable
fun MainLines(modifier: Modifier) {
    val cellSize = with(LocalDensity.current) { Constants.BASE_SIZE_S.toPx() }

    Canvas(modifier = modifier) {
        val columns = (size.width / cellSize).toInt() + 1
        val rows = (size.height / cellSize).toInt() + 1

        for (i in 0..columns) {
            val x = cellSize / 2 +  i * cellSize
            drawLine(
                color = MainColors.THIN_BORDER.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        for (i in 0..rows) {
            val y = cellSize / 2 + i * cellSize
            drawLine(
                color = MainColors.THIN_BORDER.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun ClickableBox(
    modifier: Modifier = Modifier,
    background: Color = MainColors.TRANSPARENT,
    shape: Shape = ShapesStyle.small,
    contentAlignment: Alignment = Alignment.Center,
    rippleRadius: Dp = Dp.Unspecified,
    rippleColor: Color = MaterialTheme.colorScheme.primary,
    bounded: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val clickModifier = when {
        onLongClick == null && onDoubleClick == null -> {
            Modifier.clickable(
                onClick = onClick,
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
            )
        }
        onLongClick == null && onDoubleClick != null -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDoubleClick()
                },
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
            )
        }
        onLongClick != null && onDoubleClick == null -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
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
                indication = ripple(bounded, rippleRadius, rippleColor),
                interactionSource = interactionSource
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(clickModifier)
    ) {
        Box(
            modifier = Modifier
                .align(contentAlignment)
                .matchParentSize(),
            contentAlignment = contentAlignment
        ) { content() }
    }
}

@Composable
fun MainText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontSize: TextUnit = style.fontSize,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = MaterialTheme.colorScheme.onBackground,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textDecoration: TextDecoration = TextDecoration.None
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = style,
        color = color,
        maxLines = maxLines,
        textDecoration = textDecoration,
        overflow = overflow,
        fontSize = fontSize
    )
}

@Composable
fun MainText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontSize: TextUnit = style.fontSize,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = MaterialTheme.colorScheme.onBackground,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textDecoration: TextDecoration = TextDecoration.None
) {
    MainText(
        text = AnnotatedString(text),
        modifier = modifier,
        style = style,
        textAlign = textAlign,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textDecoration = textDecoration,
        fontSize = fontSize
    )
}

@Composable
fun PageTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    MainText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.displayMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary
    )
}