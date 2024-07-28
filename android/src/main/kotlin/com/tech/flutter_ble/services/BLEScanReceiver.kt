package com.tech.flutter_ble.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.polidea.rxandroidble2.scan.BackgroundScanner
import com.tech.flutter_ble.FlutterBlePlugin

class BLEScanReceiver: BroadcastReceiver(){
    companion object{
        @SuppressLint("WrongConstant")
        fun newPendingIntent(context: Context): PendingIntent =
            Intent(context, BLEScanReceiver::class.java).let{
                PendingIntent.getBroadcast(context, 42, it, PendingIntent.FLAG_MUTABLE)
            }
    }
    @SuppressLint("NewApi", "UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val bleClient = FlutterBlePlugin.getBleClient(context)
        val backgroundScanner: BackgroundScanner = bleClient.backgroundScanner
        val scanResults = backgroundScanner.onScanResultReceived(intent)
        for (scanResult in scanResults){
            FlutterBlePlugin.onScanResult(scanResult)
        }
    }

}
