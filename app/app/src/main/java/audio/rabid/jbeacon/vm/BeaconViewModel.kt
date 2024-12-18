package audio.rabid.jbeacon.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import audio.rabid.jbeacon.JBeaconApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

class BeaconViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceFinder
        get() = getApplication<JBeaconApplication>().deviceFinder

    private val db
        get() = getApplication<JBeaconApplication>().db

    sealed class State {
        abstract val lastSeen: Instant
        data class OutOfRange(override val lastSeen: Instant): State()
        data class InRange(val db: Double, override val lastSeen: Instant): State()
    }

    private val _state = MutableStateFlow<State>(State.OutOfRange(lastSeen = Instant.EPOCH))
    val uiState: StateFlow<State> = _state.asStateFlow()

    private val _permissionsGranted = MutableStateFlow<Boolean?>(null)
    val permissionState = _permissionsGranted.asStateFlow()

    init {
        val hasPermissions =  deviceFinder.hasPermissions()
        _permissionsGranted.value = hasPermissions
        if (hasPermissions) {
            startScan()
        }
    }

    fun permissionsGranted(state: Map<String, Boolean>) {
        val hasPermissions = deviceFinder.hasPermissions()
        _permissionsGranted.value = hasPermissions
        if (hasPermissions) {
            startScan()
        }
    }

    private fun startScan() {
        deviceFinder.scan(emptyList())
    }

//    fun ring()
}
