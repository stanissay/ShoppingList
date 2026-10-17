package stanissay.shopping.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import stanissay.shopping.common.MainColors
import stanissay.shopping.common.MainConstants
import stanissay.shopping.common.MainStrings
import stanissay.shopping.common.PaddingStyle
import stanissay.shopping.common.ShoppingItem
import stanissay.shopping.list.utils.AddBtn
import stanissay.shopping.list.utils.CheckBtn
import stanissay.shopping.list.utils.ClickableBox
import stanissay.shopping.list.utils.CloseBtn
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.DeleteBtn
import stanissay.shopping.list.utils.DialogTitle
import stanissay.shopping.list.utils.DoneBtn
import stanissay.shopping.list.utils.EditBtn
import stanissay.shopping.list.utils.MainCard
import stanissay.shopping.list.utils.MainDivider
import stanissay.shopping.list.utils.MainText
import stanissay.shopping.list.utils.MainTextField
import stanissay.shopping.list.utils.MainTheme
import stanissay.shopping.list.utils.PageTitle
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
        Spacer(modifier = Modifier.size(Constants.BASE_SIZE))
        PageTitle(MainStrings.HEAD)
        AddBtn { viewModel.showNewItemDialog() }
    }
}

@Composable
fun MainList(viewModel: MainViewModelA) {
    val firstDoneIndex = viewModel.shoppingItems.indexOfFirst { it.isDone }

    MainCard(
        borderColor = MaterialTheme.colorScheme.outline,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MainConstants.SPACE)
        ) {
            items(viewModel.shoppingItems.size) { index ->
                if (index == firstDoneIndex && firstDoneIndex > 0) MainDivider()

                val item = viewModel.shoppingItems[index]

                ShoppingListItem(
                    item = item,
                    onEdit = { viewModel.showEditItemDialog(item) },
                    onDelete = { viewModel.deleteItem(item) },
                    onCheck = { viewModel.toggleItemDone(item) }
                )
            }
        }
    }
}

@Composable
fun ShoppingListItem(
    item: ShoppingItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheck: () -> Unit
) {
    MainCard(modifier = Modifier.fillMaxWidth()) {
        ClickableBox(
            onClick = { onEdit() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Constants.BASE_SIZE)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
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

                    Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE_S))
                    EditBtn { onEdit() }
                    DeleteBtn { onDelete() }
                }

                if (item.isDone) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = MainColors.BACKGROUND.copy(alpha = 0.4f)
                        )

                        drawLine(
                            color = MainColors.GRAY.copy(alpha = 0.6f),
                            start = Offset(
                                x = 0.dp.toPx(),
                                y = size.height / 2
                            ),
                            end = Offset(
                                x = size.width,
                                y = size.height / 2
                            ),
                            strokeWidth = Constants.THICKNESS.toPx()
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
                Box(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
                    contentAlignment = Alignment.Center
                ) {
                    MainTextField(
                        state = name,
                        placeholderText = MainStrings.NAME,
                        focusRequester = focusRequesterName,
                        imeAction = ImeAction.Next
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
                    contentAlignment = Alignment.Center
                ) {
                    MainTextField(
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
                    CloseBtn { onDismiss() }
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
                    }
                }
            }
        }
    }
}