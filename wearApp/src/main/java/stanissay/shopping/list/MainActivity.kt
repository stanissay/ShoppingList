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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import stanissay.shopping.common.MainConstants
import stanissay.shopping.list.utils.MainText
import stanissay.shopping.list.utils.MainTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModelW by viewModels()

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
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = { /*TODO*/ },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    MainText("More")
                }
            },
        ) { contentPadding -> // ScreenScaffold provides default padding; adjust as needed
            TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                item {
                    ListHeader(
                        modifier =
                            Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        MainText(text = "stringResource(R.string.hello_world, greetingName")
                    }
                }
                item {
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        MainText("Button A")
                    }
                }
                item {
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        MainText("Button B")
                    }
                }
                item {
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        MainText("Button C")
                    }
                }
            }
        }
    }
}