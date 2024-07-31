import 'dart:async';

import 'package:simple_flutter_ble/src/flutter_ble_platform_interface.dart';
import 'package:simple_flutter_ble/src/models/ble_device_imp.dart';

import '../simple_flutter_ble.dart';

class FlutterBleImp implements FlutterBle {
  final StreamController<List<BLEDevice>> _ddController =
      StreamController.broadcast();
  final StreamController _streamController = StreamController.broadcast();
  late StreamController<bool> _bluetoothController;
  DeviceListener? deviceListener;

  @override
  Stream<List<BLEDevice>> get discoveredDevices => _ddController.stream;
  Stream get status => _streamController.stream;
  StreamSubscription? btSubscription;
  @override
  Stream<bool> get onBluetoothConnection {
    btSubscription = FlutterBlePlatform.instance.bluetoothListener
        .listen(_updateBluetoothStatus);
    return _bluetoothController.stream;
  }

  void _updateBluetoothStatus(bool data) {
    _bluetoothController.sink.add(data);
    if (data) {
      deviceListener?.onBluetoothConnected(data);
    } else {
      deviceListener?.onBluetoothDisconnected(data);
    }
  }

  @override
  void setListener(DeviceListener listener) {
    deviceListener = listener;
  }

  factory FlutterBleImp({List<String> services = const []}) {
    return FlutterBleImp._internal(services: services);
  }

  FlutterBleImp._internal({List<String> services = const []}) {
    FlutterBlePlatform.instance.initialize(services: services);
    FlutterBlePlatform.instance.discoveredDevices.listen((event) {
      List<BLEDevice> devices = BLEDeviceImp.fromMap(event);
      _ddController.sink.add(devices);
      deviceListener?.onDiscoveredDevices(devices);
    });
    FlutterBlePlatform.instance.status.listen((Map<dynamic, dynamic> device) {
      deviceListener?.onDeviceConnected(
          device['address'].toString(), device['status'] as bool);
    });
    FlutterBlePlatform.instance.onNotify.listen((data) {
      deviceListener?.onNotify(data);
    });
  }

  @override
  void start() {
    FlutterBlePlatform.instance.start();
  }

  @override
  void stop() {
    FlutterBlePlatform.instance.stop();
  }

  @override
  void startBluetoothListener() {
    FlutterBlePlatform.instance.bluetoothController =
        StreamController.broadcast();
    _bluetoothController = StreamController.broadcast();
    FlutterBlePlatform.instance.startBluetoothListener();
  }

  @override
  void stopBluetoothListener() {
    FlutterBlePlatform.instance.stopBluetoothListener();
    btSubscription?.cancel();
  }
}
