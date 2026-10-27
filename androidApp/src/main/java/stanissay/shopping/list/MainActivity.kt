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

package stanissay.shopping.list

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import stanissay.shopping.list.utils.AddBtn
import stanissay.shopping.list.utils.CaptionText
import stanissay.shopping.list.utils.CheckBtn
import stanissay.shopping.list.utils.CircularBar
import stanissay.shopping.list.utils.ClickableBox
import stanissay.shopping.list.utils.CloseBtn
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.DeleteBtn
import stanissay.shopping.list.utils.DialogTitle
import stanissay.shopping.list.utils.DoneBtn
import stanissay.shopping.list.utils.EditBtn
import stanissay.shopping.list.utils.GoogleBtn
import stanissay.shopping.list.utils.MainCard
import stanissay.shopping.list.utils.MainDivider
import stanissay.shopping.list.utils.MainLines
import stanissay.shopping.list.utils.MainText
import stanissay.shopping.list.utils.MainTextField
import stanissay.shopping.list.utils.MainTheme
import stanissay.shopping.list.utils.MainViewModelFactory
import stanissay.shopping.list.utils.MinusBtn
import stanissay.shopping.list.utils.PageTitle
import stanissay.shopping.list.utils.PasteBtn
import stanissay.shopping.list.utils.PlusBtn
import stanissay.shopping.list.utils.SelectionText
import stanissay.shopping.list.utils.TextDropdownMenu
import stanissay.shopping.list.utils.VerticalScrollBar
import stanissay.shopping.list.utils.WatchBtn
import stanissay.shopping.shared.AndroidStringProvider
import stanissay.shopping.shared.AppEventManager
import stanissay.shopping.shared.Config
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.GoogleAuth
import stanissay.shopping.shared.MainColors
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.PaddingStyle
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ShoppingCategory
import stanissay.shopping.shared.ShoppingItem
import stanissay.shopping.shared.UiEvent
import stanissay.shopping.shared.showCustomToast
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModelA by viewModels {
        MainViewModelFactory(application, AndroidStringProvider(applicationContext))
    }
    private var requestedCategoryId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            requestedCategoryId = intent?.getStringExtra(DataKeys.CATEGORY_ID)
        }
        setContent {
            val isLoading by viewModel.sharedViewModel.isLoading.collectAsState()
            MainTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(PaddingStyle.xs)
                ) {
                    MainList(
                        viewModel = viewModel,
                        requestedCategoryId = requestedCategoryId,
                        onCategoryHandled = { requestedCategoryId = null }
                    )
                    if(isLoading) CircularBar()
                }
            }
        }
        startSyncService()
        ensureNotificationPermission()
        ensureBatteryOptimizationExemption()
    }

    override fun onStart() {
        super.onStart()
        NotificationManagerCompat.from(this).cancel(DataKeys.NEW_ITEMS_NOTIFICATION_ID)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedCategoryId = intent.getStringExtra(DataKeys.CATEGORY_ID)
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startSyncService() }

    private fun ensureNotificationPermission() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return
        val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
        if (prefs.getBoolean(DataKeys.NOTIFICATIONS_PERMISSION_ASKED, false)) return
        prefs.edit { putBoolean(DataKeys.NOTIFICATIONS_PERMISSION_ASKED, true) }
        notificationPermissionLauncher.launch(permission)
    }

    private fun startSyncService() {
        if (Firebase.auth.currentUser == null) return
        startForegroundService(Intent(this, SyncService::class.java).setAction(DataKeys.ACTION_CONNECT_NOW))
    }

    @SuppressLint("BatteryLife")
    private fun ensureBatteryOptimizationExemption() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName
        val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
        val alreadyAsked = prefs.getBoolean(DataKeys.BATTERY_EXEMPTION_ASKED, false)
        if (alreadyAsked) return

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle(this.getString(R.string.work_in_back))
                .setMessage(this.getString(R.string.work_in_back_message))
                .setPositiveButton(this.getString(R.string.go_to_settings)) { _, _ ->
                    val intent = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData("package:$packageName".toUri())
                    startActivity(intent)
                    prefs.edit { putBoolean(DataKeys.BATTERY_EXEMPTION_ASKED, true) }
                }
                .setNegativeButton(this.getString(R.string.close)) { _, _ ->
                    prefs.edit { putBoolean(DataKeys.BATTERY_EXEMPTION_ASKED, true) }
                }
                .show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainList(
    viewModel: MainViewModelA,
    requestedCategoryId: String? = null,
    onCategoryHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
    val snackbarHostState = remember { SnackbarHostState() }
    val categories = viewModel.sharedViewModel.shoppingCategories
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val activeCategory = categories.getOrNull(pagerState.currentPage)
    var versionExpanded by remember { mutableStateOf(false) }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            viewModel.sharedViewModel.moveCategory(from.index, to.index)
        },
        onDragEnd = { _, to ->
            scope.launch {
                viewModel.updateCategoriesOrder()
                pagerState.animateScrollToPage(to)
            }
        },
        canDragOver = { _, _ -> true }
    )

    LaunchedEffect(requestedCategoryId, categories) {
        val id = requestedCategoryId ?: return@LaunchedEffect
        if (categories.isEmpty()) return@LaunchedEffect
        val index = categories.indexOfFirst { it.id == id }
        if (index >= 0) pagerState.scrollToPage(index)
        onCategoryHandled()
    }

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

    LaunchedEffect(pagerState, categories) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val category = categories.getOrNull(page) ?: return@collect
                val index = categories.indexOfFirst { it.id == category.id }
                if (index < 0) return@collect
                val listState = reorderState.listState
                val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == category.id }
                val viewport = listState.layoutInfo
                val isFullyVisible = itemInfo != null && itemInfo.offset >= viewport.viewportStartOffset &&
                        itemInfo.offset + itemInfo.size <= viewport.viewportEndOffset
                if (!isFullyVisible) listState.animateScrollToItem(index)
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary,
                    subtitleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    ClickableBox(
                        onLongClick = { versionExpanded = !versionExpanded }
                    ) {
                        PageTitle(
                            text = stringResource(R.string.app_name),
                            modifier = Modifier.padding(PaddingStyle.xs)
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = versionExpanded,
                        enter = fadeIn(tween(MainConstants.ANIMATION_MS)) + expandHorizontally(tween(MainConstants.ANIMATION_MS)),
                        exit = fadeOut(tween(MainConstants.ANIMATION_MS)) + shrinkHorizontally(tween(MainConstants.ANIMATION_MS))
                    ) {
                        CaptionText(
                            modifier = Modifier.padding(PaddingStyle.horizontalS),
                            text = "${stringResource(R.string.version)} $versionName"
                        )
                    }
                    if(viewModel.sharedViewModel.watchHasApp) WatchBtn { viewModel.showWatchDialog() }
                    if(viewModel.sharedViewModel.shoppingCategories.isNotEmpty()) {
                        PasteBtn {
                            scope.launch {
                                val text = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()
                                if (!text.isNullOrBlank()) viewModel.sharedViewModel.pasteItems(text, activeCategory?.id)
                            }
                        }
                    }
                    if(!viewModel.sharedViewModel.isLoggedIn) {
                        GoogleBtn {
                            scope.launch {
                                val result = GoogleAuth(activity).signIn()
                                result.onSuccess {
                                    viewModel.login()
                                    ContextCompat.startForegroundService(
                                        context,
                                        Intent(context, SyncService::class.java).setAction(DataKeys.ACTION_CONNECT_NOW)
                                    )
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar(
                                        message = error.message.toString(),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    } else {
                        AsyncImage(
                            model = viewModel.sharedViewModel.userPhotoUrl,
                            contentDescription = viewModel.sharedViewModel.userName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(Constants.BUTTON_SIZE)
                                .clip(CircleShape)
                                .clickable(onClick = { viewModel.sharedViewModel.showLogoutDialog = true })
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if(viewModel.sharedViewModel.isLoggedIn) {
                var dropdownExpanded by remember { mutableStateOf(false) }
                AddBtn(
                    onClick = {
                        if (viewModel.sharedViewModel.shoppingCategories.isNotEmpty()) {
                            viewModel.sharedViewModel.showNewItemDialog()
                        } else dropdownExpanded = true
                    },
                    onLongClick = { dropdownExpanded = true }
                )
                TextDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    items =
                        if (viewModel.sharedViewModel.shoppingCategories.isNotEmpty()) {
                            listOf(stringResource(R.string.add_item), stringResource(R.string.add_category))
                        } else listOf(stringResource(R.string.add_category)),
                    onItemClick = {
                        when (it) {
                            viewModel.strings.getString(R.string.add_item) -> viewModel.sharedViewModel.showNewItemDialog()
                            viewModel.strings.getString(R.string.add_category) -> viewModel.sharedViewModel.showNewCategoryDialog()
                        }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier.padding(PaddingStyle.xs),
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        actionColor = MaterialTheme.colorScheme.primary,
                        dismissActionContentColor = MaterialTheme.colorScheme.primary,
                        snackbarData = data
                    )
                }
            )
        },
        bottomBar = {
            if (categories.isNotEmpty()) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                    LazyRow(
                        state = reorderState.listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Constants.BASE_SIZE)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.large
                            )
                            .clip(MaterialTheme.shapes.large)
                            .reorderable(reorderState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categories.forEachIndexed { index, category ->
                            val listSize = viewModel.sharedViewModel.shoppingItems.filter { it.categoryId == category.id && !it.done }.size.toString()
                            item(key = category.id) {
                                ReorderableItem(
                                    state = reorderState,
                                    key = category.id
                                ) { isDragging ->
                                    LaunchedEffect(isDragging) {
                                        if (isDragging) haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    }

                                    ClickableBox(
                                        background = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .width(Constants.BASE_SIZE_L)
                                            .fillMaxHeight()
                                            .shadow(elevation = if (isDragging) MainConstants.ELEVATION else MainConstants.NULL)
                                            .detectReorderAfterLongPress(reorderState),
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        onDoubleClick = { viewModel.sharedViewModel.showEditCategoryDialog(category) }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(MainConstants.LINE)
                                                    .padding(PaddingStyle.horizontalS)
                                                    .background(
                                                        color = if (category.id == activeCategory?.id) MaterialTheme.colorScheme.primary else MainColors.TRANSPARENT,
                                                        shape = MaterialTheme.shapes.extraSmall
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MainText(
                                                    text = "${category.name} ($listSize)",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = if (category.id == activeCategory?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

    if(viewModel.sharedViewModel.showItemDialog) ItemDialog(viewModel, activeCategory?.id) { viewModel.sharedViewModel.hideItemDialog() }
    if(viewModel.sharedViewModel.showCategoryDialog) CategoryDialog(viewModel) { viewModel.sharedViewModel.hideCategoryDialog() }
    if(viewModel.sharedViewModel.showLogoutDialog) LogoutDialog(viewModel) { viewModel.sharedViewModel.showLogoutDialog = false }
    if(viewModel.watchDialogState) WatchDialog(viewModel)
}

@Composable
fun ItemsList(viewModel: MainViewModelA, categoryId: String) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val items = viewModel.sharedViewModel.shoppingItems.filter { it.categoryId == categoryId }
    var swipedItemId by remember { mutableStateOf<String?>(null) }
    val firstDoneIndex = items.indexOfFirst { it.done }
    val currentItems by rememberUpdatedState(items)
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to ->
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset

            viewModel.sharedViewModel.moveItem(categoryId, from.index, to.index)

            if (from.index == firstIndex || to.index == firstIndex) {
                scope.launch {
                    listState.requestScrollToItem(firstIndex, firstOffset)
                }
            }
        },
        onDragEnd = { _, _ ->
            viewModel.updateItemsOrder()
        },
        canDragOver = { from, to ->
            val current = currentItems
            val fromItem = current.getOrNull(from.index)
            val toItem = current.getOrNull(to.index)

            fromItem != null && toItem != null &&
                    !fromItem.done && !toItem.done
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = null,
                indication = null
            ) { viewModel.sharedViewModel.toggleItemExpanded(null) }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingStyle.horizontalS)
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
                                isExpanded = viewModel.sharedViewModel.expandedItemId == item.id,
                                onExpand = { viewModel.sharedViewModel.toggleItemExpanded(item) },
                                onEdit = { viewModel.sharedViewModel.showEditItemDialog(item) },
                                onDelete = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.sharedViewModel.deleteItem(item)
                                },
                                onCheck = {
                                    Log.d(
                                        "ShoppingSwipe",
                                        "onCheck: id=${item.id}, done=${item.done}"
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.sharedViewModel.toggleItemDone(item)
                                },
                                onQuantity = { viewModel.sharedViewModel.updateQuantity(item, it) },
                                isSwiped = swipedItemId == item.id,
                                onSwipe = { swipedItemId = item.id },
                                onSwipeClosed = { swipedItemId = null }
                            )
                        }
                    }
                }
            }
        }
        VerticalScrollBar(listState = listState, modifier = Modifier.align(Alignment.CenterEnd))
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentItem by rememberUpdatedState(item)
    val currentOnCheck by rememberUpdatedState(onCheck)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnSwipeClosed by rememberUpdatedState(onSwipeClosed)
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
        val targetY = if (currentItem.done) -itemWidthPx * 0.5f else itemWidthPx * 0.5f
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
                targetValue = itemWidthPx * 1.5f,
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
        if (offsetX > 0f) {
            ClickableBox(
                modifier = Modifier.matchParentSize(),
                background = MainColors.NEGATIVE.copy(alpha = 0.5f),
                contentAlignment = Alignment.CenterStart
            ) {
                DeleteBtn(modifier = Modifier.padding(PaddingStyle.horizontalXl)) {
                    animateDelete {
                        onSwipeClosed()
                        onDelete()
                    }
                }
            }
        }
        if (offsetX < 0f) {
            ClickableBox(
                modifier = Modifier.matchParentSize(),
                background = MainColors.POSITIVE.copy(alpha = 0.5f),
                contentAlignment = Alignment.CenterEnd
            ) {
                DoneBtn(modifier = Modifier.padding(PaddingStyle.horizontalXl)) {
                    animateCheck {
                        onSwipeClosed()
                        onCheck()
                    }
                }
            }
        }
        MainCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .onSizeChanged { itemWidthPx = it.width.toFloat() }
                .pointerInput(item.id, isExpanded, itemWidthPx, isSwiped) {
                    if (isExpanded) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        if (itemWidthPx <= 0f) return@awaitEachGesture
                        val edge = itemWidthPx * 0.25f
                        val fullSwipe = itemWidthPx * 0.5f
                        val openedAtStart = isSwiped || offsetX != 0f
                        val startedFromLeft = startX <= edge
                        val startedFromRight = startX >= itemWidthPx - edge
                        if (!openedAtStart && !startedFromLeft && !startedFromRight) return@awaitEachGesture
                        dragStartOffset = offsetX

                        val firstDrag =
                            awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                                val movingRight = over > 0f
                                val allowed = openedAtStart ||
                                        (startedFromLeft && movingRight) ||
                                        (startedFromRight && !movingRight)

                                if (allowed) {
                                    change.consume()
                                    offsetX = (offsetX + over).coerceIn(
                                        -itemWidthPx, itemWidthPx
                                    )
                                }
                            }

                        if (firstDrag == null) return@awaitEachGesture

                        val completed = horizontalDrag(firstDrag.id) { change ->
                            val delta = change.position.x - change.previousPosition.x

                            offsetX = when {
                                dragStartOffset > 0f ->
                                    (offsetX + delta).coerceIn(0f, itemWidthPx)

                                dragStartOffset < 0f ->
                                    (offsetX + delta).coerceIn(-itemWidthPx, 0f)

                                else -> (offsetX + delta).coerceIn(-itemWidthPx, itemWidthPx)
                            }

                            change.consume()
                        }

                        if (!completed) {
                            currentOnSwipeClosed()
                            animateOffset(0f)
                            return@awaitEachGesture
                        }

                        when {
                            dragStartOffset < 0f && offsetX > dragStartOffset -> {
                                currentOnSwipeClosed()
                                animateOffset(0f)
                            }

                            dragStartOffset > 0f && offsetX < dragStartOffset -> {
                                currentOnSwipeClosed()
                                animateOffset(0f)
                            }

                            offsetX <= -fullSwipe -> {
                                animateCheck {
                                    currentOnSwipeClosed()
                                    currentOnCheck()
                                }
                            }

                            offsetX >= fullSwipe ->
                                animateDelete {
                                    currentOnSwipeClosed()
                                    currentOnDelete()
                                }

                            offsetX < -revealWidthPx / 2f -> {
                                currentOnSwipe()
                                animateOffset(-revealWidthPx)
                            }

                            offsetX > revealWidthPx / 2f -> {
                                currentOnSwipe()
                                animateOffset(revealWidthPx)
                            }

                            else -> {
                                currentOnSwipeClosed()
                                animateOffset(0f)
                            }
                        }
                    }
                },
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
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(tween(MainConstants.ANIMATION_MS)) + expandHorizontally(tween(MainConstants.ANIMATION_MS)),
                            exit = fadeOut(tween(MainConstants.ANIMATION_MS)) + shrinkHorizontally(tween(MainConstants.ANIMATION_MS))
                        ) { CheckBtn(checked = item.done) { onCheck() } }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(PaddingStyle.horizontalS),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            MainText(
                                text = item.name,
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.height(Constants.BASE_SIZE),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            item.quantity?.takeIf { it.isNotBlank() }?.let {
                                val quantity = it.toIntOrNull()
                                quantity?.let { q ->
                                    Box(
                                        modifier = Modifier.width(Constants.BUTTON_SIZE_S),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = fadeIn(tween(MainConstants.ANIMATION_MS)) + expandHorizontally(tween(MainConstants.ANIMATION_MS)),
                                            exit = fadeOut(tween(MainConstants.ANIMATION_MS)) + shrinkHorizontally(tween(MainConstants.ANIMATION_MS))
                                        ) { MinusBtn { onQuantity((q - 1).coerceAtLeast(1)) } }
                                    }
                                }
                                Box(
                                    modifier = Modifier.width(
                                        if(quantity != null) Constants.BASE_SIZE else Constants.BASE_SIZE + Constants.BUTTON_SIZE_S * 2
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MainText(
                                        text = it,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                quantity?.let { q ->
                                    Box(
                                        modifier = Modifier.width(Constants.BUTTON_SIZE_S),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = fadeIn(tween(MainConstants.ANIMATION_MS)) + expandHorizontally(tween(MainConstants.ANIMATION_MS)),
                                            exit = fadeOut(tween(MainConstants.ANIMATION_MS)) + shrinkHorizontally(tween(MainConstants.ANIMATION_MS))
                                        ) { PlusBtn { onQuantity(q + 1) } }
                                    }
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(tween(MainConstants.ANIMATION_MS)) + fadeIn(tween(MainConstants.ANIMATION_MS)),
                        exit = shrinkVertically(tween(MainConstants.ANIMATION_MS)) + fadeOut(tween(MainConstants.ANIMATION_MS))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (item.note.isNotBlank()) {
                                SelectionText(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(
                                            min = Constants.BASE_SIZE,
                                            max = Constants.BASE_SIZE_L
                                        )
                                        .padding(PaddingStyle.xl)
                                        .verticalScroll(rememberScrollState()),
                                    text = item.note
                                )
                            } else Spacer(modifier = Modifier
                                .weight(1f)
                                .height(Constants.BASE_SIZE))
                            Row(
                                modifier = Modifier.height(Constants.BASE_SIZE),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditBtn { onEdit() }
                                Spacer(modifier = Modifier.size(Constants.BASE_SIZE))
                                DeleteBtn { onDelete() }
                            }
                        }
                    }
                }

                if (item.done) {
                    val endPadding by animateDpAsState(
                        targetValue = if (isExpanded) {
                            Constants.BUTTON_SIZE_S * 2 + Constants.BASE_SIZE + MainConstants.LINE * 4
                        } else MainConstants.LINE * 4,
                        animationSpec = tween(MainConstants.ANIMATION_MS),
                        label = "endPadding"
                    )

                    val randomOffsets = remember(item.id) {
                        val random = kotlin.random.Random(item.id.hashCode())
                        List(500) { random.nextFloat() to random.nextFloat() }
                    }

                    val randomThicknesses = remember(item.id) {
                        val random = kotlin.random.Random(item.id.hashCode() + 1)
                        List(500) { 0.8f + random.nextFloat() * 0.4f }
                    }

                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(color = MainColors.BACKGROUND.copy(alpha = 0.5f))

                        clipRect(bottom = Constants.BASE_SIZE.toPx()) {
                            val centerY = Constants.BASE_SIZE.toPx() / 2
                            val maxThickness = MainConstants.LINE.toPx()
                            val startPaddingPx = maxThickness * 4
                            val endPaddingPx = with(density) { endPadding.toPx() }
                            val stepMin = 8.dp.toPx()
                            val stepRandom = 12.dp.toPx()
                            val yRange = 2.dp.toPx()
                            val points = mutableListOf<Pair<Float, Float>>()
                            val thicknesses = mutableListOf<Float>()
                            var x = startPaddingPx
                            var y = centerY
                            var index = 0

                            points.add(x to y)
                            thicknesses.add(maxThickness * randomThicknesses[index++])

                            while (true) {
                                val nextX = x + stepMin + randomOffsets[index].first * stepRandom

                                if (nextX >= size.width - endPaddingPx) break

                                x = nextX
                                y = (y + randomOffsets[index].second * 2f * yRange - yRange)
                                    .coerceIn(centerY - yRange, centerY + yRange)

                                points.add(x to y)
                                thicknesses.add(maxThickness * randomThicknesses[index])
                                index++
                            }

                            val endX = (size.width - endPaddingPx).coerceAtLeast(startPaddingPx)
                            val endY = if (points.size > 1) points.last().second else centerY

                            points.add(endX to endY)
                            thicknesses.add(maxThickness * randomThicknesses[index.coerceAtMost(499)])

                            val path = Path().apply {
                                val first = points.first()
                                val last = points.last()
                                val firstHalf = thicknesses.first() / 2
                                val lastHalf = thicknesses.last() / 2

                                moveTo(first.first, first.second - firstHalf)
                                points.forEachIndexed { i, (px, py) ->
                                    if (i > 0) {
                                        lineTo(px, py - thicknesses[i] / 2)
                                    }
                                }

                                arcTo(
                                    rect = Rect(
                                        last.first - lastHalf,
                                        last.second - lastHalf,
                                        last.first + lastHalf,
                                        last.second + lastHalf
                                    ),
                                    startAngleDegrees = -90f,
                                    sweepAngleDegrees = 180f,
                                    forceMoveTo = false
                                )

                                points.indices.reversed().forEach { i ->
                                    val (px, py) = points[i]
                                    lineTo(px, py + thicknesses[i] / 2)
                                }

                                arcTo(
                                    rect = Rect(
                                        first.first - firstHalf,
                                        first.second - firstHalf,
                                        first.first + firstHalf,
                                        first.second + firstHalf
                                    ),
                                    startAngleDegrees = 90f,
                                    sweepAngleDegrees = 180f,
                                    forceMoveTo = false
                                )

                                close()
                            }

                            drawPath(
                                path = path,
                                color = MainColors.THIN_BORDER.copy(alpha = 0.7f)
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
    val item = viewModel.sharedViewModel.editItem
    val name by remember { mutableStateOf(TextFieldState(item?.name ?: "")) }
    val quantity by remember { mutableStateOf(TextFieldState(item?.quantity ?: "1")) }
    val note by remember { mutableStateOf(TextFieldState(item?.note ?: "")) }
    val focusRequesterName = remember { FocusRequester() }
    val focusRequesterQuantity = remember { FocusRequester() }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var suggestionSelected by remember { mutableStateOf(false) }
    var isInitialValue by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        focusRequesterName.requestFocus()
    }

    LaunchedEffect(name.text) {
        dropdownExpanded = false
        suggestions = emptyList()

        if (isInitialValue) {
            isInitialValue = false
            return@LaunchedEffect
        }

        if (suggestionSelected) {
            suggestionSelected = false
            return@LaunchedEffect
        }

        if (name.text.isNotBlank()) {
            delay(300.milliseconds)
            suggestions = viewModel.sharedViewModel.getNameSuggestions(name.text.toString())
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
                ) { DialogTitle(stringResource(R.string.buy)) }
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
                            placeholderText = stringResource(R.string.name),
                            focusRequester = focusRequesterName,
                            imeAction = ImeAction.Next,
                            onKeyboardAction = {
                                quantity.clearText()
                                focusRequesterQuantity.requestFocus()
                            }
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
                        placeholderText = stringResource(R.string.null_str),
                        focusRequester = focusRequesterQuantity,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                }
                MainCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE_L),
                    contentPadding = PaddingStyle.xs,
                    containerPadding = PaddingStyle.xl,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    MainTextField(
                        modifier = Modifier.fillMaxSize(),
                        state = note,
                        placeholderText = stringResource(R.string.note),
                        keyboardType = KeyboardType.Text,
                        lineLimits = TextFieldLineLimits.Default,
                        textAlign = TextAlign.Start,
                        alignment = Alignment.TopStart
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
                            showCustomToast(viewModel.strings.getString(R.string.no_name), context)
                            return@DoneBtn
                        }
                        val finalItem = ShoppingItem(
                            id = item?.id ?: "",
                            name = name.text.toString(),
                            quantity = quantity.text.toString().ifBlank { null },
                            note = note.text.toString(),
                            order = item?.order ?: 0
                        )
                        viewModel.sharedViewModel.editItem(finalItem, categoryId)

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
    val category = viewModel.sharedViewModel.editCategory
    val items = viewModel.sharedViewModel.shoppingItems.filter { it.id == category?.id }
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
                        .padding(PaddingStyle.horizontalS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE_S))
                    DialogTitle(stringResource(R.string.category))
                    if(viewModel.sharedViewModel.shoppingCategories.size > 1 && category?.id != null && items.isEmpty()) {
                        DeleteBtn { viewModel.sharedViewModel.deleteCategory(category.id) }
                    } else Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE_S))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE),
                    contentAlignment = Alignment.Center
                ) {
                    MainTextField(
                        state = name,
                        placeholderText = stringResource(R.string.name),
                        focusRequester = focusRequesterName,
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
                            showCustomToast(viewModel.strings.getString(R.string.no_name), context)
                            return@DoneBtn
                        }
                        val finalCategory = ShoppingCategory(
                            id = category?.id ?: "",
                            name = name.text.toString(),
                            order = category?.order ?: 0
                        )
                        viewModel.sharedViewModel.editCategory(finalCategory)
                    }
                }
            }
        }
    }
}

@Composable
fun LogoutDialog(viewModel: MainViewModelA, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

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
                ) { DialogTitle(stringResource(R.string.logout)) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CloseBtn { onDismiss() }
                    DoneBtn {
                        scope.launch {
                            activity.stopService(Intent(activity, SyncService::class.java))
                            GoogleAuth(activity).signOut()
                            viewModel.logout()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchDialog(viewModel: MainViewModelA) {
    val wearVersion = Config.wearVersion

    Dialog(onDismissRequest = { viewModel.hideWatchDialog() }) {
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
                ) { DialogTitle(stringResource(R.string.watch)) }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    MainText(
                        "${stringResource(R.string.version)}: ${wearVersion ?: stringResource(R.string.unknown_version)}"
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.BASE_SIZE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CloseBtn { viewModel.hideWatchDialog() }
                    Spacer(modifier = Modifier.size(Constants.BUTTON_SIZE))
                }
            }
        }
    }
}