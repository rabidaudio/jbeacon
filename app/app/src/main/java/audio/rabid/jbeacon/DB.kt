package audio.rabid.jbeacon

import android.content.Context
import org.json.JSONObject

class DB(application: JBeaconApplication) {

    private val sharedPrefs = application.applicationContext.getSharedPreferences(
        application.packageName,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val DEVICES_KEY = "devices"
        private const val ADVERTISEMENTS_KEY = "advertisements"
    }

    fun getDevices(): List<Beacon> {
        return synchronized(this) {
            val set = sharedPrefs.getStringSet(DEVICES_KEY, emptySet())!!
            set.map { Beacon.fromJson(JSONObject(it).toString()) }
        }
    }

    fun setDevices(devices: List<Beacon>) {
        synchronized(this) {
            sharedPrefs.edit().putStringSet(DEVICES_KEY, devices.map(Beacon::toJson).toSet()).apply()
        }
    }

    fun getInRange(): DeviceSet {
        synchronized(this) {
            val set = sharedPrefs.getStringSet(ADVERTISEMENTS_KEY, emptySet())!!
            return set.map { Scanner.Advertisement.fromJson(it) }.associateBy { it.address }
        }
    }

    fun setInRange(inRange: DeviceSet) {
        synchronized(this) {
            sharedPrefs.edit()
                .putStringSet(ADVERTISEMENTS_KEY, inRange.values.map(Scanner.Advertisement::toJson).toSet())
                .apply()
        }
    }
}