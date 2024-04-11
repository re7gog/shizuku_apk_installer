import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'shizuku_apk_installer_platform_interface.dart';

/// An implementation of [ShizukuApkInstallerPlatform] that uses method channels.
class MethodChannelShizukuApkInstaller extends ShizukuApkInstallerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('shizuku_apk_installer');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<String?> checkPermission() async {
    final permission = await methodChannel.invokeMethod<String>('checkPermission');
    return permission;
  }

  @override
  Future<bool?> installAPKs(List<String> apkFilesURIs) async {
    final success = await methodChannel.invokeMethod<bool>('installAPKs', {'apkFilesURIs': apkFilesURIs});
    return success;
  }

  @override
  Future<bool?> uninstallPackage(String packageName) async {
    final success = await methodChannel.invokeMethod<bool>('uninstallPackage', {'packageName': packageName});
    return success;
  }
}
