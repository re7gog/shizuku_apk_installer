package dev.re7gog.shizuku_apk_installer

import android.annotation.SuppressLint
import android.content.Context
import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.Process
import android.util.Log
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import rikka.sui.Sui
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Casting unsafe spells 🧙‍♂️🔮
 */
class ShizukuWizard(private val appContext: Context) {
    private var isBinderAvailable = false
    private val requestPermissionCode = 1945
    private val requestPermissionMutex by lazy { Mutex(locked = true) }
    private var permissionGranted: Boolean? = null
    private var isRoot: Boolean? = null
    private var packageToPretendToBe = ""

    fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content", "Landroid/os")
        val isSui = Sui.init(appContext.packageName)
        if (!isSui) {  // Not sure if it's needed
            ShizukuProvider.enableMultiProcessSupport(false)
            ShizukuProvider.requestBinderForNonProviderProcess(appContext)
        }
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
    }

    fun exit() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { isBinderAvailable = true }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { isBinderAvailable = false }
    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            if (requestCode == requestPermissionCode) {
                permissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
                requestPermissionMutex.unlock()
            }
        }

    suspend fun checkPermission(): String {
        return if (!isBinderAvailable) {
            "binder_not_found"
        } else if (Shizuku.isPreV11()) {
            "old_shizuku"
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            if (!registerUidObserverPermissionLimitedCheck()){
                "granted_" + if (isRoot!!) "root" else "adb"
            } else "old_android_with_adb"
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {  // "Deny and don't ask again"
            "denied"
        } else {
            Shizuku.requestPermission(requestPermissionCode)
            requestPermissionMutex.lock()
            if (!registerUidObserverPermissionLimitedCheck()) {
                if (permissionGranted!!){
                    "granted_" + if (isRoot!!) "root" else "adb"
                } else "denied"
            } else "old_android_with_adb"
        }
    }

    /**
     * Android 8.0 with ADB lacks IActivityManager#registerUidObserver permission, so we can't install apps without activity
     */
    private fun registerUidObserverPermissionLimitedCheck(): Boolean {
        isRoot = Shizuku.getUid() == 0
        return !isRoot!! and (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1)
    }

    // Thanks to https://github.com/LSPosed/LSPatch and https://gitlab.com/AuroraOSS/AuroraStore

    private fun IBinder.wrap() = ShizukuBinderWrapper(this)
    private fun IInterface.asShizukuBinder() = this.asBinder().wrap()

    private val iPackageInstaller: IPackageInstaller by lazy {
        val iPackageManager = IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package").wrap())
        IPackageInstaller.Stub.asInterface(iPackageManager.packageInstaller.asShizukuBinder())
    }

    private val packageInstaller: PackageInstaller by lazy {
        val installerPackageName = if (packageToPretendToBe == "") appContext.packageName else packageToPretendToBe
        val userId = if (!isRoot!!) Process.myUserHandle().hashCode() else 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Refine.unsafeCast(PackageInstallerHidden(iPackageInstaller, installerPackageName, appContext.attributionTag, userId))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Refine.unsafeCast(PackageInstallerHidden(iPackageInstaller, installerPackageName, userId))
        } else {
            Refine.unsafeCast(PackageInstallerHidden(appContext, appContext.packageManager, iPackageInstaller, installerPackageName, userId))
        }
    }

    private val sessionParams: PackageInstaller.SessionParams by lazy {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        var flags = Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags
        flags = flags or PackageManagerHidden.INSTALL_ALLOW_TEST or PackageManagerHidden.INSTALL_REPLACE_EXISTING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            flags = flags or PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
        }
        Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params).installFlags = flags
        params
    }

    private fun createPackageInstallerSession(): PackageInstaller.Session {
        val sessionId = packageInstaller.createSession(sessionParams)
        val iSession = IPackageInstallerSession.Stub.asInterface(iPackageInstaller.openSession(sessionId).asShizukuBinder())
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }

    /**
     * Install a list of APK splits (AAB) using their URIs without asking user.
     * The permission must have already been checked!
     * @param packageToPretendToBe pretend to be the provided package installer (root access is not needed)
     */
    suspend fun installAPKs(apkURIs: List<String>, packageToPretendToBe: String = ""): Int {
        isRoot = Shizuku.getUid() == 0
        this.packageToPretendToBe = packageToPretendToBe
        var status = PackageInstaller.STATUS_FAILURE
        withContext(Dispatchers.IO) {
            runCatching {
                createPackageInstallerSession().use { session ->
                    apkURIs.forEachIndexed { index, uriString ->
                        val uri = Uri.parse(uriString)
                        val stream = appContext.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open input stream")
                        stream.use {
                            session.openWrite("$index.apk", 0, stream.available().toLong()).use { output ->
                                stream.copyTo(output)
                                session.fsync(output)
                            }
                        }
                    }
                    var result: Intent? = null
                    suspendCoroutine { cont ->
                        val adapter = IntentSenderHelper.IIntentSenderAdaptor { intent ->
                            result = intent
                            cont.resume(Unit)
                        }
                        val intentSender = IntentSenderHelper.newIntentSender(adapter)
                        session.commit(intentSender)
                    }
                    result?.let {
                        status = it.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                        val message = it.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No message"
                        Log.i("shizuku_apk_installer", "Package installer result: $message")
                    } ?: throw IOException("Intent is null")
                }
            }.onFailure {
                val message = it.message + "\n" + it.stackTraceToString()
                Log.e("shizuku_apk_installer", "Installing error: $message")
            }
        }
        return status
    }

    /**
     * Uninstall a package (app) by its name without asking user.
     * The permission must have already been checked!
     * android.permission.REQUEST_DELETE_PACKAGES is not needed.
     */
    @SuppressLint("MissingPermission")
    suspend fun uninstallPackage(packageName: String): Int {
        var status = PackageInstaller.STATUS_FAILURE
        withContext(Dispatchers.IO) {
            runCatching {
                var result: Intent? = null
                suspendCoroutine { cont ->
                    val adapter = IntentSenderHelper.IIntentSenderAdaptor { intent ->
                        result = intent
                        cont.resume(Unit)
                    }
                    val intentSender = IntentSenderHelper.newIntentSender(adapter)
                    packageInstaller.uninstall(packageName, intentSender)
                }
                result?.let {
                    status = it.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    val message = it.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No message"
                    Log.i("shizuku_apk_installer", "Package uninstaller result: $message")
                } ?: throw IOException("Intent is null")
            }.onFailure {
                val message = it.message + "\n" + it.stackTraceToString()
                Log.e("shizuku_apk_installer", "Uninstalling error: $message")
            }
        }
        return status
    }
}


object IntentSenderHelper {
    fun newIntentSender(binder: IIntentSender): IntentSender {
        return IntentSender::class.java.getConstructor(IIntentSender::class.java).newInstance(binder)
    }

    class IIntentSenderAdaptor(private val listener: (Intent) -> Unit) : IIntentSender.Stub() {
        override fun send(
            code: Int,
            intent: Intent,
            resolvedType: String?,
            finishedReceiver: IIntentReceiver?,
            requiredPermission: String?,
            options: Bundle?
        ): Int {
            listener(intent)
            return 0
        }

        override fun send(
            code: Int,
            intent: Intent,
            resolvedType: String?,
            whitelistToken: IBinder?,
            finishedReceiver: IIntentReceiver?,
            requiredPermission: String?,
            options: Bundle?
        ) {
            listener(intent)
        }
    }
}
