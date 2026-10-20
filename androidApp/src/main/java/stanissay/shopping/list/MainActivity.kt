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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import stanissay.shopping.common.MainColors
import stanissay.shopping.common.MainConstants
import stanissay.shopping.common.MainStrings
import stanissay.shopping.common.PaddingStyle
import stanissay.shopping.common.ShoppingCategory
import stanissay.shopping.common.ShoppingItem
import stanissay.shopping.list.utils.AddBtn
import stanissay.shopping.list.utils.AppEventManager
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
import stanissay.shopping.list.utils.TextDropdownMenu
import stanissay.shopping.list.utils.UiEvent
import stanissay.shopping.list.utils.VerticalScrollBar
import stanissay.shopping.list.utils.showCustomToast
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

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
                    MainList(viewModel)
                    if(isLoading) CircularBar()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainList(viewModel: MainViewModelA) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val categories = viewModel.shoppingCategories.sortedBy { it.order }
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activeCategory = categories.getOrNull(pagerState.currentPage)

    LaunchedEffect(Unit) {
        AppEventManager.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showCustomToast(event.message, context)
                    AppEventManager.resetReplayCache()
                }
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                    AppEventManager.resetReplayCache()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { PageTitle(MainStrings.HEAD) },
                actions = {
                    PasteBtn {
                        scope.launch {
                            val text = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()
                            if (!text.isNullOrBlank()) viewModel.pasteItems(text, activeCategory?.id)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            var dropdownExpanded by remember { mutableStateOf(false) }
            AddBtn(
                onClick = { viewModel.showNewItemDialog() },
                onLongClick = { dropdownExpanded = true }
            )
            TextDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                items = listOf(MainStrings.ADD_ELEMENT, MainStrings.ADD_CATEGORY),
                onItemClick = {
                    when(it) {
                        MainStrings.ADD_ELEMENT -> viewModel.showNewItemDialog()
                        MainStrings.ADD_CATEGORY -> viewModel.showNewCategoryDialog()
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (categories.isNotEmpty()) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val reorderState = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            viewModel.moveCategory(from.index, to.index)
                        },
                        onDragEnd = { _, to ->
                            scope.launch {
                                pagerState.animateScrollToPage(to)
                            }
                        },
                        canDragOver = { _, _ -> true }
                    )

                    LazyRow(
                        state = reorderState.listState,
                        modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large)
                            .clip(MaterialTheme.shapes.large)
                            .reorderable(reorderState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categories.forEachIndexed { index, category ->
                            val listSize = viewModel.shoppingItems.filter { it.categoryId == category.id && !it.isDone }.size.toString()
                            item(key = category.id) {
                                ReorderableItem(
                                    state = reorderState,
                                    key = category.id
                                ) { isDragging ->
                                    ClickableBox(
                                        modifier = Modifier.width(Constants.BASE_SIZE_L).fillMaxHeight()
                                            .detectReorderAfterLongPress(reorderState)
                                            .shadow(if (isDragging) MainConstants.ELEVATION else MainConstants.NULL),
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                        onDoubleClick = { viewModel.showEditCategoryDialog(category) }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(MainConstants.LINE)
                                                    .padding(PaddingStyle.horizontalXs)
                                                    .background(
                                                        color = if (category.id == activeCategory?.id) MaterialTheme.colorScheme.primary else MainColors.TRANSPARENT,
                                                        shape = MaterialTheme.shapes.extraSmall
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MainText(
                                                    text = "${category.name} ($listSize)",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = if (category.id == activeCategory?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (categories.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                ItemsList(
                    viewModel = viewModel,
                    categoryId = categories[page].id
                )
            }
        }
    }

    if(viewModel.showItemDialog) ItemDialog(viewModel, activeCategory?.id) { viewModel.hideItemDialog() }
    if(viewModel.showCategoryDialog) CategoryDialog(viewModel) { viewModel.hideCategoryDialog() }
}

@Composable
fun ItemsList(viewModel: MainViewModelA, categoryId: String) {
    val haptic = LocalHapticFeedback.current
    val items = viewModel.shoppingItems.filter { it.categoryId == categoryId }
    var swipedItemId by remember { mutableStateOf<String?>(null) }
    val firstDoneIndex = items.indexOfFirst { it.isDone }
    val currentItems by rememberUpdatedState(items)
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            viewModel.moveItem(categoryId, from.index, to.index)
        },
        onDragEnd = { _, _ ->
            viewModel.onDragEnd()
        },
        canDragOver = { from, to ->
            val current = currentItems
            val fromItem = current.getOrNull(from.index)
            val toItem = current.getOrNull(to.index)

            fromItem != null && toItem != null &&
                    !fromItem.isDone && !toItem.isDone
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
            items.forEachIndexed { index, item ->
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
                            val maxThickness = MainConstants.LINE.toPx()

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
fun ItemDialog(viewModel: MainViewModelA, categoryId: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val item = viewModel.editItem
    val name by remember { mutableStateOf(TextFieldState(item?.name ?: "")) }
    val quantity by remember { mutableStateOf(TextFieldState(item?.quantity ?: "1")) }
    val focusRequesterName = remember { FocusRequester() }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var suggestionSelected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequesterName.requestFocus()
    }

    LaunchedEffect(name.text) {
        dropdownExpanded = false
        suggestions = emptyList()

        if (suggestionSelected) {
            suggestionSelected = false
            return@LaunchedEffect
        }

        if (name.text.isNotBlank()) {
            delay(300.milliseconds)
            suggestions = viewModel.getNameSuggestions(name.text.toString())
            dropdownExpanded = suggestions.isNotEmpty()
        }
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        MainTextField(
                            state = name,
                            placeholderText = MainStrings.NAME,
                            focusRequester = focusRequesterName,
                            imeAction = ImeAction.Next
                        )
                        TextDropdownMenu(
                            modifier = Modifier.align(Alignment.Center),
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            items = suggestions,
                            onItemClick = { selectedName ->
                                suggestionSelected = true
                                name.setTextAndPlaceCursorAtEnd(selectedName)
                                dropdownExpanded = false
                            }
                        )
                    }
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
                        viewModel.editItem(finalItem, categoryId)

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

@Composable
fun CategoryDialog(viewModel: MainViewModelA, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val category = viewModel.editCategory
    val name by remember { mutableStateOf(TextFieldState(category?.name ?: "")) }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE)
                        .padding(PaddingStyle.horizontalSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE_S))
                    DialogTitle(MainStrings.CATEGORY)
                    if(viewModel.shoppingCategories.size > 1 && category?.id != null) DeleteBtn { viewModel.deleteCategory(category.id) }
                    else Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE_S))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE),
                    contentAlignment = Alignment.Center
                ) {
                    MainTextField(
                        state = name,
                        placeholderText = MainStrings.NAME,
                        focusRequester = focusRequesterName,
                        imeAction = ImeAction.Next
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
                        val finalCategory = ShoppingCategory(
                            id = category?.id ?: "",
                            name = name.text.toString(),
                            order = category?.order ?: 0
                        )
                        viewModel.editCategory(finalCategory)
                    }
                }
            }
        }
    }
}