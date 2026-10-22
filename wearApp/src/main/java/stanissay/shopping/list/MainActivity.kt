package stanissay.shopping.list

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
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
import stanissay.shopping.list.utils.Constants
import stanissay.shopping.list.utils.GoogleBtn
import stanissay.shopping.list.utils.MainText
import stanissay.shopping.list.utils.MainTheme
import stanissay.shopping.list.utils.MainViewModelFactory
import stanissay.shopping.list.utils.PageTitle
import stanissay.shopping.shared.AndroidStringProvider
import stanissay.shopping.shared.GoogleAuth
import stanissay.shopping.shared.MainConstants
import stanissay.shopping.shared.showCustomToast
import stanissay.shopping.shared.R

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

    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                if(viewModel.sharedViewModel.isLoggedIn) {
                    EdgeButton(
                        onClick = {  },
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
                    count = viewModel.sharedViewModel.shoppingCategories.size,
                    key = { index ->
                        viewModel.sharedViewModel.shoppingCategories[index].id
                    }
                ) { index ->
                    val category = viewModel.sharedViewModel.shoppingCategories
                        .sortedBy { it.order }[index]

                    val items = viewModel.sharedViewModel.shoppingItems
                        .filter { it.categoryId == category.id }
                        .sortedBy { it.order }

                    this@TransformingLazyColumn.item {
                        ListHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            PageTitle(text = category.name)
                        }
                    }

                    items.forEach { shoppingItem ->
                        this@TransformingLazyColumn.item {
                            Button(
                                onClick = {
                                    // TODO: дія при натисканні на товар
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                                transformation = SurfaceTransformation(transformationSpec),
                            ) {
                                MainText(
                                    text = buildString {
                                        append(if (shoppingItem.done) "✓ " else "• ")
                                        append(shoppingItem.name)
                                        shoppingItem.quantity?.let {
                                            append(" × $it")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}