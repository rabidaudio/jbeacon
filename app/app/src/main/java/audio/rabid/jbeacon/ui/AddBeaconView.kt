package audio.rabid.jbeacon.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import audio.rabid.jbeacon.Beacon
import audio.rabid.jbeacon.BeaconManager
import audio.rabid.jbeacon.Scanner
import audio.rabid.jbeacon.ui.theme.JBeaconTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBeaconView(
    inRange: SharedFlow<Set<Scanner.Advertisement>>,
    onBack: () -> Unit,
    onSelected: (name: String, advertisement: Scanner.Advertisement) -> Unit
) {
    val state = inRange
        .onSubscription { Log.d("UI", "in range subscribed") }
        .onEach { Log.d("UI", "in range new devices: $it") }
        .collectAsStateWithLifecycle(emptySet())

    var selectedAdvertisement by remember { mutableStateOf<Scanner.Advertisement?>(null) }
    var showingDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxHeight()) {
        TopAppBar(
            colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            title = {
                Text("Add Beacon")
            }
        )

        LinearProgressIndicator(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
        )

        if (showingDialog) {
            BasicAlertDialog(
                onDismissRequest = {
                    showingDialog = false
                    name = ""
                }
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Give the beacon a name")
                        TextField(
                            value = name,
                            modifier = Modifier.focusGroup(),
                            onValueChange = { name = it }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.align(Alignment.End)) {
                            Button(
                                onClick = {
                                    showingDialog = false
                                    name = ""
                                }
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    showingDialog = false
                                    onSelected(name, selectedAdvertisement!!)
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }

        if (state.value.isEmpty()) {
            ErrorView(message = "No beacons found in range")
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                for (advertisement in state.value) {
                    BeaconView(
                        beacon = Beacon(
                            name = advertisement.name ?: "<No name set> ${advertisement.address}",
                            macAddress = advertisement.address,
                            lastSeen = advertisement.advertisedAt
                        ),
                        modifier = Modifier.clickable {
                            selectedAdvertisement = advertisement
                            showingDialog = true
                        },
                        status = BeaconManager.BeaconStatus.InRange(
                            rssi = advertisement.rssi,
                            lastSeen = advertisement.advertisedAt
                        )
                    )
                }
            }
        }
    }
}

@SuppressLint("FlowOperatorInvokedInComposition")
@Preview(showBackground = true, widthDp = 320, heightDp = 570)
@Composable
fun AddBeaconPreview() {
    JBeaconTheme {
        val inRange = flowOf(
//            emptySet<Scanner.Advertisement>()
            setOf(
                Scanner.Advertisement(
                    address = "00:11:22:33:44:55",
                    rssi = -60f,
                    advertisedAt = Instant.now().minusSeconds(1),
                    name = "Device 1"
                ),
                Scanner.Advertisement(
                    address = "AA:BB:CC:DD:EE:FF",
                    rssi = -120f,
                    advertisedAt = Instant.now().minusSeconds(3),
                    name = null
                ),
            )
        ).shareIn(MainScope(), started = SharingStarted.WhileSubscribed(), replay = 1)
        AddBeaconView(
            inRange = inRange,
            onBack = {},
            onSelected = { a, b -> Unit }
        )
    }
}