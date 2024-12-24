package audio.rabid.jbeacon

import android.app.Application

class JBeaconApplication : Application() {
    lateinit var beaconManager: BeaconManager
    lateinit var scanner: Scanner
    lateinit var db: DB
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        db = DB(this)
        notificationManager = NotificationManager(this)
        scanner = Scanner(this, notificationManager)
        beaconManager = BeaconManager(scanner, db)
    }
}
