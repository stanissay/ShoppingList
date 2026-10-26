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
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.timeTextCurvedText
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import stanissay.shopping.list.utils.CircularBar
import stanissay.shopping.list.utils.ClickableBox
import stanissay.shopping.list.utils.CloseBtn
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.DialogTitle
import stanissay.shopping.list.utils.DoneBtn
import stanissay.shopping.list.utils.GoogleBtn
import stanissay.shopping.list.utils.MainCard
import stanissay.shopping.list.utils.MainLines
import stanissay.shopping.list.utils.MainText
import stanissay.shopping.list.utils.MainTheme
import stanissay.shopping.list.utils.MainViewModelFactory
import stanissay.shopping.list.utils.PageTitle
import stanissay.shopping.list.utils.transformedItem
import stanissay.shopping.shared.AndroidStringProvider
import stanissay.shopping.shared.AppEventManager
import stanissay.shopping.shared.DataKeys
import stanissay.shopping.shared.GoogleAuth
import stanissay.shopping.shared.MainColors
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.PaddingStyle
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ShoppingItem
import stanissay.shopping.shared.UiEvent
import stanissay.shopping.shared.showCustomToast
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModelW by viewModels {
        MainViewModelFactory(application, AndroidStringProvider(applicationContext))
    }
    private lateinit var ongoingActivityManager: OngoingManager
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) ongoingActivityManager.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        ongoingActivityManager = OngoingManager(this)
        prepareOngoingActivity()
        setContent {
            MainTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val ambientState = remember { mutableStateOf(false) }
                val isLoading by viewModel.sharedViewModel.isLoading.collectAsState()
                val listState = rememberTransformingLazyColumnState()

                DisposableEffect(lifecycleOwner) {
                    val ambientObserver = AmbientLifecycleObserver(
                        context as Activity,
                        callbacks = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
                            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) { ambientState.value = true }
                            override fun onExitAmbient() { ambientState.value = false }
                            override fun onUpdateAmbient() {}
                        }
                    )

                    val lifecycleObserver = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> ambientState.value = false
                            else -> Unit
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(ambientObserver)
                    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(ambientObserver)
                        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    }
                }

                Crossfade(
                    targetState = ambientState.value,
                    animationSpec = tween(
                        durationMillis = MainConstants.ANIMATION_MS,
                        easing = LinearOutSlowInEasing
                    ),
                    label = "ambientTransition"
                ) { isAmbient ->
                    if (!isAmbient) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MainScreen(viewModel, listState)
                            if(isLoading) CircularBar()
                            if(viewModel.sharedViewModel.showLogoutDialog) LogoutDialog(viewModel) { viewModel.sharedViewModel.showLogoutDialog = false }
                        }
                    } else AmbientDisplay()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ongoingActivityManager.start()
    }

    override fun onPause() {
        ongoingActivityManager.stop()
        super.onPause()
    }

    private fun prepareOngoingActivity() {
        val permission = Manifest.permission.POST_NOTIFICATIONS

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            ongoingActivityManager.start()
            return
        }

        val prefs = getSharedPreferences(DataKeys.APP_SETTINGS, MODE_PRIVATE)
        val alreadyAsked = prefs.getBoolean(DataKeys.NOTIFICATIONS_PERMISSION_ASKED, false)

        if (!alreadyAsked) {
            prefs.edit { putBoolean(DataKeys.NOTIFICATIONS_PERMISSION_ASKED, true) }
            notificationPermissionLauncher.launch(permission)
        }
    }
}

@Composable
fun AmbientDisplay() {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) { TimeText() }
}

