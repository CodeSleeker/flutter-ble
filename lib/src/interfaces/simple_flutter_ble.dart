part of '../../simple_flutter_ble.dart';

abstract class FlutterBle {
  factory FlutterBle({List<String> services}) = FlutterBleImp;

  ///Start bluetooth connection listener
  void startBluetoothListener();

  ///Stop bluetooth connection listener
  void stopBluetoothListener();

  ///Set listener
  ///Use this when you are using implements
  void setListener(DeviceListener listener);

  ///Listen to bluetooth connection changes
  Stream<bool> get onBluetoothConnection;

  ///Listen to changes of the discovered devices
  Stream<List<BLEDevice>> get discoveredDevices;

  ///Start discovering
  void start();

  ///Stop discovering
  void stop();
}
