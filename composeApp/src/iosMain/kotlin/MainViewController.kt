import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.ais_sst_mobile.App
import com.example.ais_sst_mobile.di.initKoin
import com.example.ais_sst_mobile.navigation.DefaultRootComponent
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()

    val lifecycle = LifecycleRegistry()
    val backDispatcher = BackDispatcher()

    val root = DefaultRootComponent(
        DefaultComponentContext(
            lifecycle = lifecycle,
            backHandler = backDispatcher
        )
    )

    return ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = null,
            modifier = Modifier.fillMaxSize()
        ) {
            App(root)
        }
    }
}