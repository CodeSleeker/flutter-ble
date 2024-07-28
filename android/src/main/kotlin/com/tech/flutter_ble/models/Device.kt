package com.tech.flutter_ble.models

import android.os.ParcelUuid
import com.polidea.rxandroidble2.scan.ScanResult

class Device (
    var name: String = "",
    var address: String = "",
    var serviceUuids: List<String> = listOf(),
    var rssi: Int = 0,
){
    companion object{
        private fun fromList(list: List<ParcelUuid>): List<String>{
            return list.map{it.uuid.toString()}
        }
    }
    constructor(result: ScanResult): this(
        name = result.bleDevice.name ?: "",
        address = result.bleDevice.macAddress,
        serviceUuids = fromList(result.scanRecord.serviceUuids ?: listOf()),
        rssi = result.rssi,
    )
    fun toJson(): HashMap<String, Any>{
        val deviceHash = hashMapOf<String, Any>()
        deviceHash["name"] = name
        deviceHash["address"] = address
        deviceHash["service_uuids"] = serviceUuids
        deviceHash["rssi"] = rssi
        return deviceHash
    }
}
