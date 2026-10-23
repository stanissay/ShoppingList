package stanissay.shopping.list

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import stanissay.shopping.list.utils.CheckBtn
import stanissay.shopping.list.utils.ClickableBox
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.DoneBtn
import stanissay.shopping.list.utils.GoogleBtn
import stanissay.shopping.list.utils.MainCard
import stanissay.shopping.list.utils.MainLines
import stanissay.shopping.list.utils.MainText
import stanissay.shopping.list.utils.MainTheme
import stanissay.shopping.list.utils.MainViewModelFactory
import stanissay.shopping.list.utils.PageTitle
import stanissay.shopping.shared.AndroidStringProvider
import stanissay.shopping.shared.GoogleAuth
import stanissay.shopping.shared.MainColors
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.PaddingStyle
import stanissay.shopping.shared.showCustomToast
import stanissay.shopping.shared.R
import stanissay.shopping.shared.ShoppingItem
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModelW by viewModels {
        MainViewModelFactory(application, AndroidStringProvider(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MainTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val ambientState = remember { mutableStateOf(false) }

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
                        MainScreen(viewModel)
                    } else AmbientDisplay()
                }
            }
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
fun MainScreen(viewModel: MainViewModelW) {
    val context = LocalContext.current
    val activity = context as Activity
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val scope = rememberCoroutineScope()
    val categories = viewModel.sharedViewModel.shoppingCategories
    var swipedItemId by remember { mutableStateOf<String?>(null) }

    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                if(viewModel.sharedViewModel.isLoggedIn) {
                    EdgeButton(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) { MainText(stringResource(R.string.logout)) }
                }
            },
        ) { contentPadding ->
            TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                item {
                    if(!viewModel.sharedViewModel.isLoggedIn) {
                        GoogleBtn {
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
                            modifier = Modifier.size(Constants.BUTTON_SIZE)
                                .clip(CircleShape)
                        )
                    }
                }
                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) { PageTitle(text = stringResource(R.string.app_name)) }
                }
                items(
                    count = categories.size,
                    key = { index ->
                        categories[index].id
                    }
                ) { index ->
                    val category = categories[index]
                    val items = viewModel.sharedViewModel.shoppingItems.filter { it.categoryId == category.id && !it.done }

                    this@TransformingLazyColumn.item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) { PageTitle(text = category.name) }
                    }

                    items.forEach { item ->
                        this@TransformingLazyColumn.item {
                            ShoppingListItem(
                                modifier = Modifier.padding(PaddingStyle.verticalXs),
                                item = item,
                                onCheck = { viewModel.sharedViewModel.toggleItemDone(item) },
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
        val targetY = if (item.done) -itemWidthPx * 0.5f else itemWidthPx * 0.5f
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
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(
                            0f,
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
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(Constants.BASE_SIZE)
                        .padding(PaddingStyle.horizontalXl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckBtn(checked = item.done) { onCheck() }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .padding(PaddingStyle.horizontalS),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        MainText(
                            text = item.name,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxHeight().width(Constants.BASE_SIZE),
                        contentAlignment = Alignment.Center
                    ) {
                        item.quantity?.takeIf { it.isNotBlank() }?.let {
                            MainText(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

}