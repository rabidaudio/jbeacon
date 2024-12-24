package audio.rabid.jbeacon

import android.util.Log
import audio.rabid.jbeacon.Scanner.Advertisement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import java.time.Instant

typealias DeviceSet = Map<MacAddress, Advertisement>
typealias BeaconStatuses = Map<Beacon, BeaconManager.BeaconStatus>

/**
 * Exposes the state of beacons based on bluetooth advertisement data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BeaconManager(
    private val scanner: Scanner,
    private val db: DB
) {

    sealed class BeaconStatus {
        abstract val lastSeen: Instant

        data class Unknown(override val lastSeen: Instant) : BeaconStatus()
        data class InRange(val rssi: Float, override val lastSeen: Instant) : BeaconStatus()
        data class OutOfRange(override val lastSeen: Instant) : BeaconStatus()
    }

    companion object {
        const val ADVERTISEMENT_TIMEOUT = 1 * 60 * 1000L
    }

    private val beacons = MutableStateFlow<Set<Beacon>>(emptySet())

    init {
        beacons.value = db.getDevices().toSet()
    }

    private fun inRangeDevices(): Flow<DeviceSet> {
        return scanner.advertisements
            .accumulateByAddress()
            // emit the cached value from the last time we were scanning
            .onStart { emit(db.getInRange()) }
            .expireOldDevices()
            // Save results to cache
            .onEach {
                Log.d("SCANNER", "in range device count: ${it.size}")
            }
            .onEach { db.setInRange(it) }
    }

    /**
     * A list of beacons in range that have not been added to be tracked
     */
    fun inRangeNewDevices(): Flow<Set<Advertisement>> {
        return inRangeDevices().combine(beacons) { inRange, knownBeacons ->
            val knownAddrs = knownBeacons.map { it.macAddress }.toSet()
            inRange.filterKeys { addr -> !knownAddrs.contains(addr) }.values.toSet()
        }
    }

    /**
     * The current status of all known beacons
     */
    fun knownDeviceStatuses(): Flow<BeaconStatuses> {
        return inRangeDevices().combine(beacons) { inRange, knownBeacons ->
            knownBeacons.associateWith { beacon ->
                when (val advertisement = inRange[beacon.macAddress]) {
                    null -> BeaconStatus.OutOfRange(beacon.lastSeen)
                    else -> BeaconStatus.InRange(
                        advertisement.rssi,
                        advertisement.lastAdvertisement
                    )
                }
            }
        }.onEach { statuses ->
            // update last-seen
            db.setDevices(statuses.map { (beacon, status) -> beacon.copy(lastSeen = status.lastSeen) })
        }.onStart {
            // start with an unknown state until the first advertisement comes in
            beacons.value.associateWith { BeaconStatus.Unknown(it.lastSeen) }
        }
    }

    /**
     * Triggered when a beacon is no longer detected
     */
    fun beaconLost(): Flow<Beacon> {
        return knownDeviceStatuses().compareToPrevious { oldInRange, newInRange ->
            buildSet {
                for ((beacon, status) in newInRange) {
                    when (status) {
                        is BeaconStatus.Unknown -> continue // still pending
                        is BeaconStatus.InRange -> continue // still in range
                        is BeaconStatus.OutOfRange -> when (oldInRange[beacon]) {
                            is BeaconStatus.InRange -> add(beacon)
                            is BeaconStatus.OutOfRange,
                            is BeaconStatus.Unknown -> continue // was already out of range
                            null -> continue // new device
                        }
                    }
                }
            }
        }.transform { lostBeacons ->
            for (lost in lostBeacons) {
                emit(lost)
            }
        }
    }

    fun addBeacon(beacon: Beacon) {
        val newSet = beacons.value.plus(beacon)
        db.setDevices(newSet.toList())
        beacons.value = newSet
    }

    fun removeBeacon(beacon: Beacon) {
        val newSet = beacons.value.minus(beacon)
        db.setDevices(newSet.toList())
        beacons.value = newSet
    }

    private fun Flow<Advertisement>.accumulateByAddress(): Flow<DeviceSet> =
        runningFold(emptyMap()) { accumulator, value ->
            accumulator.plus(Pair(value.address, value))
        }

    private fun Flow<DeviceSet>.expireOldDevices(): Flow<DeviceSet> = transformLatest { inRange ->
        var inRangeMutable = inRange
        emit(inRangeMutable)

        while (inRangeMutable.isNotEmpty()) {
            val nextExpiryMillis = inRangeMutable.values.minOf { it.expiresInMillis() }
            if (nextExpiryMillis > 0) {
                delay(nextExpiryMillis)
            }
            inRangeMutable = inRangeMutable.filterValues { a -> !a.isExpired() }
            emit(inRangeMutable)
        }
    }

    private fun Advertisement.timeSinceMillis(): Long =
        Instant.now().toEpochMilli() - lastAdvertisement.toEpochMilli()

    private fun Advertisement.expiresInMillis(): Long = ADVERTISEMENT_TIMEOUT - timeSinceMillis()

    private fun Advertisement.isExpired(): Boolean = timeSinceMillis() >= ADVERTISEMENT_TIMEOUT
}

private fun <T, V> Flow<T>.compareToPrevious(block: (prev: T, current: T) -> V): Flow<V> =
    runningFold(emptyList<T>()) { acc, value ->
        if (acc.isEmpty()) listOf(value) else listOf(acc.last(), value)
    }.map { list -> block(list[0], list[1]) }
