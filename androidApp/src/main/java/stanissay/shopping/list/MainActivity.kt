package stanissay.shopping.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.reorderable
import stanissay.shopping.common.MainColors
import stanissay.shopping.common.MainConstants
import stanissay.shopping.common.MainStrings
import stanissay.shopping.common.PaddingStyle
import stanissay.shopping.common.ShoppingItem
import stanissay.shopping.list.utils.AddBtn
import stanissay.shopping.list.utils.CheckBtn
import stanissay.shopping.list.utils.CircularBar
import stanissay.shopping.list.utils.ClickableBox
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.DeleteBtn
import stanissay.shopping.list.utils.DialogTitle
import stanissay.shopping.list.utils.DoneBtn
import stanissay.shopping.list.utils.EditBtn
import stanissay.shopping.list.utils.MainCard
import stanissay.shopping.list.utils.MainDivider
import stanissay.shopping.list.utils.MainLines
import stanissay.shopping.list.utils.MainText
import stanissay.shopping.list.utils.MainTextField
import stanissay.shopping.list.utils.MainTheme
import stanissay.shopping.list.utils.MinusBtn
import stanissay.shopping.list.utils.PageTitle
import stanissay.shopping.list.utils.PasteBtn
import stanissay.shopping.list.utils.PlusBtn
import stanissay.shopping.list.utils.VerticalScrollBar
import stanissay.shopping.list.utils.showCustomToast
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModelA by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isLoading by viewModel.isLoading.collectAsState()
            MainTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(PaddingStyle.sm)
                ) {
                    Column {
                        MainHead(viewModel)
                        MainList(viewModel)

                        if(viewModel.showItemDialog) ItemDialog(viewModel) { viewModel.hideItemDialog() }
                    }
                    if(isLoading) CircularBar()
                }
            }
        }
    }
}

@Composable
fun MainHead(viewModel: MainViewModelA) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PasteBtn {
            scope.launch {
                val text = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrBlank()) viewModel.pasteItems(text)
            }
        }
        PageTitle(MainStrings.HEAD)
        AddBtn { viewModel.showNewItemDialog() }
    }
}

