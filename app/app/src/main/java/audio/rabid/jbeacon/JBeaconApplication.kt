package audio.rabid.jbeacon

import android.app.Application

class JBeaconApplication : Application() {
    lateinit var deviceFinder: DeviceFinder
    lateinit var db: DB

    override fun onCreate() {
        super.onCreate()
        deviceFinder = DeviceFinder(this)
        db = DB(this)
    }
}
