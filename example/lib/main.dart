import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:shizuku_apk_installer/shizuku_apk_installer.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _permission = 'Not requested yet';
  String _installRes = 'Not installed yet';
  final _installerPackageNameCtrl = TextEditingController();
  String _uninstallRes = 'Not uninstalled yet';
  final _uninstallPackageNameCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion = (await ShizukuApkInstaller.getPlatformVersion())?.toString() ?? 'Failed to get platform version.';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  Future<void> checkPermission() async {
    String permission;
    try {
      permission =
          await ShizukuApkInstaller.checkPermission() ?? 'Unknown permission state';
    } on PlatformException {
      permission = 'Failed to get permission state.';
    }
    setState(() {
      _permission = permission;
    });
  }

  Future<void> installAPKs() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      type: FileType.custom, allowedExtensions: ['apk']
    );
    if (result != null) {
      String fileURI = result.paths.map((path) => 'file://${path!}').toList().first;
      String packageInstaller = _installerPackageNameCtrl.text;
      int? resInt = await ShizukuApkInstaller.installAPK(fileURI, packageInstaller);
      String res = resInt! == 0 ? "Success" : "Fail";
      setState(() {
        _installRes = res;
      });
    } else {
      // User canceled the picker
    }
  }

  Future<void> uninstallPackage() async {
    String text = _uninstallPackageNameCtrl.text;
    if (text != "") {
      int? resInt = await ShizukuApkInstaller.uninstallPackage(text);
      String res = resInt! == 0 ? "Success" : "Fail";
      setState(() {
        _uninstallRes = res;
      });
    } else {
      // User canceled the picker
    }
  }

  @override
  Widget build(BuildContext context) {
    const box = SizedBox(height: 32);
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Center(
            child: Column(
                children: [
                  Text('Running on: $_platformVersion\n'),
                  TextButton(
                      onPressed: checkPermission,
                      child: const Text('Check Shizuku Permission')
                  ),
                  Text(_permission),
                  box,
                  TextFormField(
                      controller: _installerPackageNameCtrl,
                      decoration: const InputDecoration(
                        border: UnderlineInputBorder(),
                        labelText: 'Enter the package installer name to pretend to be it',
                      )
                  ),
                  TextButton(
                      onPressed: installAPKs,
                      child: const Text('Pick and install APK')
                  ),
                  Text(_installRes),
                  box,
                  TextFormField(
                    controller: _uninstallPackageNameCtrl,
                    decoration: const InputDecoration(
                      border: UnderlineInputBorder(),
                      labelText: 'Enter package name to uninstall',
                    )
                  ),
                  TextButton(
                      onPressed: uninstallPackage,
                      child: const Text("Uninstall package with given name")
                  ),
                  Text(_uninstallRes)
                ]
            ),
          )
      ),
    );
  }
}
