package audio.rabid.jbeacon.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import audio.rabid.jbeacon.BeaconStatuses
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun BeaconsListView(uiState: SharedFlow<BeaconStatuses>, modifier: Modifier = Modifier) {
    val state = uiState.collectAsStateWithLifecycle(emptyMap())
    Column {
        for ((beacon, status) in state.value) {
            BeaconView(beacon, modifier, status)
        }
    }
}
