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
    private lateinit var channel: MethodChannel
    private lateinit var worker: ShizukuWizard
    private var job: Job? = null
    private var result: Result? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        worker = ShizukuWizard(flutterPluginBinding.applicationContext)
        worker.init()
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "shizuku_apk_installer")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, currentResult: Result) {
        if (job?.isActive == true) {
            job!!.cancel()
            result!!.error("cancelled", "Job cancelled",
                "The job was canceled due to the creation of another new one")
        }
        result = currentResult
        when (call.method) {
            "checkPermission" -> {
                job = CoroutineScope(Dispatchers.Default).async {
                    val res = worker.checkPermission()
                    result!!.success(res)
                }
            }
            "installAPKs" -> {
                val apkFilesURIs: List<String> = call.argument("apkFilesURIs")!!
                val packageToPretendToBe: String = call.argument("packageToPretendToBe")!!
                job = CoroutineScope(Dispatchers.Default).async {
                    val res = worker.installAPKs(apkFilesURIs, packageToPretendToBe)
                    result!!.success(res)
                }
            }
            "uninstallPackage" -> {
                val packageName: String = call.argument("packageName")!!
                job = CoroutineScope(Dispatchers.Default).async {
                    val res = worker.uninstallPackage(packageName)
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
        channel.setMethodCallHandler(null)
        if (job?.isActive == true) {
            job!!.cancel()
            result!!.error("destroyed", "Job destroyed",
                "The job was canceled due to the destruction of the plugin")
        }
        worker.exit()
    }
}
