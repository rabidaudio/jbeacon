package audio.rabid.jbeacon

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import audio.rabid.jbeacon.ui.Beacon
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import audio.rabid.jbeacon.vm.BeaconViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm = androidViewModel<BeaconViewModel>()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            vm::permissionsGranted
        )

        // TODO: request only if necessary
        // normally it would be good to call this but anyone using the app will know why we need it
        // shouldShowRequestPermissionRationale()
        permissionLauncher.launch(DeviceFinder.requiredPermissions.toTypedArray())
//        vm.permissionState.onEach { granted ->
//            if (granted == false) requestPermissions()
//        }

        setContent {
            JBeaconTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Beacon(
                        name = "John's Keys",
                        modifier = Modifier.padding(innerPadding),
                        stateFlow = vm.uiState
                    )
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