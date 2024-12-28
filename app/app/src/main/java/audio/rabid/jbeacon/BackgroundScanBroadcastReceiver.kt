package audio.rabid.jbeacon

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class BackgroundScanBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val REQUEST_CODE = 0xBEAC07

        fun getScanPendingIntent(context: Context) =
            PendingIntent.getBroadcast(context, REQUEST_CODE,
                Intent(context, BackgroundScanBroadcastReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        fun cancelIfScheduled(context: Context) {
            val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE,
                Intent(context, BackgroundScanBroadcastReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE)
            pendingIntent?.cancel()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as JBeaconApplication
        val scanner = application.scanner
//        val beaconManager = application.beaconManager
//        val notificationManager = application.notificationManager

        scanner.connectBackground()

        Log.d("BackgroundBR", "scan: $intent extras: ${intent.extras?.keySet()?.joinToString(",")}")

//        when (intent.action) {
//            SCAN_ACTION -> {
//                // TODO: subscribe to new stream and send any push notifications
//
//
//
//                // Show notifications if beacons are no longer in range
//                // timeout keeps the broadcast receiver from running for too long in the background
//                runBlocking {
//                    beaconManager.beaconLost()
//                        .timeout(1.seconds)
//                        .onEach { notificationManager.showBeaconLost(it) }
//                        .collect()
//                }
//            }
//        }
    }
}
