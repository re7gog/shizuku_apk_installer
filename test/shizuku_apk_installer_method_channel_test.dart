import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shizuku_apk_installer/shizuku_apk_installer_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelShizukuApkInstaller platform = MethodChannelShizukuApkInstaller();
  const MethodChannel channel = MethodChannel('shizuku_apk_installer');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