@Composable
fun MainList(viewModel: MainViewModelA) {
    val haptic = LocalHapticFeedback.current
    var swipedItemId by remember { mutableStateOf<String?>(null) }
    val firstDoneIndex = viewModel.shoppingItems.indexOfFirst { it.isDone }
    val reorderState = org.burnoutcrew.reorderable.rememberReorderableLazyListState(
        onMove = { from, to ->
            viewModel.moveItem(from.index, to.index)
        },
        onDragEnd = { _, _ ->
            viewModel.onDragEnd()
        },
        canDragOver = { from, to ->
            val fromStep = viewModel.shoppingItems[from.index]
            val toStep = viewModel.shoppingItems[to.index]
            !fromStep.isDone && !toStep.isDone
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = null,
                indication = null
            ) { viewModel.toggleItemExpanded(null) }
    ) {
        LazyColumn(
            state = reorderState.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingStyle.horizontalSm)
                .reorderable(reorderState)
        ) {
            viewModel.shoppingItems.forEachIndexed { index, item ->
                item(key = item.id) {
                    ReorderableItem(
                        state = reorderState,
                        key = item.id
                    ) { isDragging ->
                        LaunchedEffect(isDragging) {
                            if (isDragging) haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )
                        }

                        Column {
                            if (index == firstDoneIndex && firstDoneIndex > 0) MainDivider()
                            ShoppingListItem(
                                item = item,
                                modifier = Modifier
                                    .padding(PaddingStyle.verticalXs)
                                    .detectReorderAfterLongPress(reorderState)
                                    .shadow(if (isDragging) MainConstants.ELEVATION else MainConstants.NULL),
                                isExpanded = viewModel.expandedItemId == item.id,
                                onExpand = { viewModel.toggleItemExpanded(item) },
                                onEdit = { viewModel.showEditItemDialog(item) },
                                onDelete = { viewModel.deleteItem(item) },
                                onCheck = { viewModel.toggleItemDone(item) },
                                onQuantity = { viewModel.updateQuantity(item, it) },
                                isSwiped = swipedItemId == item.id,
                                onSwipe = { swipedItemId = item.id },
                                onSwipeClosed = { swipedItemId = null }
                            )
                        }
                    }
                }
            }
        }
        VerticalScrollBar(listState = reorderState.listState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun ShoppingListItem(
    modifier: Modifier,
    item: ShoppingItem,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheck: () -> Unit,
    onQuantity: (Int) -> Unit,
    isSwiped: Boolean,
    onSwipe: () -> Unit,
    onSwipeClosed: () -> Unit
) {
    val quantity = item.quantity?.toIntOrNull()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val revealWidthPx = with(density) { (Constants.BASE_SIZE * 2).toPx() }
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    var itemWidthPx by remember { mutableFloatStateOf(0f) }
    var dragStartOffset by remember(item.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(item.id) { mutableFloatStateOf(0f) }

    fun animateOffset(target: Float, onFinished: (() -> Unit)? = null) {
        scope.launch {
            Animatable(offsetX).animateTo(
                targetValue = target,
                animationSpec = tween(MainConstants.ANIMATION_MS)
            ) { offsetX = value }
            onFinished?.invoke()
        }
    }

    fun animateCheck(onFinished: (() -> Unit)? = null) {
        val targetY = if (item.isDone) -itemWidthPx * 0.5f else itemWidthPx * 0.5f
        scope.launch {
            val xAnimation = launch {
                Animatable(offsetX).animateTo(
                    targetValue = 0f,
                    animationSpec = tween(MainConstants.ANIMATION_MS)
                ) { offsetX = value }
            }

            val yAnimation = launch {
                Animatable(offsetY).animateTo(
                    targetValue = targetY,
                    animationSpec = tween(MainConstants.ANIMATION_MS)
                ) { offsetY = value }
            }

            xAnimation.join()
            yAnimation.join()
            offsetX = 0f
            offsetY = 0f
            onFinished?.invoke()
        }
    }

    fun animateDelete(onFinished: (() -> Unit)? = null) {
        scope.launch {
            Animatable(offsetX).animateTo(
                targetValue = -itemWidthPx * 1.5f,
                animationSpec = tween(MainConstants.ANIMATION_MS)
            ) { offsetX = value }
            onFinished?.invoke()
        }
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) animateOffset(0f)
    }

    LaunchedEffect(isSwiped) {
        if (!isSwiped && offsetX != 0f) animateOffset(0f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        if (offsetX < 0f) {
            ClickableBox(
                modifier = Modifier.matchParentSize(),
                background = MainColors.NEGATIVE.copy(alpha = 0.5f),
                contentAlignment = Alignment.CenterEnd
            ) {
                DeleteBtn(modifier = Modifier.padding(PaddingStyle.horizontalXl)) {
                    animateDelete { onDelete() }
                }
            }
        }
        if (offsetX > 0f) {
            ClickableBox(
                modifier = Modifier.matchParentSize(),
                background = MainColors.POSITIVE.copy(alpha = 0.5f),
                contentAlignment = Alignment.CenterStart
            ) {
                DoneBtn(modifier = Modifier.padding(PaddingStyle.horizontalXl)) {
                    animateCheck { onCheck() }
                }
            }
        }
        MainCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .onSizeChanged { itemWidthPx = it.width.toFloat() }
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = !isExpanded,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(
                            -itemWidthPx,
                            itemWidthPx
                        )
                    },
                    onDragStarted = { dragStartOffset = offsetX },
                    onDragStopped = {
                        when {
                            dragStartOffset < 0f && offsetX > dragStartOffset -> {
                                onSwipeClosed()
                                animateOffset(0f)
                            }

                            dragStartOffset > 0f && offsetX < dragStartOffset -> {
                                onSwipeClosed()
                                animateOffset(0f)
                            }

                            offsetX <= -itemWidthPx * 0.75f ->
                                animateDelete {
                                    onSwipeClosed()
                                    onDelete()
                                }

                            offsetX >= itemWidthPx * 0.75f ->
                                animateCheck {
                                    onSwipeClosed()
                                    onCheck()
                                }

                            offsetX < -revealWidthPx / 2f -> {
                                onSwipe()
                                animateOffset(-revealWidthPx)
                            }

                            offsetX > revealWidthPx / 2f -> {
                                onSwipe()
                                animateOffset(revealWidthPx)
                            }

                            else -> {
                                onSwipe()
                                animateOffset(0f)
                            }
                        }
                    }
                ),
            borderColor = MaterialTheme.colorScheme.outline
        ) {
            MainLines(modifier = Modifier.matchParentSize())
            ClickableBox(
                onClick = {
                    if (offsetX != 0f) {
                        scope.launch {
                            animateOffset(0f)
                        }
                    } else onExpand()
                }
            ) {
                Column(modifier = Modifier.padding(PaddingStyle.horizontalXl)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Constants.BASE_SIZE),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if(isExpanded) CheckBtn(checked = item.isDone) { onCheck() } else Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE_S))
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(PaddingStyle.horizontalLg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MainText(
                                text = item.name,
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AnimatedVisibility(
                                visible = !isExpanded && !item.quantity.isNullOrBlank(),
                                enter = fadeIn(tween(MainConstants.ANIMATION_MS)) + expandHorizontally(tween(MainConstants.ANIMATION_MS)),
                                exit = fadeOut(tween(MainConstants.ANIMATION_MS)) + shrinkHorizontally(tween(MainConstants.ANIMATION_MS))
                            ) {
                                Row {
                                    MainText(
                                        text = " - ",
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    MainText(
                                        text = item.quantity.orEmpty(),
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(tween(MainConstants.ANIMATION_MS)) + expandHorizontally(tween(MainConstants.ANIMATION_MS)),
                            exit = fadeOut(tween(MainConstants.ANIMATION_MS)) + shrinkHorizontally(tween(MainConstants.ANIMATION_MS))
                        ) { EditBtn { onEdit() } }
                    }
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(tween(MainConstants.ANIMATION_MS)) + fadeIn(tween(MainConstants.ANIMATION_MS)),
                        exit = shrinkVertically(tween(MainConstants.ANIMATION_MS)) + fadeOut(tween(MainConstants.ANIMATION_MS))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Constants.BASE_SIZE),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            item.quantity?.takeIf { it.isNotBlank() }?.let {
                                quantity?.let { MinusBtn { onQuantity((quantity - 1).coerceAtLeast(1)) } }
                                    ?: Spacer(modifier = Modifier.width(Constants.BUTTON_SIZE_S))
                                MainText(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append(it) }
                                    },
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(PaddingStyle.horizontalLg)
                                )
                                quantity?.let { PlusBtn { onQuantity(quantity + 1) } }
                                    ?: Spacer(modifier = Modifier.width(Constants.BUTTON_SIZE_S))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn(tween(MainConstants.ANIMATION_MS)) + expandHorizontally(tween(MainConstants.ANIMATION_MS)),
                                exit = fadeOut(tween(MainConstants.ANIMATION_MS)) + shrinkHorizontally(tween(MainConstants.ANIMATION_MS))
                            ) { DeleteBtn { onDelete() } }
                        }
                    }
                }

                if (item.isDone) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(color = MainColors.BACKGROUND.copy(alpha = 0.5f))
                        clipRect(bottom = Constants.BASE_SIZE.toPx()) {
                            val random = kotlin.random.Random(item.id.hashCode())
                            val centerY = Constants.BASE_SIZE.toPx() / 2
                            val maxThickness = MainConstants.DONE_LINE.toPx()

                            val points = mutableListOf<Pair<Float, Float>>()
                            var x = 0f
                            var y = centerY

                            while (x < size.width) {
                                points.add(x to y)
                                x = (x + random.nextFloat() * 12.dp.toPx() + 8.dp.toPx()).coerceAtMost(size.width)
                                y = (y + random.nextFloat() * 2.dp.toPx() - 1.dp.toPx()).coerceIn(centerY - 2.dp.toPx(), centerY + 2.dp.toPx())
                            }
                            points.add(size.width to y)

                            val thicknesses = points.map {
                                maxThickness * (0.8f + random.nextFloat() * 0.4f)
                            }

                            val path = Path().apply {
                                points.forEachIndexed { index, (px, py) ->
                                    val half = thicknesses[index] / 2
                                    if (index == 0) moveTo(px, py - half)
                                    else lineTo(px, py - half)
                                }

                                points.indices.reversed().forEach { index ->
                                    val (px, py) = points[index]
                                    lineTo(px, py + thicknesses[index] / 2)
                                }

                                close()
                            }

                            drawPath(
                                path = path,
                                color = MainColors.GRAY.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDialog(viewModel: MainViewModelA, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val item = viewModel.editItem
    val name by remember { mutableStateOf(TextFieldState(item?.name ?: "")) }
    val quantity by remember { mutableStateOf(TextFieldState(item?.quantity ?: "1")) }
    val focusRequesterName = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequesterName.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        MainCard(borderColor = MaterialTheme.colorScheme.outline) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE),
                    contentAlignment = Alignment.Center
                ) { DialogTitle(MainStrings.TO_BUY) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MainTextField(
                        modifier = Modifier.weight(1f),
                        state = name,
                        placeholderText = MainStrings.NAME,
                        focusRequester = focusRequesterName,
                        imeAction = ImeAction.Next
                    )
                    MainTextField(
                        modifier = Modifier.width(Constants.BASE_SIZE_L),
                        state = quantity,
                        placeholderText = MainStrings.NULL,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE))
                    DoneBtn {
                        if (name.text.toString().isBlank()) {
                            showCustomToast(MainStrings.NO_NAME, context)
                            return@DoneBtn
                        }
                        val finalItem = ShoppingItem(
                            id = item?.id ?: "",
                            name = name.text.toString(),
                            quantity = quantity.text.toString().ifBlank { null },
                            order = item?.order ?: 0,
                            isDone = item?.isDone ?: false
                        )
                        viewModel.editItem(finalItem)

                        if (item == null) {
                            name.clearText()
                            quantity.clearText()
                            quantity.edit { append("1") }
                            focusRequesterName.requestFocus()
                        }
                    }
                }
            }
        }
    }
}