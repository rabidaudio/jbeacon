package audio.rabid.jbeacon

import android.app.PendingIntent
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BackgroundScanBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val REQUEST_CODE = 0xBEAC07

        fun getScanPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, REQUEST_CODE,
                Intent(context, BackgroundScanBroadcastReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        fun cancelIfScheduled(context: Context) {
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE,
                Intent(context, BackgroundScanBroadcastReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
            )
            pendingIntent?.cancel()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as JBeaconApplication
        val scanner = application.scanner
        val beaconManager = application.beaconManager
        val notificationManager = application.notificationManager

        if (!scanner.bluetoothEnabled()) {
            notificationManager.requestBluetoothEnable()
            return
        }

        scanner.connectBackground()

        Log.d(
            "BackgroundBR", "scan: $intent extras: " +
                    "${intent.extras?.keySet()?.joinToString(",")}"
        )

        val errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, 0)
        if (errorCode != 0) {
            Log.e("BackgroundBR", "Problem scanning: $errorCode")
            // TODO
            return
        }

        val scanResults = intent.getParcelableArrayListExtra(
            BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            ScanResult::class.java
        ) ?: return

        // TODO: this alone isn't going to be reliable. Need to use CompanionDevice API
        // https://developer.android.com/develop/connectivity/bluetooth/companion-device-pairing#keep-awake
        beaconManager.processScanResultsFromBackground(scanResults)
    }
}
