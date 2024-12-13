package audio.rabid.jbeacon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import audio.rabid.jbeacon.ui.Beacon
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import audio.rabid.jbeacon.vm.BeaconViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm: BeaconViewModel by viewModels()

        setContent {
            JBeaconTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Beacon(
                        name = "John's Keys",
                        Modifier.padding(innerPadding),
                        stateFlow = vm.uiState
                    )
                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    JBeaconTheme {
//        Greeting("Android")
//    }
//}