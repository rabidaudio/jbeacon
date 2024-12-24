package audio.rabid.jbeacon

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
import android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED
import android.media.AudioAttributes.USAGE_ALARM
import android.net.Uri
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import android.text.format.DateUtils
import androidx.compose.ui.graphics.toArgb
import audio.rabid.jbeacon.ui.theme.Colors
import java.text.DateFormat
import java.time.Instant


class NotificationManager(private val applicationContext: Context) {

    private val androidNotificationManager =
        applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val DEVICE_STATUS_CHANNEL_ID = "audio.rabid.jbeacon.notifications.DEVICE_STATUS"
    }

    val permissions = PermissionGranter(applicationContext, setOf(
        Manifest.permission.POST_NOTIFICATIONS
    ))

    private val notificationSound
        get() = Uri.parse("android.resource://${applicationContext.packageName}/${R.raw.forgetting}")

    private val deviceStatusChannel = NotificationChannel(DEVICE_STATUS_CHANNEL_ID,
        "Device Status", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Get alerts when a device moves out of range"
        enableLights(true)
        enableVibration(true)
        setSound(notificationSound, AudioAttributes.Builder()
            .setUsage(USAGE_ALARM)
            .setFlags(FLAG_AUDIBILITY_ENFORCED)
            .setContentType(CONTENT_TYPE_SONIFICATION)
            .build())
    }

    fun registerChannels() {
        if (!permissions.hasPermissions()) return

        androidNotificationManager.createNotificationChannel(deviceStatusChannel)
    }

    private val appLaunchPendingIntent
        get() = TaskStackBuilder.create(applicationContext).apply {
                addNextIntentWithParentStack(Intent(applicationContext, MainActivity::class.java))
            }.getPendingIntent(0,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun showBeaconLost(beacon: Beacon) {
        if (!androidNotificationManager.areNotificationsEnabled()) return

        val lastSeen = beacon.lastSeen.formatSameDayTime()
        val notif = Notification.Builder(applicationContext, DEVICE_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_nearby_error_24)
            .setColor(Colors.danger.toArgb())
            .setColorized(true)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setContentIntent(appLaunchPendingIntent)
            .setSubText("Did you forget something?")
            .setContentText("${beacon.name} is no longer in range. Last seen: $lastSeen")
            .build()

        androidNotificationManager.notify(beacon.stableId, notif)
    }

    private fun vibrate() {
        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        applicationContext.getSystemService(VibratorManager::class.java)
            .vibrate(CombinedVibration.createParallel(effect))
    }

    private fun Instant.formatSameDayTime() =
        DateUtils.formatSameDayTime(toEpochMilli(), Instant.now().toEpochMilli(),
            DateFormat.SHORT, DateFormat.SHORT)
}