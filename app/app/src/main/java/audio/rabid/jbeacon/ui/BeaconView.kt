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
import audio.rabid.jbeacon.Beacon
import audio.rabid.jbeacon.BeaconManager.BeaconStatus
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import audio.rabid.jbeacon.ui.theme.Typography
import audio.rabid.jbeacon.vm.BeaconViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

@Composable
fun BeaconView(
    beacon: Beacon, modifier: Modifier = Modifier, status: BeaconStatus
) {
    Card(
        modifier
            .fillMaxWidth()
            .padding(16.0.dp)
    ) {
        Text(beacon.name, Modifier.padding(8.dp), style = Typography.bodyLarge)

        val (icon, text) = when (status) {
            is BeaconStatus.InRange -> Pair(Icons.Default.MyLocation, "In Range")
            is BeaconStatus.OutOfRange -> Pair(
                Icons.Default.LocationDisabled, "Out Of Range"
            )
        }

        Row(Modifier.padding(vertical = 8.dp)) {
            Icon(icon, text, Modifier.padding(16.dp))
            Column {
                Text(text = text, style = Typography.bodySmall)
                Text(
                    text = "Last Seen: ${status.relativeLastSeen()}",
                    style = Typography.bodySmall
                )
            }
        }
    }
}

private fun BeaconStatus.relativeLastSeen(): CharSequence = if (lastSeen == Instant.EPOCH) "never"
else DateUtils.getRelativeTimeSpanString(lastSeen.toEpochMilli())

@Preview(showBackground = true, widthDp = 320, heightDp = 570)
@Composable
fun BeaconPreview() {
    JBeaconTheme {
        Column {
            BeaconView(
                beacon = Beacon(
                    name = "John's Keys",
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    lastSeen = Instant.now()
                ),
                status = BeaconStatus.InRange(
                    rssi = -30.0f,
                    lastSeen = Instant.now().minusSeconds(30)
                ),
            )

            BeaconView(
                beacon = Beacon(
                    name = "John's Phone",
                    macAddress = "00:11:22:33:44:55",
                    lastSeen = Instant.now()
                ),
                status = BeaconStatus.OutOfRange(
                    lastSeen = Instant.now().minusSeconds(600)
                ),
            )
        }
    }
}
