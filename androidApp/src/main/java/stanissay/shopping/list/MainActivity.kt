package stanissay.shopping.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import stanissay.shopping.common.MainColors
import stanissay.shopping.common.MainConstants
import stanissay.shopping.common.MainStrings
import stanissay.shopping.common.PaddingStyle
import stanissay.shopping.common.ShoppingItem
import stanissay.shopping.list.utils.AddBtn
import stanissay.shopping.list.utils.CheckBtn
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
import stanissay.shopping.list.utils.PlusBtn
import stanissay.shopping.list.utils.showCustomToast

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModelA by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainTheme {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(PaddingStyle.sm)
                ) {
                    MainHead(viewModel)
                    MainList(viewModel)

                    if(viewModel.showItemDialog) ItemDialog(viewModel) { viewModel.hideItemDialog() }
                }
            }
        }
    }
}

@Composable
fun MainHead(viewModel: MainViewModelA) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageTitle(MainStrings.HEAD)
        AddBtn { viewModel.showNewItemDialog() }
    }
}

@Composable
fun MainList(viewModel: MainViewModelA) {
    val firstDoneIndex = viewModel.shoppingItems.indexOfFirst { it.isDone }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(PaddingStyle.xs)
            .clickable(
                interactionSource = null,
                indication = null
            ) { viewModel.toggleItemExpanded(null) },
        verticalArrangement = Arrangement.spacedBy(MainConstants.SPACE)
    ) {
        items(viewModel.shoppingItems.size) { index ->
            val item = viewModel.shoppingItems[index]
            if (index == firstDoneIndex && firstDoneIndex > 0) MainDivider()

            ShoppingListItem(
                item = item,
                isExpanded = viewModel.expandedItemId == item.id,
                onExpand = { viewModel.toggleItemExpanded(item) },
                onEdit = { viewModel.showEditItemDialog(item) },
                onDelete = { viewModel.deleteItem(item) },
                onCheck = { viewModel.toggleItemDone(item) }
            )
        }
    }
}

@Composable
fun ShoppingListItem(
    item: ShoppingItem,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheck: () -> Unit
) {
    MainCard(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(),
        borderColor = MaterialTheme.colorScheme.outline
    ) {
        MainLines(modifier = Modifier.matchParentSize())
        ClickableBox(onClick = { onExpand() }) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE)
                        .padding(PaddingStyle.horizontalLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckBtn(
                        checked = item.isDone
                    ) { onCheck() }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        MainText(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(item.name)
                                }

                                append(" - ")

                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    append(item.quantity ?: "")
                                }
                            },
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(PaddingStyle.horizontalLg)
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(120))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        PlusBtn { }
                        MinusBtn { }
                        EditBtn { onEdit() }
                        DeleteBtn { onDelete() }
                    }
                }
            }

            if (item.isDone) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(color = MainColors.BACKGROUND.copy(alpha = 0.5f))
                    clipRect(bottom = Constants.BASE_SIZE.toPx()) {
                        val random = kotlin.random.Random(item.id.hashCode())
                        val path = Path().apply {
                            val centerY = Constants.BASE_SIZE.toPx() / 2
                            moveTo(0f, centerY)
                            var x = 0f
                            var y = centerY

                            while (x < size.width) {
                                x += random.nextFloat() * 12.dp.toPx() + 8.dp.toPx()
                                y += random.nextFloat() * 2.dp.toPx() - 1.dp.toPx()

                                lineTo(
                                    x.coerceAtMost(size.width),
                                    y.coerceIn(centerY - 2.dp.toPx(), centerY + 2.dp.toPx())
                                )
                            }
                        }

                        drawPath(
                            path = path,
                            color = MainColors.GRAY.copy(alpha = 0.7f),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
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
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
                    contentAlignment = Alignment.Center
                ) { DialogTitle(MainStrings.TO_BUY) }
                Row(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MainTextField(
                        modifier = Modifier.width(Constants.BASE_SIZE_XL).weight(1f),
                        state = name,
                        placeholderText = MainStrings.NAME,
                        focusRequester = focusRequesterName,
                        imeAction = ImeAction.Next
                    )
                    MainTextField(
                        modifier = Modifier.width(Constants.BASE_SIZE_XL),
                        state = quantity,
                        placeholderText = MainStrings.NULL,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
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