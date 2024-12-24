package audio.rabid.jbeacon

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.companion.BluetoothDeviceFilter
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

typealias MacAddress = String

class Scanner(
    private val applicationContext: Context,
    private val notificationManager: NotificationManager
) {

    data class Advertisement(
        val address: MacAddress,
        val rssi: Float, // in dBm, down to -127
        val lastAdvertisement: Instant,
        val name: String?
    ) {
        companion object {
            @Throws(JSONException::class)
            fun fromJson(json: String): Advertisement {
                val obj = JSONObject(json)
                return Advertisement(
                    address = obj.getString("address"),
                    rssi = obj.getDouble("rssi").toFloat(),
                    lastAdvertisement = Instant.ofEpochMilli(obj.getLong("last_advertisement")),
                    name = obj.optString("name")
                )
            }
        }

        fun toJson(): String {
            return JSONObject().apply {
                put("address", address)
                put("rssi", rssi)
                put("last_advertisement", lastAdvertisement.toEpochMilli())
                put("name", name)
            }.toString()
        }
    }

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
        private val requiredPermissions = buildSet {
            if (Build.VERSION.SDK_INT < 31) add(Manifest.permission.BLUETOOTH)
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_SCAN)
            // Provides UUID and mac address access
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

        const val REPORT_FREQUENCY_BACKGROUND = 30 * 1000L
    }

    private val manager = applicationContext.getSystemService(BluetoothManager::class.java)

    val coroutineContext = CoroutineScope(Dispatchers.IO)

    var state = State.STARTING_UP
        private set

    private val _advertisements = MutableSharedFlow<Advertisement>()

    val advertisements: Flow<Advertisement> get() = _advertisements
        .buffer(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // These are used to get proper timestamps from scan results
    private val startTime = Instant.now()
    private val bootTimeNanos = SystemClock.elapsedRealtimeNanos()

    val permissions = PermissionGranter(applicationContext, requiredPermissions)

    fun bluetoothEnabled(): Boolean = manager.adapter.isEnabled

    fun connectBackground() {
        synchronized(this) {
            if (!permissions.hasPermissions()) {
                state = State.ERROR_NO_PERMISSIONS
                // TODO: pop notif
                return
            }
            if (!bluetoothEnabled()) {
                state = State.ERROR_BT_DISABLED
                notificationManager.requestBluetoothEnable()
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
            coroutineContext.cancel("Shutting down")
        }
    }

    fun connectForeground() {
        synchronized(this) {
            if (state == State.SCANNING_FOREGROUND) return

            if (!permissions.hasPermissions()) {
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
            coroutineContext.launch {
                _advertisements.emit(result.toInRange())
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results ?: return
            Log.d("JBEACON-BT", "onBatchScanResults: $results")
            coroutineContext.launch {
                for (result in results) {
                    _advertisements.emit(result.toInRange())
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("JBEACON-BT", "scan Failed: $errorCode")
            state = State.ERROR_SCAN_FAILED
            super.onScanFailed(errorCode)
        }
    }

    private val deviceFilters = listOf(ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(SERVICE_UUID))
        .build())

    @SuppressLint("MissingPermission")
    private fun scanForeground() {
        Log.d("Scanner", "Starting Foreground Scan")
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        manager.adapter.bluetoothLeScanner.startScan(deviceFilters, settings, foregroundScanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopForeground() {
        manager.adapter.bluetoothLeScanner.stopScan(foregroundScanCallback)
    }

    private val backgroundScanPendingIntent by lazy {
//        BackgroundScanBroadcastReceiver.getScanPendingIntent(applicationContext)
        BeaconService.getScanPendingIntent(applicationContext)
    }

    @SuppressLint("MissingPermission")
    private fun scanBackground() {
        Log.d("Scanner", "Starting Background Scan")
        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setReportDelay(REPORT_FREQUENCY_BACKGROUND)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
//            .setReportDelay(ScanSettings.AUTO_BATCH_MIN_REPORT_DELAY_MILLIS)
//            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH)
            .build()
        val errorCode = manager.adapter.bluetoothLeScanner.startScan(deviceFilters, settings, backgroundScanPendingIntent)
        if (errorCode != 0) Log.e("Scanner", "scan error: $errorCode")
    }

    fun emitBackgroundAdvertisements(scanResults: List<ScanResult>) {
        coroutineContext.launch {
            for (result in scanResults) {
                _advertisements.emit(result.toInRange())
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBackground() {
        manager.adapter.bluetoothLeScanner.stopScan(backgroundScanPendingIntent)
    }

    private fun ScanResult.getAdvertisementTime(): Instant =
        startTime.plusNanos(timestampNanos - bootTimeNanos)

    @SuppressLint("MissingPermission")
    private fun ScanResult.toInRange() = Advertisement(
        address = device.address,
        rssi = rssi.toFloat(),
        lastAdvertisement = getAdvertisementTime(),
        name = device.name
    )
}
