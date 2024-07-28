part of '../flutter_ble.dart';

abstract class DeviceListener {
  void onDiscoveredDevices(List<BLEDevice> devices) {}
  void onBluetoothConnected(bool status) {}
  void onBluetoothDisconnected(bool status) {}
  void onNotify(dynamic data) {}
  void onDeviceConnected(String address, bool status) {}
  void onDeviceDisconnected(String address, bool status) {}
}
