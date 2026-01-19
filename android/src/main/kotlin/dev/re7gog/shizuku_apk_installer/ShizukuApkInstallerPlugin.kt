package dev.re7gog.shizuku_apk_installer

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async

class ShizukuApkInstallerPlugin: FlutterPlugin, MethodCallHandler {
    private var channel: MethodChannel? = null
    private var worker: ShizukuWorker? = null
    private var job: Job? = null
    private var result: Result? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        worker = ShizukuWorker(flutterPluginBinding.applicationContext)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "shizuku_apk_installer")
        channel?.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, currentResult: Result) {
        result = currentResult
        if (job?.isActive == true) {
            job?.cancel()
            result!!.error(
                "cancelled",
                "Job cancelled",
                "The job was canceled due to the creation of a new one"
            )
            return
        }
        if (worker == null) {
            result!!.error(
                "error",
                "Worker not initialized",
                "Shizuku worker hasn't been initialized"
            )
            return
        }
        when (call.method) {
            "checkPermission" -> {
                job = CoroutineScope(Dispatchers.Default).async {
                    val res = worker!!.checkPermission()
                    result!!.success(res)
                }
            }
            "installAPKs" -> {
                val apkFilesURIs: List<String>? = call.argument("apkFilesURIs")
                val fakeInstallSource: String? = call.argument("fakeInstallSource")
                if (apkFilesURIs == null || fakeInstallSource == null) {
                    result!!.error(
                        "error",
                        "Missing arguments",
                        "Arguments are null"
                    )
                    return
                }
                job = CoroutineScope(Dispatchers.Default).async {
                    val res = worker!!.installAPKs(apkFilesURIs, fakeInstallSource)
                    result!!.success(res)
                }
            }
            "uninstallPackage" -> {
                val packageName: String? = call.argument("packageName")
                if (packageName == null) {
                    result!!.error(
                        "error",
                        "Missing argument",
                        "Argument is null"
                    )
                    return
                }
                job = CoroutineScope(Dispatchers.Default).async {
                    val res = worker!!.uninstallPackage(packageName)
                    result!!.success(res)
                }
            }
            "getPlatformVersion" -> {
                result!!.success(android.os.Build.VERSION.SDK_INT)
            }
            else -> {
                result!!.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        if (job?.isActive == true) {
            job?.cancel()
            result!!.error(
                "cancelled",
                "Job cancelled",
                "The job was canceled due to the destruction of the plugin"
            )
        }
        worker?.exit()
    }
}
