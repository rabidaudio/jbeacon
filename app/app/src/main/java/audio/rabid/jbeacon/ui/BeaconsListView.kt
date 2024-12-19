package audio.rabid.jbeacon.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import audio.rabid.jbeacon.Beacon
import audio.rabid.jbeacon.BeaconStatuses
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BeaconsListView(uiState: SharedFlow<BeaconStatuses>, onLongClick: (Beacon) -> Unit) {
    val state = uiState.collectAsStateWithLifecycle(emptyMap())

    if (state.value.isEmpty()) {
        ErrorView("No beacons registered yet.")
    } else {
        Column {
            for ((beacon, status) in state.value) {
                BeaconView(
                    beacon,
                    Modifier.combinedClickable(onClick = {}, onLongClick = { onLongClick(beacon) }),
                    status
                )
            }
        }
    }
}
