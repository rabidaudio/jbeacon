package audio.rabid.jbeacon.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import audio.rabid.jbeacon.ui.theme.Typography
import audio.rabid.jbeacon.vm.BeaconViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

@Composable
fun Beacon(
    name: String, modifier: Modifier = Modifier, stateFlow: StateFlow<BeaconViewModel.State>
) {
    val state = stateFlow.collectAsStateWithLifecycle()

    Card(
        modifier
            .fillMaxWidth()
            .padding(16.0.dp)
    ) {
        Text(name, Modifier.padding(8.dp), style = Typography.bodyLarge)

        val (icon, text) = when (state.value) {
            is BeaconViewModel.State.InRange -> Pair(Icons.Default.MyLocation, "In Range")
            is BeaconViewModel.State.OutOfRange -> Pair(
                Icons.Default.LocationDisabled, "Out Of Range"
            )
        }

        Row(Modifier.padding(vertical = 8.dp)) {
            Icon(icon, text, Modifier.padding(16.dp))
            Column {
                Text(text = text, style = Typography.bodySmall)
                val lastSeen = if (state.value.lastSeen == Instant.EPOCH) "never"
                else DateUtils.getRelativeTimeSpanString(state.value.lastSeen.toEpochMilli())
                Text(text = "Last Seen: $lastSeen", style = Typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 570)
@Composable
fun BeaconPreview() {
    JBeaconTheme {
        Column {
            Beacon(
                name = "John's Keys", stateFlow = MutableStateFlow(
                    BeaconViewModel.State.InRange(
                        db = -30.0, lastSeen = Instant.now().minusSeconds(30)
                    )
                )
            )

            Beacon(
                name = "John's Phone", stateFlow = MutableStateFlow(
                    BeaconViewModel.State.OutOfRange(
                        lastSeen = Instant.now().minusSeconds(600)
                    )
                )
            )
        }
    }
}