package dev.re7gog.shizuku_apk_installer

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import dev.re7gog.shizuku_apk_installer.util.IIntentSenderAdaptor
import dev.re7gog.shizuku_apk_installer.util.IntentSenderUtils
import dev.re7gog.shizuku_apk_installer.util.PackageInstallerUtils
import dev.re7gog.shizuku_apk_installer.util.ShizukuSystemServerApi
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.io.IOException

class ShizukuWorker(private val appContext: Context) {
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

    suspend fun installAPKs(apkFilesURIs: List<String>, result: MethodChannel.Result) {
        var res: Boolean
        var session: PackageInstaller.Session? = null
        try {
            val iPackageInstaller: IPackageInstaller = ShizukuSystemServerApi.PackageManager_getPackageInstaller()
            val isRoot = Shizuku.getUid() == 0
            // The reason for use "com.android.shell" as installer package under ADB
            // is that getMySessions will check installer package's owner
            val installerPackageName = if (isRoot) appContext.packageName else "com.android.shell"
            var installerAttributionTag: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                installerAttributionTag = appContext.attributionTag
            }
            val userId = if (isRoot) Process.myUserHandle().hashCode() else 0

            val packageInstaller = PackageInstallerUtils.createPackageInstaller(
                iPackageInstaller, installerPackageName, installerAttributionTag, userId)

            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            var installFlags: Int = PackageInstallerUtils.getInstallFlags(params)
            installFlags = installFlags or (0x00000002 /*PackageManager.INSTALL_REPLACE_EXISTING*/
                    or 0x00000004 /*PackageManager.INSTALL_ALLOW_TEST*/)
            PackageInstallerUtils.setInstallFlags(params, installFlags)

            val sessionId = packageInstaller.createSession(params)
            val iSession = IPackageInstallerSession.Stub.asInterface(
                ShizukuBinderWrapper(iPackageInstaller.openSession(sessionId).asBinder()))
            session = PackageInstallerUtils.createSession(iSession)

            for ((apkI, uriStr) in apkFilesURIs.withIndex()) {
                val uri = Uri.parse(uriStr)
                val inputStream = appContext.contentResolver.openInputStream(uri)!!
                val openedSession = session.openWrite("$apkI.apk", 0, -1)

                val buffer = ByteArray(8192)
                var length: Int
                withContext(Dispatchers.IO) {
                    try {
                        while (inputStream.read(buffer).also { length = it } > 0) {
                            openedSession.write(buffer, 0, length)
                            openedSession.flush()
                            session.fsync(openedSession)
                        }
                    } finally {
                        try {
                            inputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        try {
                            openedSession.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                delay(1000)
            }

            val results = arrayOf<Intent?>(null)
            val mutex = Mutex(locked = true)
            val intentSender: IntentSender =
                IntentSenderUtils.newInstance(object : IIntentSenderAdaptor() {
                    override fun send(intent: Intent?) {
                        results[0] = intent
                        mutex.unlock()
                    }
                })
            session.commit(intentSender)
            mutex.lock()
            res = results[0]!!.getIntExtra(
                PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE) == 0
        } catch (ex: Exception) {
            ex.printStackTrace()
            res = false
        } finally {
            if (session != null) {
                try {
                    session.close()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    res = false
                }
            }
        }
        result.success(res)
    }
}