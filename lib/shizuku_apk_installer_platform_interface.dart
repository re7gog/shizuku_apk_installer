import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'shizuku_apk_installer_method_channel.dart';

abstract class ShizukuApkInstallerPlatform extends PlatformInterface {
  /// Constructs a ShizukuApkInstallerPlatform.
  ShizukuApkInstallerPlatform() : super(token: _token);

  static final Object _token = Object();

  static ShizukuApkInstallerPlatform _instance = MethodChannelShizukuApkInstaller();

  /// The default instance of [ShizukuApkInstallerPlatform] to use.
  ///
  /// Defaults to [MethodChannelShizukuApkInstaller].
  static ShizukuApkInstallerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [ShizukuApkInstallerPlatform] when
  /// they register themselves.
  static set instance(ShizukuApkInstallerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<int?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> checkPermission() {
    throw UnimplementedError('checkPermission() has not been implemented.');
  }

  Future<int?> installAPKs(List<String> apkFilesURIs, String packageToPretendToBe) {
    throw UnimplementedError('installAPKs() has not been implemented.');
  }

  Future<int?> uninstallPackage(String packageName) {
    throw UnimplementedError('packageName() has not been implemented.');
  }
}
