package audio.rabid.jbeacon

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant
import java.util.UUID

typealias MacAddress = String

class Scanner(private val applicationContext: Context) {

    data class Advertisement(
        val address: MacAddress,
        val rssi: Float, // in dBm, down to -127
        val lastAdvertisement: Instant
    )

    enum class State {
        STARTING_UP, SCANNING_BACKGROUND, SCANNING_FOREGROUND,
        ERROR_NO_PERMISSIONS, ERROR_BT_DISABLED, ERROR_SCAN_FAILED;

        fun isScanning() = when (this) {
            SCANNING_BACKGROUND, SCANNING_FOREGROUND -> true
            STARTING_UP,
            ERROR_NO_PERMISSIONS,
            ERROR_BT_DISABLED,
            ERROR_SCAN_FAILED -> false
        }
    }

    companion object {
        @SuppressLint("ObsoleteSdkInt")
        val requiredPermissions = buildList {
            if (Build.VERSION.SDK_INT < 31) add(Manifest.permission.BLUETOOTH)
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_SCAN)
            // Provides UUID and mac address access
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

        val REPORT_FREQUENCY_BACKGROUND = 30 * 1000L
    }

    private val manager = applicationContext.getSystemService(BluetoothManager::class.java)

    var state = State.STARTING_UP
        private set

    private val _advertisements = MutableSharedFlow<Advertisement>()

    val advertisements: SharedFlow<Advertisement> get() = _advertisements

    // These are used to get proper timestamps from scan results
    private val startTime = Instant.now()
    private val bootTimeNanos = SystemClock.elapsedRealtimeNanos()

    fun hasPermissions(): Boolean {
        for (permission in requiredPermissions) {
            when (ContextCompat.checkSelfPermission(applicationContext, permission)) {
                PackageManager.PERMISSION_GRANTED -> continue
                PackageManager.PERMISSION_DENIED -> return false
            }
        }
        return true
    }

    fun bluetoothEnabled(): Boolean = manager.adapter.isEnabled

    fun connectBackground() {
        synchronized(this) {
            if (!hasPermissions()) {
                state = State.ERROR_NO_PERMISSIONS
                // TODO: pop notif
                return
            }
            if (!bluetoothEnabled()) {
                state = State.ERROR_BT_DISABLED
                // TODO: pop notif
                return
            }
            state = State.SCANNING_BACKGROUND
            scanBackground()
        }
    }

    fun disconnectBackground() {
        synchronized(this) {
            if (state != State.SCANNING_BACKGROUND) return

            stopBackground()
            state = State.STARTING_UP
        }
    }

    fun connectForeground() {
        synchronized(this) {
            if (state == State.SCANNING_FOREGROUND) return

            if (!hasPermissions()) {
                state = State.ERROR_NO_PERMISSIONS
                return
            }

            if (!bluetoothEnabled()) {
                state = State.ERROR_BT_DISABLED
                return
            }

            if (state == State.SCANNING_BACKGROUND) {
                stopBackground()
            }

            state = State.SCANNING_FOREGROUND
            scanForeground()
        }
    }

    fun disconnectForeground() {
        synchronized(this) {
            if (state != State.SCANNING_FOREGROUND) return

            stopForeground()
            // switch to background scans
            connectBackground()
        }
    }

    private val foregroundScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            Log.d("JBEACON-BT", "onScanResult: $callbackType $result")
            Log.d(
                "JBEACON-BT",
                "UUIDS: ${result.device.uuids}, address: ${result.device.address} " +
                        "name: ${result.scanRecord?.deviceName} serviceuuids: " +
                        "${result.scanRecord?.serviceUuids} solicitation uuids " +
                        "${result.scanRecord?.serviceSolicitationUuids} " +
                        "service data ${result.scanRecord?.serviceData}"
            )
            _advertisements.tryEmit(result.toInRange())
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results ?: return
            Log.d("JBEACON-BT", "onBatchScanResults: $results")
            for (result in results) {
                _advertisements.tryEmit(result.toInRange())
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("JBEACON-BT", "scan Failed: $errorCode")
            state = State.ERROR_SCAN_FAILED
            super.onScanFailed(errorCode)
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanForeground() {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
//        val settings = ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
////                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
//            .setReportDelay(REPORT_FREQUENCY_BACKGROUND)
//            // TODO: back-support older sdk versions (e.g. 26)
//            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH)
//            .build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()

        manager.adapter.bluetoothLeScanner.startScan(listOf(filter), settings, foregroundScanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopForeground() {
        manager.adapter.bluetoothLeScanner.stopScan(foregroundScanCallback)
    }

    private fun scanBackground() {
        TODO()
    }

    private fun stopBackground() {
        TODO()
    }

    private fun ScanResult.getAdvertisementTime(): Instant =
        startTime.plusNanos(timestampNanos - bootTimeNanos)

    private fun ScanResult.toInRange() = Advertisement(
        address = device.address,
        rssi = rssi.toFloat(),
        lastAdvertisement = getAdvertisementTime()
    )
}
