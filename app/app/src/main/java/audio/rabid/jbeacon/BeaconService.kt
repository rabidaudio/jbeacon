package audio.rabid.jbeacon

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class BeaconService : IntentService("BeaconService") {

    companion object {
        const val REQUEST_CODE = 0xBEAC07
        const val SCAN_ACTION = "audio.rabid.jbeacon.BLUETOOTH_ADVERTISEMENTS"

        fun getScanPendingIntent(context: Context): PendingIntent =
            PendingIntent.getService(
                context, REQUEST_CODE,
                Intent(context, BeaconService::class.java).setAction(SCAN_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val application get() = applicationContext as JBeaconApplication
    private val scanner get() = application.scanner
    private val beaconManager get() = application.beaconManager
    private val notificationManager get() = application.notificationManager

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        scanner.connectBackground()

        intent ?: return

        Log.d("BeaconService", "onReceive: $intent has extras: ${intent.extras?.keySet()}")
        // TODO: for some reason, the pending intent is delivered but contains no extras. Instead,
        // we switch to "foreground" mode (callback-based scan) and subscribe to beaconLost

        if (scanner.state.isScanning()) {
            coroutineScope.launch {
                beaconManager.beaconLost()
                    .onStart { Log.d("BeaconService", "listening for lost") }
                    .timeout(2.seconds)
                    .onEach { notificationManager.showBeaconLost(it) }
                    .onCompletion { Log.d("BeaconService", "stopping lost listener") }
                    .onCompletion { stopForeground(STOP_FOREGROUND_REMOVE) }
                    .collect()
            }
        }
        scanner.connectForeground()
        startForeground(REQUEST_CODE, notificationManager.getForegroundServiceNotification())
        Log.d("BeaconService", "Move foreground: $foregroundServiceType")
    }

    @Deprecated("Deprecated in Java")
    override fun onDestroy() {
        Log.d("BeaconService", "onDestroy")
        coroutineScope.cancel("Service Shutting down")
        scanner.connectBackground()
        super.onDestroy()
    }
}
