import 'shizuku_apk_installer_platform_interface.dart';

class ShizukuApkInstaller {
  /// Returns current android platform version id.
  /// Can be useful if you want to hide this plugin options in old android versions.
  Future<int?> getPlatformVersion() {
    return ShizukuApkInstallerPlatform.instance.getPlatformVersion();
  }

  /// Check permission and request it if not granted.
  /// Can return:
  /// "services_not_found" - Shizuku and Dhizuku services not found, probably because Shizuku and Dhizuku are not installed or not configured and launched
  /// "old_shizuku" - Old Shizuku version (<11), user must update it
  /// "granted_adb" - Permission granted with ADB access
  /// "granted_root" - Permission granted with root access
  /// "granted_owner" - Permission granted with device owner access, using Dhizuku
  /// "denied" - Permission denied by user
  /// "old_android_with_adb" - Unsupported, Shizuku running on Android < 8.1 with ADB, user must update Android or use root method
  Future<String?> checkPermission() {
    return ShizukuApkInstallerPlatform.instance.checkPermission();
  }

  /// Install APK by its URI
  /// [fakeInstallSource] - Set "installed by ..." package property
  Future<int?> installAPK(String apkFileURI, String fakeInstallSource) {
    return ShizukuApkInstallerPlatform.instance.installAPKs([apkFileURI], fakeInstallSource);
  }

  /// Install list of AAB splits of one app by their URIs
  /// [fakeInstallSource] - Set "installed by ..." package property
  Future<int?> installAABSplits(List<String> aabSplitsFilesURIs, String fakeInstallSource) {
    return ShizukuApkInstallerPlatform.instance.installAPKs(aabSplitsFilesURIs, fakeInstallSource);
  }

  /// Uninstall package by its name
  Future<int?> uninstallPackage(String packageName) {
    return ShizukuApkInstallerPlatform.instance.uninstallPackage(packageName);
  }
}
