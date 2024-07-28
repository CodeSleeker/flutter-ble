part of '../../simple_flutter_ble.dart';

class BLEResponse {
  bool? success;
  dynamic data;
  String? message;
  BLEResponse({
    this.data,
    this.message,
    this.success,
  });
  BLEResponse.fromJson(Map<dynamic, dynamic> json) {
    success = json["success"];
    data = json["data"];
    message = json["message"];
  }
}
