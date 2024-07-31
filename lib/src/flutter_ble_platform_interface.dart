import 'dart:async';

import 'package:simple_flutter_ble/simple_flutter_ble.dart';
import 'package:simple_flutter_ble/src/flutter_ble_method_channel.dart';

abstract class FlutterBlePlatform {
  static FlutterBlePlatform instance = MethodChannelFlutterBle();

  StreamController ddController = StreamController.broadcast();
  StreamController<Map<dynamic, dynamic>> statusController =
      StreamController.broadcast();
  StreamController notifyController = StreamController.broadcast();
  StreamController<bool> bluetoothController = StreamController.broadcast();

  Stream get discoveredDevices => ddController.stream;
  Stream<Map<dynamic, dynamic>> get status => statusController.stream;
  Stream get onNotify => notifyController.stream;
  Stream<bool> get bluetoothListener => bluetoothController.stream;

  void initialize({List<String> services = const []});
  void start();
  void stop();
  Future<bool> connect(String address, int retryCount);
  Future<bool> disconnect(String address);
  Future<bool> write(String address, String uuid, dynamic value);
  Future<BLEResponse> writeWithResponse(
      String address, String uuid, dynamic value);
  Future<BLEResponse> read(String address, String uuid);
  void notify(String address, String uuid);
  void startBluetoothListener();
  void stopBluetoothListener();
}