@Composable
fun MainScreen(viewModel: MainViewModelW, listState: TransformingLazyColumnState) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as Activity
    val transformationSpec = rememberTransformationSpec()
    val scope = rememberCoroutineScope()
    val categories = viewModel.sharedViewModel.shoppingCategories
    var swipedItemId by remember { mutableStateOf<String?>(null) }
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
    val versionText = "${stringResource(R.string.version)} $versionName"
    val versionStyle = MaterialTheme.typography.arcMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    var versionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppEventManager.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showCustomToast(event.message, context)
                    AppEventManager.resetReplayCache()
                }
                is UiEvent.ShowSnackbar -> {
                    showCustomToast(event.message, context)
                    AppEventManager.resetReplayCache()
                }
            }
        }
    }

    AppScaffold(
        timeText = {
            if (!versionExpanded) TimeText() else {
                TimeText(
                    content = {
                        timeTextCurvedText(
                            time = versionText,
                            style = versionStyle
                        )
                    }
                )
            }
        }
    ) {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                item {
                    if(!viewModel.sharedViewModel.isLoggedIn) {
                        GoogleBtn(
                            modifier = transformedItem(transformationSpec)
                        ) {
                            scope.launch {
                                val result = GoogleAuth(activity).signIn()
                                result.onSuccess {
                                    viewModel.login()
                                }.onFailure { error ->
                                    showCustomToast(error.message.toString(), context)
                                }
                            }
                        }
                    } else {
                        AsyncImage(
                            model = viewModel.sharedViewModel.userPhotoUrl,
                            contentDescription = viewModel.sharedViewModel.userName,
                            contentScale = ContentScale.Crop,
                            modifier = transformedItem(transformationSpec).size(Constants.BUTTON_SIZE)
                                .clip(CircleShape)
                                .clickable(onClick = { viewModel.sharedViewModel.showLogoutDialog = true })
                        )
                    }
                }
                item {
                    ListHeader(
                        modifier = transformedItem(transformationSpec).fillMaxWidth(),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        ClickableBox(
                            onLongClick = { versionExpanded = !versionExpanded }
                        ) { PageTitle(text = stringResource(R.string.app_name)) }
                    }
                }
                categories
                    .filter { category ->
                        viewModel.sharedViewModel.shoppingItems.any { it.categoryId == category.id && !it.done }
                    }
                    .forEach { category ->
                    item(key = category.id) {
                        Box(
                            modifier = transformedItem(transformationSpec).fillMaxWidth().height(Constants.BASE_SIZE)
                                .padding(PaddingStyle.verticalXxs),
                            contentAlignment = Alignment.Center
                        ) {
                            MainText(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    viewModel.sharedViewModel.shoppingItems.filter { it.categoryId == category.id && !it.done }
                        .forEach { item ->
                            item(key = item.id) {
                                ShoppingListItem(
                                    modifier = transformedItem(transformationSpec)
                                        .padding(PaddingStyle.verticalXxs),
                                    item = item,
                                    onCheck = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        viewModel.sharedViewModel.toggleItemDone(item)
                                    },
                                    isSwiped = swipedItemId == item.id,
                                    onSwipe = { swipedItemId = item.id },
                                    onSwipeClosed = { swipedItemId = null }
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun ShoppingListItem(
    modifier: Modifier,
    item: ShoppingItem,
    onCheck: () -> Unit,
    isSwiped: Boolean,
    onSwipe: () -> Unit,
    onSwipeClosed: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentItem by rememberUpdatedState(item)
    val currentOnCheck by rememberUpdatedState(onCheck)
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

    LaunchedEffect(isSwiped) {
        if (!isSwiped && offsetX != 0f) animateOffset(0f)
    }

    Box(modifier = modifier.fillMaxWidth()) {
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
                .pointerInput(item.id, itemWidthPx, isSwiped) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        if (itemWidthPx <= 0f) return@awaitEachGesture
                        val edge = itemWidthPx * 0.25f
                        val fullSwipe = itemWidthPx * 0.5f
                        val openedAtStart = isSwiped || offsetX != 0f
                        val startedFromRight = startX >= itemWidthPx - edge
                        if (!openedAtStart && !startedFromRight) return@awaitEachGesture
                        dragStartOffset = offsetX

                        val firstDrag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            val movingRight = over > 0f
                            val allowed = openedAtStart
                                    || (!movingRight)

                            if (allowed) {
                                change.consume()
                                offsetX = (offsetX + over).coerceIn(-itemWidthPx, 0f)
                            }
                        }

                        if (firstDrag == null) return@awaitEachGesture

                        val completed = horizontalDrag(firstDrag.id) { change ->
                            val delta = change.position.x - change.previousPosition.x

                            offsetX = (offsetX + delta).coerceIn(-itemWidthPx, 0f)

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

                            offsetX <= -fullSwipe ->
                                animateCheck {
                                    onSwipeClosed()
                                    currentOnCheck()
                                }

                            offsetX < -revealWidthPx / 2f -> {
                                currentOnSwipe()
                                animateOffset(-revealWidthPx)
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
                    }
                },
                onLongClick = {
                    if (offsetX != 0f) {
                        scope.launch {
                            animateOffset(0f)
                        }
                    } else onCheck()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE)
                        .padding(PaddingStyle.horizontalXs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                            .padding(PaddingStyle.horizontalS),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        MainText(
                            text = item.name,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item.quantity?.takeIf { it.isNotBlank() }?.let {
                        Box(
                            modifier = Modifier.width(Constants.BASE_SIZE * 2),
                            contentAlignment = Alignment.Center
                        ) {
                            MainText(
                                text = it,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogoutDialog(viewModel: MainViewModelW, onDismiss: () -> Unit) {
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
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
                    contentAlignment = Alignment.Center
                ) { DialogTitle(stringResource(R.string.logout)) }
                Row(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CloseBtn { onDismiss() }
                    DoneBtn {
                        scope.launch {
                            GoogleAuth(activity).signOut()
                            viewModel.logout()
                        }
                    }
                }
            }
        }
    }
}