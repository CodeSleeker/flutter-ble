import 'dart:async';

import 'package:flutter/material.dart';
import 'package:simple_flutter_ble/simple_flutter_ble.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> implements DeviceListener {
  final flutterBle = FlutterBle();
  //or
  //final flutterBle = FlutterBle([<serviceUuids>]);

  List<BLEDevice> devices = [];

  @override
  void initState() {
    super.initState();
    startListening();
  }

  void startListening() {
    flutterBle.start();
    flutterBle.discoveredDevices.listen((List<BLEDevice> devices) {
      //device list changes (whether added or removed)
      this.devices = devices;
    });
    flutterBle.startBluetoothListener();
    //Listen to bluetooth connection changes
    flutterBle.onBluetoothConnection.listen((bool status) {
      //bluetooth connection changes
    });

    //Use this to use implements
    flutterBle.setListener(this);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('BLE Plugin example app'),
        ),
        body: Center(
          child: ListView.builder(
            itemCount: devices.length,
            itemBuilder: (BuildContext context, index) {
              BLEDevice device = devices[index];
              return Column(
                children: [
                  Row(
                    children: [
                      Text(device.name ?? ''),
                      Container(
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: (device.online ?? false)
                              ? Colors.green
                              : Colors.redAccent,
                        ),
                        width: 10,
                        height: 10,
                      ),
                      const SizedBox(
                        width: 20,
                      ),
                      TextButton(
                        onPressed: () {
                          device.connect();
                          //you can specify retry count
                          //device.connect(retryCount: 3);
                          //listen to device status
                          device.status.listen((status) {
                            //device status changes
                          });
                        },
                        child: const Text('Connect'),
                      ),
                      device.online!
                          ? TextButton(
                              onPressed: () {
                                device.write('uuid', '<dynamic data>');
                                //you can also wait for the response
                                var data = device.writeWithResponse(
                                    'uuid', '<dynamic data>');
                              },
                              child: const Text('Send data'),
                            )
                          : const SizedBox(),
                    ],
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
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
