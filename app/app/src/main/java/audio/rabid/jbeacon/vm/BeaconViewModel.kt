package audio.rabid.jbeacon.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import audio.rabid.jbeacon.Beacon
import audio.rabid.jbeacon.BeaconStatuses
import audio.rabid.jbeacon.JBeaconApplication
import audio.rabid.jbeacon.Scanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn

class BeaconViewModel(application: Application) : AndroidViewModel(application) {

    sealed class State {
        object BeaconList : State()
        object AddingBeacon : State()
        sealed class ErrorState : State() {
            object NoPermissions : ErrorState()
            object BluetoothDisabled : ErrorState()
            data class OtherError(val message: String) : ErrorState()
        }
    }

    private val scanner
        get() = getApplication<JBeaconApplication>().scanner

    private val beaconManager
        get() = getApplication<JBeaconApplication>().beaconManager

    private val _uiState = MutableStateFlow<State>(State.BeaconList)
    val uiState = _uiState.asStateFlow()

    val beaconState = beaconManager.knownDeviceStatuses()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.Eagerly)

    val inRangeNewDevices = beaconManager.inRangeNewDevices()
        .onEach { Log.d("vm", "in range set: $it") }
        .onStart { Log.d("vm", "in range started") }
        .onCompletion { Log.d("vm", "in range completed") }
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.Lazily)
        .onSubscription { Log.d("vm", "in range subscribed") }

    init {
        startUp()
    }

    fun permissionsGranted() {
        if (_uiState.value !is State.ErrorState) return

        startUp()
    }

    fun bluetoothEnabled() {
        if (_uiState.value !is State.ErrorState) return

        startUp()
    }

    fun startAddBeacon() {
        if (_uiState.value != State.BeaconList) return

        _uiState.value = State.AddingBeacon
    }

    fun addBeacon(name: String, advertisement: Scanner.Advertisement) {
        beaconManager.addBeacon(Beacon(
            name = name,
            macAddress = advertisement.address,
            lastSeen = advertisement.lastAdvertisement
        ))
        back()
    }

    fun back() {
        if (_uiState.value != State.AddingBeacon) return

        _uiState.value = State.BeaconList
    }

    fun forgetBeacon(beacon: Beacon) {
        beaconManager.removeBeacon(beacon)
    }

    private fun startUp() {
        val hasPermissions =  scanner.hasPermissions()
        if (!hasPermissions) {
            _uiState.value = State.ErrorState.NoPermissions
        } else if (!scanner.bluetoothEnabled()) {
            _uiState.value = State.ErrorState.BluetoothDisabled
        } else {
            scanner.connectForeground()
            _uiState.value = State.BeaconList
        }
        // TODO: find a way to propagate scan errors
    }

    override fun onCleared() {
        scanner.disconnectForeground()
        super.onCleared()
    }

    // // "20:C3:8F:D6:32:04"

//    fun ring()
}
