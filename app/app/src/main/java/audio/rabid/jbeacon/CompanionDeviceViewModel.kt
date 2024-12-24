package audio.rabid.jbeacon

import android.app.Application
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class CompanionDeviceViewModel(application: Application) : AndroidViewModel(application) {

//    private val scanner
//        get() = getApplication<JBeaconApplication>().scanner

//    private val beaconManager
//        get() = getApplication<JBeaconApplication>().beaconManager
//
//    private val notificationManager
//        get() = getApplication<JBeaconApplication>().notificationManager

    companion object {
        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

        val filter
            get() = BluetoothDeviceFilter.Builder()
//                .addServiceUuid(ParcelUuid(SERVICE_UUID), null))
                .setAddress("20:C3:8F:D6:32:04")
                .build()
    }

    private val deviceManager
        get() = getApplication<JBeaconApplication>()
            .getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager

    private val _associationIntentSender = MutableStateFlow<IntentSender?>(null)
    val associationIntentSender get() = _associationIntentSender.asStateFlow()

    fun start() {
        val req = AssociationRequest.Builder()
            .addDeviceFilter(filter)
            .setSingleDevice(false)
            .setDisplayName(getApplication<JBeaconApplication>().packageName)
            .build()

        deviceManager.associate(req, { it.run() }, object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                super.onAssociationPending(intentSender)
                _associationIntentSender.value = intentSender
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                super.onAssociationCreated(associationInfo)
                _associationIntentSender.value = null
            }

            override fun onFailure(error: CharSequence?) {
                Log.e("CompanionDeviceViewModel", "associate onFailure: $error")
                _associationIntentSender.value = null
            }
        })
    }

    fun onDeviceSelected(scanResult: ScanResult) {
        // put in memory
        // emit statuses
    }

    fun onDeviceSelectCanceled() {
        _associationIntentSender.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}