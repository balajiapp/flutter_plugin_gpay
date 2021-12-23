export 'package:flutter_plugin_gpay/src/api.dart' show UpiPay;
export 'package:flutter_plugin_gpay/src/applications.dart' show UpiApplication;
export 'package:flutter_plugin_gpay/src/response.dart'
    show UpiTransactionResponse, UpiTransactionStatus;
export 'package:flutter_plugin_gpay/src/meta.dart' show ApplicationMeta;
export 'package:flutter_plugin_gpay/src/discovery.dart'
    show
    UpiApplicationDiscoveryAppStatusType,
    UpiApplicationDiscoveryAppPaymentType;


/*
import 'dart:async';

import 'package:flutter/services.dart';

class FlutterPluginGpay {
  static const MethodChannel _channel = MethodChannel('flutter_plugin_gpay');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
*/