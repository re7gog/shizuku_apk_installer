import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'shizuku_apk_installer_platform_interface.dart';

/// An implementation of [ShizukuApkInstallerPlatform] that uses method channels.
class MethodChannelShizukuApkInstaller extends ShizukuApkInstallerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('shizuku_apk_installer');

  @override
  Future<int?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<int?>('getPlatformVersion');
    return version;
  }

  @override
  Future<String?> checkPermission() async {
    final permission = await methodChannel.invokeMethod<String?>('checkPermission');
    return permission;
  }

  @override
  Future<int?> installAPKs(List<String> apkFilesURIs, String fakeInstallSource) async {
    final success = await methodChannel.invokeMethod<int?>(
        'installAPKs',
        {
          'apkFilesURIs': apkFilesURIs,
          'fakeInstallSource': fakeInstallSource
        }
    );
    return success;
  }

  @override
  Future<int?> uninstallPackage(String packageName) async {
    final success = await methodChannel.invokeMethod<int?>(
        'uninstallPackage',
        {
          'packageName': packageName
        }
    );
    return success;
  }
}
