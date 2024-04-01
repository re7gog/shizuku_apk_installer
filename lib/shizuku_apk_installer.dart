
import 'shizuku_apk_installer_platform_interface.dart';

class ShizukuApkInstaller {
  Future<String?> getPlatformVersion() {
    return ShizukuApkInstallerPlatform.instance.getPlatformVersion();
  }
  Future<String?> checkPermission() {
    return ShizukuApkInstallerPlatform.instance.checkPermission();
  }
}
