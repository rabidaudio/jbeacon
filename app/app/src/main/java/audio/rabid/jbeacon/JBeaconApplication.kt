package audio.rabid.jbeacon

import android.app.Application

class JBeaconApplication : Application() {
    lateinit var beaconManager: BeaconManager
    lateinit var scanner: Scanner
    lateinit var db: DB

    override fun onCreate() {
        super.onCreate()
        scanner = Scanner(this)
        db = DB(this)
        beaconManager = BeaconManager(scanner, db)

        // TODO: check launch intent, and start either background or foreground
//        scanner.connectBackground()
    }
}
