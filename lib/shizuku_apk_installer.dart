
import 'shizuku_apk_installer_platform_interface.dart';

class ShizukuApkInstaller {
  Future<String?> getPlatformVersion() {
    return ShizukuApkInstallerPlatform.instance.getPlatformVersion();
  }
}
