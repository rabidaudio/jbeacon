package audio.rabid.jbeacon

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import audio.rabid.jbeacon.ui.BeaconsListView
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import audio.rabid.jbeacon.vm.BeaconViewModel
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm = androidViewModel<BeaconViewModel>()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            vm::permissionsGranted
        )

        setContent {
            JBeaconTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    vm.permissionState.onEach { granted ->
                        // normally it would be good to call this but anyone using the app will know why we need it
                        // shouldShowRequestPermissionRationale()
                        if (granted == false)
                            permissionLauncher.launch(Scanner.requiredPermissions.toTypedArray())
                    }.collectAsStateWithLifecycle(null)

                    BeaconsListView(vm.uiState, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

inline fun <reified T: AndroidViewModel> Activity.androidViewModel(): T {
    return ViewModelProvider.AndroidViewModelFactory
        .getInstance(application)
        .create(T::class.java)
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    JBeaconTheme {
//        Greeting("Android")
//    }
//}