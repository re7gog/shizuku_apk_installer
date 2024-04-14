import 'shizuku_apk_installer_platform_interface.dart';

class ShizukuApkInstaller {
  /// Returns current android platform version id.
  /// Can be useful if you want to hide this plugin options in old android versions.
  static Future<int?> getPlatformVersion() {
    return ShizukuApkInstallerPlatform.instance.getPlatformVersion();
  }

  /// Check permission and request it if not granted.
  /// Can return:
  /// "binder_not_found" - Shizuku binder not found, probably because Shizuku is not installed
  /// "old_shizuku" - Old Shizuku version (<11), user must update it
  /// "granted_adb" - Permission granted with ADB access
  /// "granted_root" - Permission granted with root access
  /// "denied" - Permission denied by user
  /// "old_android_with_adb" - Unsupported, Shizuku running on Android < 8.1 with ADB, user must update Android or use root method
  static Future<String?> checkPermission() {
    return ShizukuApkInstallerPlatform.instance.checkPermission();
  }

  /// Install APK by its URI without asking user
  /// [packageToPretendToBe] - Set "installed by ..." package property
  static Future<int?> installAPK(String apkFileURI, String packageToPretendToBe) {
    return ShizukuApkInstallerPlatform.instance.installAPKs([apkFileURI], packageToPretendToBe);
  }

  /// Install list of AAB splits of one app by their URIs without asking user
  /// [packageToPretendToBe] - Set "installed by ..." package property
  static Future<int?> installAABsplits(List<String> aabSplitsFilesURIs, String packageToPretendToBe) {
    return ShizukuApkInstallerPlatform.instance.installAPKs(aabSplitsFilesURIs, packageToPretendToBe);
  }

  /// Uninstall package by its name without asking user
  static Future<int?> uninstallPackage(String packageName) {
    return ShizukuApkInstallerPlatform.instance.uninstallPackage(packageName);
  }
}
