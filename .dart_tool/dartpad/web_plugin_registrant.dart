// Flutter web plugin registrant file.
//
// Generated file. Do not edit.
//

// @dart = 2.13
// ignore_for_file: type=lint

import 'package:flutter_secure_storage_web/flutter_secure_storage_web.dart';
import 'package:google_sign_in_web/google_sign_in_web.dart';
import 'package:kakao_flutter_sdk_common/src/web/kakao_flutter_sdk_plugin.dart';
import 'package:shared_preferences_web/shared_preferences_web.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';

void registerPlugins([final Registrar? pluginRegistrar]) {
  final Registrar registrar = pluginRegistrar ?? webPluginRegistrar;
  FlutterSecureStorageWeb.registerWith(registrar);
  GoogleSignInPlugin.registerWith(registrar);
  KakaoFlutterSdkPlugin.registerWith(registrar);
  SharedPreferencesPlugin.registerWith(registrar);
  registrar.registerMessageHandler();
}
