package audio.rabid.jbeacon

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.whenStarted
import androidx.lifecycle.withStarted
import audio.rabid.jbeacon.ui.AddBeaconView
import audio.rabid.jbeacon.ui.BeaconsListView
import audio.rabid.jbeacon.ui.ErrorView
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext

class MainActivity : ComponentActivity() {

    object EnableBluetooth : ActivityResultContract<Unit, Boolean>() {
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return (resultCode == RESULT_OK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm = androidViewModel<BeaconViewModel>()

        val permissionRequester = vm.permissions.createPermissionRequester(this) {
            vm.permissionsGranted()
        }

        val enableBluetoothLauncher = registerForActivityResult(EnableBluetooth) { enabled ->
            if (enabled) vm.bluetoothEnabled()
        }
        fun enableBluetooth() {
            enableBluetoothLauncher.launch(Unit)
        }

        val companionDeviceVM = androidViewModel<CompanionDeviceViewModel>()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
//                vm.onStart()
                companionDeviceVM.start()
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                vm.onStop()
            }
        })

        val selectDeviceLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != RESULT_OK) {
                companionDeviceVM.onDeviceSelectCanceled()
            }
        }


        setContent {
            val state = vm.uiState.collectAsStateWithLifecycle()

            val intentSender = companionDeviceVM.associationIntentSender.collectAsStateWithLifecycle()
            intentSender.value?.let { intentSender ->
                LaunchedEffect("select device") {
                    selectDeviceLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            }

            JBeaconTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
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
                                LaunchedEffect("no-permissions") {
                                    lifecycle.withStarted {
                                        permissionRequester.request()
                                    }
                                }
                                ErrorView(
                                    message = "Bluetooth permissions need to be granted.",
                                    onClick = { permissionRequester.request() }
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
