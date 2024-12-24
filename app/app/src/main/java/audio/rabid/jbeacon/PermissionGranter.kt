package audio.rabid.jbeacon

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat

class PermissionGranter(private val applicationContext: Context, val permissions: Set<String>) {

    class Requester(
        private val launcher: ActivityResultLauncher<Array<String>>,
        private val permissions: Array<String>
    ) {

        fun request() {
            launcher.launch(permissions)
        }
    }

    fun hasPermissions(): Boolean {
        for (permission in permissions) {
            when (ContextCompat.checkSelfPermission(applicationContext, permission)) {
                PackageManager.PERMISSION_GRANTED -> continue
                PackageManager.PERMISSION_DENIED -> return false
            }
        }
        return true
    }

    inline fun createPermissionRequester(
        activity: ComponentActivity,
        crossinline block: (Map<String, Boolean>) -> Unit
    ) = Requester(
        activity.registerForActivityResult(RequestMultiplePermissions()) { block(it) },
        permissions.toTypedArray()
    )

    operator fun plus(other: PermissionGranter) =
        PermissionGranter(applicationContext, permissions + other.permissions)
}
