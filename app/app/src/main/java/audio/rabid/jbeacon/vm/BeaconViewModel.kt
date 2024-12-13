package audio.rabid.jbeacon.vm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

class BeaconViewModel : ViewModel() {

    sealed class State {
        abstract val lastSeen: Instant
        data class OutOfRange(override val lastSeen: Instant): State()
        data class InRange(val db: Double, override val lastSeen: Instant): State()
    }

    private val _state = MutableStateFlow<State>(State.OutOfRange(lastSeen = Instant.EPOCH))
    val uiState: StateFlow<State> = _state.asStateFlow()

//    fun ring()
}
