import 'dart:async';

import 'package:simple_flutter_ble/simple_flutter_ble.dart';
import 'package:simple_flutter_ble/src/flutter_ble_platform_interface.dart';

class BLEDeviceImp implements BLEDevice {
  final StreamController<bool> _statusController = StreamController.broadcast();
  @override
  Stream<bool> get status {
    FlutterBlePlatform.instance.status.listen(_updateStatus);
    return _statusController.stream;
  }

  final StreamController _notifyController = StreamController.broadcast();

  @override
  Stream notify(String uuid) {
    FlutterBlePlatform.instance.notify(address!, uuid);
    FlutterBlePlatform.instance.onNotify.listen(_updateNotification);
    return _notifyController.stream;
  }

  void _updateNotification(dynamic data) {
    _notifyController.sink.add(data);
  }

  void _updateStatus(Map<dynamic, dynamic> device) {
    if (device['address'].toString() == address) {
      online = device['status'];
      _statusController.sink.add(online!);
    }
  }

  BLEDeviceImp({
    this.online,
    this.serviceUuids,
    this.name,
    this.address,
    this.rssi,
    this.oldStatus,
    this.firstConnect,
  });

  factory BLEDeviceImp.fromJson(Map<dynamic, dynamic> json) {
    return BLEDeviceImp(
      name: json['name'],
      address: json['address'],
      serviceUuids: json['service_uuids'] == null ? [] : toList(json['service_uuids']),
      online: json['online'],
      rssi: json['rssi'],
      oldStatus: json['old_status'],
      firstConnect: json['first_connect'],
    );
  }

  static List<String> toList(List<dynamic> list) {
    List<String> services = [];
    for (var element in list) {
      services.add(element.toString());
    }
    return services;
  }

  static List<BLEDevice> fromMap(Map<dynamic, dynamic> list) {
    List<BLEDevice> devices = [];
    list.forEach((key, value) {
      devices.add(BLEDevice.fromJson(value));
    });
    return devices;
  }

  @override
  Future<bool> connect({int retryCount = 1}) async {
    return await FlutterBlePlatform.instance.connect(address!, retryCount);
  }

  @override
  Future<bool> disconnect() async {
    paired = false;
    return await FlutterBlePlatform.instance.disconnect(address!);
  }

  @override
  Future<bool> write(String uuid, dynamic value) async {
    return await FlutterBlePlatform.instance.write(address!, uuid, value);
  }

  @override
  Future<dynamic> read(String uuid) async {
    return await FlutterBlePlatform.instance.read(address!, uuid);
  }

  @override
  Future<dynamic> writeWithResponse(String uuid, dynamic value) async {
    return await FlutterBlePlatform.instance.writeWithResponse(address!, uuid, value);
  }

  @override
  String? address;

  @override
  bool? firstConnect;

  @override
  String? name;

  @override
  bool? oldStatus;

  @override
  bool? online;

  @override
  bool? paired;

  @override
  int? rssi;

  @override
  List<String>? serviceUuids;
}
