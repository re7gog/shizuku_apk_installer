import 'package:flutter_test/flutter_test.dart';
import 'package:shizuku_apk_installer/shizuku_apk_installer.dart';
import 'package:shizuku_apk_installer/shizuku_apk_installer_platform_interface.dart';
import 'package:shizuku_apk_installer/shizuku_apk_installer_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockShizukuApkInstallerPlatform
    with MockPlatformInterfaceMixin
    implements ShizukuApkInstallerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<String?> checkPermission() {
    // TODO: implement checkPermission
    throw UnimplementedError();
  }

  @override
  Future<bool?> installAPKs(List<String> apkFilesURIs) {
    // TODO: implement installAPKs
    throw UnimplementedError();
  }

  @override
  Future<bool?> uninstallPackage(String packageName) {
    // TODO: implement uninstallPackage
    throw UnimplementedError();
  }
}

void main() {
  final ShizukuApkInstallerPlatform initialPlatform = ShizukuApkInstallerPlatform.instance;

  test('$MethodChannelShizukuApkInstaller is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelShizukuApkInstaller>());
  });

  test('getPlatformVersion', () async {
    ShizukuApkInstaller shizukuApkInstallerPlugin = ShizukuApkInstaller();
    MockShizukuApkInstallerPlatform fakePlatform = MockShizukuApkInstallerPlatform();
    ShizukuApkInstallerPlatform.instance = fakePlatform;

    expect(await shizukuApkInstallerPlugin.getPlatformVersion(), '42');
  });
}
