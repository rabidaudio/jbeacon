package audio.rabid.jbeacon

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import audio.rabid.jbeacon.ui.AddBeaconView
import audio.rabid.jbeacon.ui.BeaconsListView
import audio.rabid.jbeacon.ui.ErrorView
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import audio.rabid.jbeacon.vm.BeaconViewModel

class MainActivity : ComponentActivity() {

    object EnableBluetooth : ActivityResultContract<Unit, Boolean>() {
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return (resultCode == RESULT_OK)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm = androidViewModel<BeaconViewModel>()

        val permissionLauncher = registerForActivityResult(RequestMultiplePermissions()) {
            vm.permissionsGranted()
        }
        fun requestBluetoothPermissions() {
            permissionLauncher.launch(Scanner.requiredPermissions.toTypedArray())
        }

        val enableBluetoothLauncher = registerForActivityResult(EnableBluetooth) { enabled ->
            if (enabled) vm.bluetoothEnabled()
        }
        fun enableBluetooth() {
            enableBluetoothLauncher.launch(Unit)
        }

        setContent {
            val state = vm.uiState.collectAsStateWithLifecycle()
            JBeaconTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
//                    topBar = {
//                        TopAppBar(
//                            colors = topAppBarColors(
//                                containerColor = MaterialTheme.colorScheme.primaryContainer,
//                                titleContentColor = MaterialTheme.colorScheme.primary,
//                            ),
//                            navigationIcon = {
//                                if (state.value is BeaconViewModel.State.AddingBeacon) {
//                                    IconButton(onClick = { vm.back() }) {
//                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
//                                    }
//                                }
//                            },
//                            title = {
//                                if (state.value is BeaconViewModel.State.AddingBeacon) {
//                                    Text("Add Beacon")
//                                } else {
//                                    Text("JBeacon")
//                                }
//                            }
//                        )
//                    },
                    floatingActionButton = {
                        if (state.value is BeaconViewModel.State.BeaconList) {
                            FloatingActionButton(
                                onClick = { vm.startAddBeacon() },
                            ) {
                                Icon(Icons.Filled.Add, "Add beacon")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (val currentState = state.value) {
                            BeaconViewModel.State.AddingBeacon -> AddBeaconView(
                                inRange = vm.inRangeNewDevices,
                                onBack = { vm.back() },
                                onSelected = { name, ad -> vm.addBeacon(name, ad) }
                            )
                            BeaconViewModel.State.BeaconList -> {
                                BeaconsListView(vm.beaconState,
                                    onLongClick = { vm.forgetBeacon(it) })
                            }

                            BeaconViewModel.State.ErrorState.BluetoothDisabled -> ErrorView(
                                message = "Bluetooth is disabled. Click anywhere to enable",
                                onClick = { enableBluetooth() }
                            )

                            BeaconViewModel.State.ErrorState.NoPermissions -> {
                                requestBluetoothPermissions()

                                ErrorView(
                                    message = "Bluetooth permissions need to be granted.",
                                    onClick = { requestBluetoothPermissions() }
                                )
                            }

                            is BeaconViewModel.State.ErrorState.OtherError -> ErrorView(
                                message = currentState.message
                            )
                        }
                    }
                }
            }
        }
    }
}

inline fun <reified T : AndroidViewModel> Activity.androidViewModel(): T {
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
