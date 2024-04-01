package dev.re7gog.shizuku_apk_installer

import android.content.pm.PackageManager
import android.os.Build
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.sync.Mutex
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku

class ShizukuWorker {
    private val reqPermCode = (1000..2000).random()
    private val reqPermListenerMutex = Mutex(locked = true)
    private var reqPermListenerResult = "undefined"
    private val requestPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            if (requestCode == reqPermCode) {
                reqPermListenerResult = if (grantResult == PackageManager.PERMISSION_GRANTED) "granted" else "denied"
                reqPermListenerMutex.unlock()
            }
        }

    fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
        Shizuku.addRequestPermissionResultListener(requestPermissionListener)
    }

    fun exit() {
        Shizuku.removeRequestPermissionResultListener(requestPermissionListener)
    }

    suspend fun checkPermission(result: MethodChannel.Result) {
        try {
            if (Shizuku.isPreV11()) {
                result.success("unsupported")
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                result.success("granted")
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {  // Deny and don't ask again
                result.success("denied")
            } else {
                Shizuku.requestPermission(reqPermCode)
                reqPermListenerMutex.lock()
                result.success(reqPermListenerResult)
            }
        } catch (_: Exception) {  // Shizuku binder not found
            result.success("not_found")
        }
    }
}