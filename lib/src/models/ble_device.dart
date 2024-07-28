part of '../../simple_flutter_ble.dart';

class BLEDevice {
  String? name;
  String? address;
  List<String>? serviceUuids;
  bool? online;
  bool? paired;
  int? rssi;
  bool? oldStatus;
  String? server;
  bool? firstConnect;

  final StreamController<bool> _statusController = StreamController.broadcast();
  Stream<bool> get status {
    // FlutterBlePlatform.instance.connect(address!);
    FlutterBlePlatform.instance.status.listen(_updateStatus);
    return _statusController.stream;
  }

  final StreamController _notifyController = StreamController.broadcast();
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

  BLEDevice({
    this.online,
    this.serviceUuids,
    this.name,
    this.address,
    this.rssi,
    this.oldStatus,
    this.server,
    this.firstConnect,
  });

  @override
  BLEDevice.fromJson(Map<dynamic, dynamic> json) {
    name = json['name'];
    address = json['address'];
    serviceUuids = json['service_uuids'] == null ? [] : toList(json['service_uuids']);
    online = json['online'];
    rssi = json['rssi'];
    oldStatus = json['old_status'];
    server = json['server'];
    firstConnect = json['first_connect'];
  }

  static List<String> toList(List<dynamic> list) {
    List<String> services = [];
    for (var element in list) {
      services.add(element.toString());
    }
    return services;
  }

  static List<BLEDevice> fromList(Map<dynamic, dynamic> list) {
    List<BLEDevice> devices = [];
    list.forEach((key, value) {
      devices.add(BLEDevice.fromJson(value));
    });
    return devices;
  }

  Future<bool> connect({int retryCount = 1}) async {
    return await FlutterBlePlatform.instance.connect(address!, retryCount);
  }

  Future<bool> disconnect() async {
    paired = false;
    return await FlutterBlePlatform.instance.disconnect(address!);
  }

  Future<bool> write(String uuid, dynamic value) async {
    return await FlutterBlePlatform.instance.write(address!, uuid, value);
  }

  Future<dynamic> read(String uuid) async {
    return await FlutterBlePlatform.instance.read(address!, uuid);
  }

  Future<dynamic> writeWithResponse(String uuid, dynamic value) async {
    return await FlutterBlePlatform.instance.writeWithResponse(address!, uuid, value);
  }
}
