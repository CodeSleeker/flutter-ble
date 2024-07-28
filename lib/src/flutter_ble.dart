part of '../flutter_ble.dart';

class FlutterBle {
  final StreamController<List<BLEDevice>> _ddController = StreamController.broadcast();
  final StreamController _streamController = StreamController.broadcast();
  late StreamController<bool> _bluetoothController;
  DeviceListener? deviceListener;

  Stream<List<BLEDevice>> get discoveredDevices => _ddController.stream;
  Stream get status => _streamController.stream;
  StreamSubscription? btSubscription;
  Stream<bool> get onBluetooth {
    btSubscription = FlutterBlePlatform.instance.bluetoothListener.listen(_updateBluetoothStatus);
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

  void setListener(DeviceListener listener) {
    deviceListener = listener;
  }

  FlutterBle({List<String> services = const []}) {
    FlutterBlePlatform.instance.initialize(services: services);
    FlutterBlePlatform.instance.discoveredDevices.listen((event) {
      List<BLEDevice> devices = BLEDevice.fromList(event);
      _ddController.sink.add(devices);
      deviceListener?.onDiscoveredDevices(devices);
    });
    FlutterBlePlatform.instance.status.listen((Map<dynamic, dynamic> device) {
      deviceListener?.onDeviceConnected(device['address'].toString(), device['status'] as bool);
    });
    FlutterBlePlatform.instance.onNotify.listen((data) {
      deviceListener?.onNotify(data);
    });
  }

  void start() {
    FlutterBlePlatform.instance.start();
  }

  void stop() {
    FlutterBlePlatform.instance.stop();
  }

  void startBluetoothListener() {
    FlutterBlePlatform.instance.bluetoothController = StreamController.broadcast();
    _bluetoothController = StreamController.broadcast();
    FlutterBlePlatform.instance.startBluetoothListener();
  }

  void stopBluetoothListener() {
    FlutterBlePlatform.instance.stopBluetoothListener();
    btSubscription?.cancel();
  }
}
