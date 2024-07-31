part of '../../simple_flutter_ble.dart';

abstract class BLEDevice {
  ///Device name
  String? name;

  ///Device mac address
  String? address;

  ///Service uuids available
  List<String>? serviceUuids;

  ///Device status
  bool? online;

  ///Device pairing status
  bool? paired;

  ///Device signal strength
  int? rssi;

  ///Device previous status
  bool? oldStatus;

  ///First connection status
  bool? firstConnect;

  ///Convert json to BLEDevice
  factory BLEDevice.fromJson(Map<dynamic, dynamic> data) {
    return BLEDeviceImp.fromJson(data);
  }

  ///Listen to device status
  Stream<bool> get status;

  ///Listen to notification
  Stream notify(String uuid);

  ///Connect to device
  Future<bool> connect({int retryCount = 1});

  ///Disconnect from device
  Future<bool> disconnect();

  ///Send data
  Future<bool> write(String uuid, dynamic value);

  ///Read data
  Future<dynamic> read(String uuid);

  ///Send data and wait for response
  Future<dynamic> writeWithResponse(String uuid, dynamic value);
}
