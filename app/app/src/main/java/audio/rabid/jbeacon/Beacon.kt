package audio.rabid.jbeacon

import org.json.JSONException
import org.json.JSONObject
import java.time.Instant

data class Beacon(
    val name: String,
    val macAddress: String,
    val lastSeen: Instant
) {

    companion object {
        @Throws(JSONException::class)
        fun fromJson(json: String): Beacon {
            val obj = JSONObject(json)
            return Beacon(
                name = obj.getString("name"),
                macAddress = obj.getString("mac_address"),
                lastSeen = Instant.ofEpochMilli(obj.getLong("last_seen"))
            )
        }
    }

    fun toJson(): String {
        return JSONObject()
            .put("name", name)
            .put("mac_address", macAddress)
            .put("last_seen", lastSeen.toEpochMilli())
            .toString()
    }
}
