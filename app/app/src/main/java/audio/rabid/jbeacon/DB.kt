package audio.rabid.jbeacon

import android.content.Context
import org.json.JSONObject

class DB(application: JBeaconApplication) {

    private val sharedPrefs = application.applicationContext.getSharedPreferences(
        application.packageName,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY = "devices"
    }

    fun getDevices(): List<Beacon> {
        return synchronized(this) {
            val set = sharedPrefs.getStringSet(KEY, emptySet())!!
            set.map { Beacon.fromJson(JSONObject(it).toString()) }
        }
    }

    fun setDevices(devices: List<Beacon>) {
        synchronized(this) {
            sharedPrefs.edit().putStringSet(KEY, devices.map(Beacon::toJson).toSet()).apply()
        }
    }
}