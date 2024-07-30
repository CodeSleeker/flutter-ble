# simple_flutter_ble

Flutter plugin for Bluetooth Low Energy

## Features

* Can also discover devices based on the given list of service uuids
* Listen to bluetooth connection changes
* Can send data and wait for the response.
* Listen to device connection changes
* Subscribe to notification

## Getting Started
```
import 'package:flutter_ble/flutter_ble.dart';
```
### Instantiate
```
FlutterBle ble = FlutterBle();
```
#### or filter devices with service uuids
```
FlutterBle ble = FlutterBle(['ec75916b-2217-401d-a433-80411522b493']);
```
### Start discovery
```
ble.start();
//subscription method
//Listen to discovered devices
//this will trigger whether devices are added or removed
ble.discoveredDevices.listen((devices){

});
//Listen to bluetooth connection status
ble.startBluetoothListener();
ble.onBluetooth.listen((status){
 
});
```
### Write, read and notify
```
//Write
device.write('uuid', 'dynamic data');
//Wait for response
var data = device.writeWithResponse('uuid', 'dynamic data');
//read
var data = device.read('uuid');
//listen to notification
device.notify('uuid').listen((data){

});
```
### Using implements
```
class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> implements DeviceListener{
 FlutterBle ble = FlutterBle();
 @override
 void initState() {
  super.initState();
  flutterBle.start();
  flutterBle.startBluetoothListener();
  flutterBle.setListener(this);
 }

 @override
  void onBluetoothConnected(bool status) {
    // TODO: implement onBluetoothConnected
  }

  @override
  void onBluetoothDisconnected(bool status) {
    // TODO: implement onBluetoothDisconnected
  }

  @override
  void onDeviceConnected(String address, bool status) {
    // TODO: implement onDeviceConnected
  }

  @override
  void onDeviceDisconnected(String address, bool status) {
    // TODO: implement onDeviceDisconnected
  }

  @override
  void onDiscoveredDevices(List<BLEDevice> devices) {
    // TODO: implement onDiscoveredDevices
  }

  @override
  void onNotify(data) {
    // TODO: implement onNotify
  }
}
```
