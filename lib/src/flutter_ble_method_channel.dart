// import 'package:flutter/services.dart';
// import 'package:flutter_ble/models/ble_response.dart';
// import '../flutter_ble_platform_interface.dart';

import 'package:flutter/services.dart';
import 'package:flutter_ble/flutter_ble.dart';
import 'package:flutter_ble/src/flutter_ble_platform_interface.dart';

class MethodChannelFlutterBle extends FlutterBlePlatform {
  final channel = const MethodChannel('com.tech.flutter_ble/ble');

  MethodChannelFlutterBle() {
    channel.setMethodCallHandler((call) {
      if (call.method == 'device_list') {
        final Map<dynamic, dynamic> data = call.arguments;
        ddController.sink.add(data);
      }
      if (call.method == 'device_status') {
        final Map<dynamic, dynamic> data = call.arguments;
        statusController.sink.add(data);
      }
      if (call.method == 'notify') {
        notifyController.sink.add(call.arguments);
      }
      if (call.method == 'bluetooth_status') {
        bluetoothController.sink.add(call.arguments);
      }
      return Future(() => null);
    });
  }

  @override
  Future<bool> connect(String address, int retryCount) async {
    return await channel.invokeMethod('connect_device', {'address': address, 'retry_count': retryCount});
  }

  @override
  Future<bool> disconnect(String address) async {
    return await channel.invokeMethod('disconnect_device', address);
  }

  @override
  void initialize({List<String> services = const []}) async {
    await channel.invokeMethod('init', services);
  }

  @override
  void start() async {
    await channel.invokeMethod('start_discovery');
  }

  @override
  void stop() async {
    await channel.invokeMethod('stop_discovery');
  }

  @override
  Future<bool> write(String address, String uuid, value) async {
    return await channel.invokeMethod('write_characteristic', {'address': address, 'uuid': uuid, 'value': value});
  }

  @override
  Future<BLEResponse> writeWithResponse(String address, String uuid, value) async {
    var data = await channel.invokeMethod('write_characteristic_with_response', {'address': address, 'uuid': uuid, 'value': value});
    return BLEResponse.fromJson(data);
  }

  @override
  Future<BLEResponse> read(String address, String uuid) async {
    var data = await channel.invokeMethod('read_characteristic', {'address': address, 'uuid': uuid});
    return BLEResponse.fromJson(data);
  }

  @override
  void notify(String address, String uuid) async {
    await channel.invokeMethod('on_notify', {'address': address, 'uuid': uuid});
  }

  @override
  void startBluetoothListener() async {
    await channel.invokeMethod('start_bluetooth_listener');
  }

  @override
  void stopBluetoothListener() async {
    await channel.invokeMethod('stop_bluetooth_listener');
  }
}
