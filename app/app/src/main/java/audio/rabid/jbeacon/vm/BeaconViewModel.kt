package audio.rabid.jbeacon.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import audio.rabid.jbeacon.BeaconStatuses
import audio.rabid.jbeacon.JBeaconApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn

class BeaconViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner
        get() = getApplication<JBeaconApplication>().scanner

    private val beaconManager
        get() = getApplication<JBeaconApplication>().beaconManager

    val uiState: SharedFlow<BeaconStatuses> = beaconManager.knownDeviceStatuses()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val _permissionsGranted = MutableStateFlow<Boolean?>(null)
    val permissionState = _permissionsGranted.asStateFlow()

    init {
        val hasPermissions =  scanner.hasPermissions()
        _permissionsGranted.value = hasPermissions
        if (hasPermissions) {
            scanner.connectForeground()
        }
    }

    override fun onCleared() {
        scanner.disconnectForeground()
        super.onCleared()
    }

    fun permissionsGranted(state: Map<String, Boolean>) {
        val hasPermissions = scanner.hasPermissions()
        _permissionsGranted.value = hasPermissions
        if (hasPermissions) {
            scanner.connectForeground()
        }
    }

    // // "20:C3:8F:D6:32:04"

//    fun ring()
}
