package stanissay.shopping.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import stanissay.shopping.common.MainStrings
import stanissay.shopping.list.utils.MainTheme
import stanissay.shopping.list.utils.PageTitle

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModelA by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                MainHead()
                MainList(viewModel)
            }
        }
    }
}

@Composable
fun MainHead() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) { PageTitle(MainStrings.HEAD) }
}

@Composable
fun MainList(viewModelA: MainViewModelA) {

}