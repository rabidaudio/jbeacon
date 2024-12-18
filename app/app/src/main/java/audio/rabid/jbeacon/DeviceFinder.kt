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
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

class DeviceFinder(private val applicationContext: Context) {

    companion object {
        @SuppressLint("ObsoleteSdkInt")
        val requiredPermissions = buildList {
            if (Build.VERSION.SDK_INT < 31) add(Manifest.permission.BLUETOOTH)
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val REPORT_FREQUENCY_BACKGROUND = 30*1000L

        val ADVERTISEMENT_TIMEOUT = 1*60*1000L
    }

    private val manager = applicationContext.getSystemService(BluetoothManager::class.java)

    private var scanning = false
    private val flow = MutableStateFlow<Map<String, Pair<ScanResult, Instant>>>(emptyMap())

    fun hasPermissions(): Boolean {
        for (permission in requiredPermissions) {
            when (ContextCompat.checkSelfPermission(applicationContext, permission)) {
                PackageManager.PERMISSION_GRANTED -> continue
                    PackageManager.PERMISSION_DENIED -> return false
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun scan(macAddresses: List<String>): StateFlow<Map<String, Pair<ScanResult, Instant>>> {
        synchronized(this) {
            if (scanning) return flow
            scanning = true
        }
        val filters = macAddresses.map { address ->
            ScanFilter.Builder().setDeviceAddress(address).build()
        }
//        val settings = ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
////                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
//            .setReportDelay(REPORT_FREQUENCY_BACKGROUND)
//            // TODO: back-support older sdk versions (e.g. 26)
//            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH)
//            .build()

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()

        val inRange = mutableMapOf<String, Pair<ScanResult, Instant>>()

        // TODO: switch to pending intent method
        manager.adapter.bluetoothLeScanner.startScan(null, settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return
                Log.i("JBEACON-BT", "onScanResult: $callbackType $result")
                when (callbackType) {
                        ScanSettings.CALLBACK_TYPE_FIRST_MATCH,
                        ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
                        ScanSettings.CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH ->
                            inRange[result.device.address] = Pair(result, Instant.now())

                    ScanSettings.CALLBACK_TYPE_MATCH_LOST -> inRange.remove(result.device.address)
                    else -> throw Error("Expected callback type: $callbackType")
                }
                flow.value = inRange
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results ?: return
                Log.i("JBEACON-BT", "onBatchScanResults: $results")
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("JBEACON-BT", "scan Failed: $errorCode")
                scanning = false
                super.onScanFailed(errorCode)
            }

//            fun clearOutdated() {
//                inRange.filterValues { (_, t) -> t.isBefore() }
//            }
        })
        return flow
    }
}
