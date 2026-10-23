package stanissay.shopping.list.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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
fun MainCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    borderColor: Color = MainColors.TRANSPARENT,
    contentPadding: PaddingValues = PaddingStyle.none,
    containerPadding: PaddingValues = PaddingStyle.none,
    shape: Shape = ShapesStyle.medium,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier.padding(containerPadding),
        shape = shape,
        elevation = CardDefaults.cardElevation(MainConstants.ELEVATION),
        border = BorderStroke(MainConstants.THICKNESS, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
fun MainDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = MainConstants.THICKNESS, color = MaterialTheme.colorScheme.outline)
}

@Composable
fun TextDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<String>,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(Constants.DROPDOWN),
        properties = PopupProperties(focusable = false),
        tonalElevation = MainConstants.NULL,
        shadowElevation = MainConstants.ELEVATION,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = MainConstants.THICKNESS,
            color = MaterialTheme.colorScheme.outline
        ),
        offset = DpOffset(x = MainConstants.SPACE, y = MainConstants.SPACE)
    ) {
        Column {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onItemClick(item)
                        onDismissRequest()
                    },
                    modifier = Modifier,
                    text = { MainText(item) }
                )
            }
        }
    }
}

@Composable
fun MainTextField(
    modifier: Modifier = Modifier,
    state: TextFieldState,
    placeholderText: String,
    fontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
    fontPlaceholderSize: TextUnit = MaterialTheme.typography.labelMedium.fontSize,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    textAlign: TextAlign = TextAlign.Center,
    alignment: Alignment = Alignment.Center,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Unspecified,
    onKeyboardAction: KeyboardActionHandler? = null,
    digitOnly: Boolean = false,
    onTextChange: (String) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(state.text) { onTextChange(state.text.toString()) }

    Box(modifier = modifier.padding(PaddingStyle.xs)) {
        BasicTextField(
            state = state,
            modifier = Modifier
                .align(Alignment.Center)
                .heightIn(min = Constants.BASE_SIZE)
                .fillMaxWidth()
                .padding(PaddingStyle.horizontalS)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focusState -> isFocused = focusState.isFocused },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = textColor,
                fontSize = fontSize,
                textAlign = textAlign
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
            decorator = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = alignment
                ) {
                    if (state.text.isEmpty()) {
                        MainText(
                            text = placeholderText,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = textAlign,
                            fontSize = fontPlaceholderSize,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    innerTextField()
                }
            },
            scrollState = rememberScrollState(),
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = !digitOnly,
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            lineLimits = lineLimits,
            inputTransformation = InputTransformation.byValue { _, proposed ->
                if (digitOnly) proposed.filter { it.isDigit() }.toString() else proposed
            },
            onKeyboardAction = onKeyboardAction
        )
        if (lineLimits == TextFieldLineLimits.SingleLine) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(PaddingStyle.horizontalS),
                thickness = MainConstants.THICKNESS,
                color = if (isFocused) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
            )
        }
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
fun SelectionText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontSize: TextUnit = style.fontSize,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val annotatedText = buildAnnotatedString {
        val urlRegex = Regex("""https?://\S+""")
        var lastIndex = 0

        urlRegex.findAll(text).forEach { match ->
            append(text.substring(lastIndex, match.range.first))
            withLink(LinkAnnotation.Url(match.value)) { append(match.value) }
            lastIndex = match.range.last + 1
        }

        append(text.substring(lastIndex))
    }

    SelectionContainer(modifier = modifier) {
        MainText(
            text = annotatedText,
            style = style,
            textAlign = TextAlign.Start,
            color = color,
            maxLines = Int.MAX_VALUE,
            fontSize = fontSize
        )
    }
}

@Composable
fun CaptionText(
    text: String,
    modifier: Modifier = Modifier
) {
    MainText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
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

@Composable
fun DialogTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    MainText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = textAlign
    )
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
            color = MaterialTheme.colorScheme.background,
            trackColor = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun VerticalScrollBar(listState: LazyListState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val thumbInfo by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0)
                return@derivedStateOf 0f to 0f

            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val itemHeight = visibleItems.sumOf { it.size } / visibleItems.size
            val contentHeight = layoutInfo.totalItemsCount * itemHeight

            if (contentHeight <= viewportHeight)
                return@derivedStateOf viewportHeight.toFloat() to 0f

            val thumbHeightPx = (viewportHeight.toFloat() / contentHeight * viewportHeight)
                .coerceAtLeast(with(density) { Constants.BASE_SIZE_XS.toPx() })

            val scrollPx = listState.firstVisibleItemIndex * itemHeight +
                    listState.firstVisibleItemScrollOffset

            val maxOffset = (viewportHeight - thumbHeightPx).coerceAtLeast(0f)

            val offset = scrollPx.toFloat() / contentHeight.toFloat() * viewportHeight.toFloat()
            val thumbOffsetPx = offset.coerceIn(0f, maxOffset)

            thumbHeightPx to thumbOffsetPx
        }
    }

    val (thumbHeightPx, thumbOffsetPx) = thumbInfo

    if (thumbHeightPx >= with(density) { Constants.BASE_SIZE_XS.toPx() } &&
        thumbHeightPx < 10000f
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(Constants.SCROLL_BAR)
                .padding(PaddingStyle.scrollBarV)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), ShapesStyle.extraSmall)
        ) {
            Box(
                modifier = Modifier
                    .height(with(density) { thumbHeightPx.toDp() })
                    .width(Constants.SCROLL_BAR)
                    .offset { IntOffset(0, thumbOffsetPx.toInt()) }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), ShapesStyle.extraSmall)
            )
        }
    }
}