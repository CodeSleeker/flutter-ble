package com.tech.flutter_ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import com.tech.flutter_ble.handlers.PermissionHandler
import com.tech.flutter_ble.models.BLEDevice
import com.tech.flutter_ble.models.Device
import com.tech.flutter_ble.services.BLEScanReceiver
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/** FlutterBlePlugin */
class FlutterBlePlugin: FlutterPlugin, MethodCallHandler, PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var activity: Activity
    private var context: Context? = null
    private val permissionHandler: PermissionHandler = PermissionHandler()
    private lateinit var callbackIntent: PendingIntent
    private var started: Boolean = false
    private var firstConnect: Boolean = false

    private var connectedDevices: HashMap<String, BLEDevice>? = hashMapOf()

    private val locationPermissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    @SuppressLint("InlinedApi")
    private val bluetoothPermissions: Array<String> = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val bluetoothStateReceiver = object:BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent?) {
            if(BluetoothAdapter.ACTION_STATE_CHANGED == intent!!.action){
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when(state){
                    BluetoothAdapter.STATE_OFF->{
                        Handler(Looper.getMainLooper()).post{
                            channel.invokeMethod("bluetooth_status", false)
                        }
                        Log.d("BLUETOOTH", "OFF");
                    }
                    BluetoothAdapter.STATE_ON->{
                        Handler(Looper.getMainLooper()).post{
                            channel.invokeMethod("bluetooth_status", true)
                        }
                        Log.d("BLUETOOTH", "ON");
                    }
                }
            }
        }
    }

    companion object {
        private lateinit var bleClient: RxBleClient
        private var devices: HashMap<String, Device> = HashMap()
        private var channel: MethodChannel? = null
        private var serviceUuids: MutableList<String> = mutableListOf()

        fun getBleClient(context: Context): RxBleClient{
            if(!Companion::bleClient.isInitialized){
                bleClient = RxBleClient.create(context)
            }
            return bleClient
        }
        fun removeDevices(){
            devices.clear()
        }
        fun removeDevice(address: String){
            devices.remove(address)
        }
        fun setUuids(serviceUuids: MutableList<String>){
            this.serviceUuids = serviceUuids
        }
        fun setMethodChannel(channel: MethodChannel){
            this.channel = channel
        }
        fun onScanResult(result: ScanResult){
            val exist: Boolean = devices.entries.any{it.key == result.bleDevice.macAddress}
            val allowed: Boolean = if(serviceUuids.isEmpty()){
                true
            } else {
                if(result.scanRecord.serviceUuids == null) {
                    false
                }
                else {
                    result.scanRecord.serviceUuids.any{ serviceUuids.contains(it.uuid.toString())}
                }
            }
            if(!exist && allowed){
                devices[result.bleDevice.macAddress] = Device(result)
                val data: HashMap<String, Any> = hashMapOf()
                for (it in devices) {
                    val device = it.value.toJson()
                    data[it.value.address] = device
                }
                Handler(Looper.getMainLooper()).post{
                    channel?.invokeMethod("device_list", data)
                }
            }
        }
    }

    @SuppressLint("NewApi")
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when(call.method){
            "init" -> {
                setUuids(call.arguments as MutableList<String>)
//                val permissionMap = checkPermission()
//                if(permissionMap["location"] != true || permissionMap["bt"] != true){
//                    requestPermission()
//                }
                result.success(true)
            }
            "start_discovery" -> {
                started = false
                val permissionMap = checkPermission()
                if(permissionMap["location"] != true || permissionMap["bt"] != true){
                    requestPermission()
                } else {
                    started = true
                    startScan()
                }
            }
            "stop_discovery" -> {
                Log.d("SCAN", "Stopped")
                getBleClient(context!!).backgroundScanner.stopBackgroundBleScan(callbackIntent)
                removeDevices()
                result.success(true)
            }
            "connect_device" -> {
                Log.d("FLUTTER_BLE", "Request - Connect")
                val args = call.arguments as HashMap<*,*>
                val address: String = args["address"].toString()
                val retryCount = args["retry_count"] as Int
                if(connectedDevices?.any{it.key == address} == true){
//                    android.util.Log.e("STATUS", "Device ${address} already connected")
                    try{
                        disconnectDevice(address);
                    }
                    catch (_:Exception){

                    }
                }
//                else {
                    android.util.Log.d("STATUS", "Device ${address} connecting...")
                    val device: RxBleDevice = getBleClient(context!!).getBleDevice(address)
                    val bleDevice = BLEDevice()
                    bleDevice.disposable = CompositeDisposable()
                    connectedDevices?.set(device.macAddress, bleDevice)
                    checkConnectivity(device, retryCount)
//                    firstConnect = true
                    connectDevice(device, result, retryCount)
//                }
            }
            "disconnect_device" -> {
                Log.d("FLUTTER_BLE", "Request - Disconnect")
                android.util.Log.d("STATUS", "Device ${call.arguments as String} disconnecting")
                disconnectDevice(call.arguments as String, result)
            }
            "write_characteristic" -> {
                val args = call.arguments as HashMap<*,*>
                write(args, result)
            }
            "write_characteristic_with_response" -> {
                val args = call.arguments as HashMap<*, *>
                writeWithResponse(args, result)
            }
            "read_characteristic" -> {
                val args = call.arguments as HashMap<*, *>
                read(args, result)
            }
            "on_notify" -> {
                val args = call.arguments as HashMap<*, *>
                notify(args)
                result.success(true)
            }
            "start_bluetooth_listener" ->{
                val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                context!!.registerReceiver(bluetoothStateReceiver, filter)
                result.success(true)
            }
            "stop_bluetooth_listener" -> {
                context!!.unregisterReceiver(bluetoothStateReceiver)
                result.success(true)
            }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "com.tech.flutter_ble/ble")
        channel.setMethodCallHandler(this)
        context = binding.applicationContext
        setMethodChannel(channel)
        callbackIntent = BLEScanReceiver.newPendingIntent(context!!)
    }
    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        context = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
        binding.addRequestPermissionsResultListener(this)
    }
    override fun onDetachedFromActivity() {
        TODO("Not yet implemented")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        TODO("Not yet implemented")
    }
    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when(requestCode){
            100 -> {
                if(resultCode == Activity.RESULT_OK){
                    Log.d("LOCATION", "Enabled")
                    val btManager: BluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val btAdapter: BluetoothAdapter = btManager.adapter
                    val granted: Boolean = permissionHandler.verifyPermissions(context!!, bluetoothPermissions)
                    if(!btAdapter.isEnabled || !granted){
                        permissionHandler.showDialog(activity, 2, BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    }
                }
            }
            2 -> {
                if(resultCode == Activity.RESULT_OK){
                    Log.d("BLUETOOTH", "Permission granted")
                    if(!started) startScan()
                }
            }
        }
        return true
    }
    @SuppressLint("ServiceCast", "InlinedApi")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        when(requestCode){
            12 -> {
                android.util.Log.d("LOCATION", "Permission granted")
                val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                if(!isLocationEnabled){
                    val locationRequest: LocationRequest = LocationRequest.Builder(1000)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()
                    val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
                    builder.addLocationRequest(locationRequest)
                    builder.setAlwaysShow(true)
                    val client: SettingsClient = LocationServices.getSettingsClient(activity)
                    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

                    task.addOnSuccessListener(activity, OnSuccessListener<LocationSettingsResponse> {
                        val btManager: BluetoothManager = context?.getSystemService(Context.BATTERY_SERVICE) as BluetoothManager
                        val btAdapter: BluetoothAdapter = btManager.adapter

                        val granted: Boolean = permissionHandler.verifyPermissions(context!!, bluetoothPermissions)

                        if(!btAdapter.isEnabled || !granted){
                            permissionHandler.showDialog(activity, 2, BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        }
                    })
                    task.addOnFailureListener{exception->
                        if(exception is ResolvableApiException){
                            exception.startResolutionForResult(activity, 100)
                        }
                    }
                } else{
                    val btManager: BluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val btAdapter: BluetoothAdapter = btManager.adapter

                    val granted: Boolean = permissionHandler.verifyPermissions(context!!, bluetoothPermissions)

                    if(!btAdapter.isEnabled || !granted){
                        permissionHandler.showDialog(activity, 2, BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    }
                }
            }
        }
        return true
    }

    private fun checkPermission(): Map<String, Boolean> {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        val manager: BluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter

        val granted: Boolean = permissionHandler.verifyPermissions(context!!, locationPermissions)

        return mapOf("location" to (isLocationEnabled && granted), "bt" to adapter.isEnabled)
    }
    @SuppressLint("NewApi")
    private fun requestPermission(){
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        var granted = permissionHandler.verifyPermissions(context!!, locationPermissions)
        if(!isLocationEnabled || !granted){
            activity.requestPermissions(locationPermissions, 12)
            return
        }

        val btManager: BluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter: BluetoothAdapter = btManager.adapter

        granted = permissionHandler.verifyPermissions(context!!, bluetoothPermissions)

        if(!btAdapter.isEnabled || !granted){
            permissionHandler.showDialog(activity, 2, BluetoothAdapter.ACTION_REQUEST_ENABLE)
        }
    }

    @SuppressLint("CheckResult")
    private fun write(data: HashMap<*,*>, result: MethodChannel.Result){
        val uuid = data["uuid"].toString()
        val value = data["value"] as Any
        val address = data["address"]
        connectedDevices?.get(address)
            ?.write(uuid, value)
            ?.subscribe(
                {
                    result.success(true)
                },{
                    Log.e("WRITE", "ERROR")
                    result.success(false)
                }
            )
    }
    @SuppressLint("CheckResult")
    private fun writeWithResponse(data: HashMap<*,*>, result:MethodChannel.Result){
        val uuid = data["uuid"].toString()
        val value = data["value"] as Any
        val address = data["address"].toString()
        val res = hashMapOf<String, Any>()
        connectedDevices?.get(address)
            ?.write(uuid, value)
            ?.subscribe(
                {
                    connectedDevices?.get(address)
                        ?.read(uuid)
                        ?.subscribe(
                            {
                                res["success"] = true
                                res["data"] = it["data"] as Any
                                result.success(res)
                            },{
                                res["success"] = false
                                res["message"] = it.message as Any
                                result.success(res)
                            }
                        )
                },{
                    res["success"] = false
                    res["message"] = it.message as Any
                    result.success(res)
                }
            )
    }
    @SuppressLint("CheckResult")
    private fun read(data: HashMap<*, *>, result: MethodChannel.Result){
        val uuid = data["uuid"].toString()
        val address = data["address"].toString()
        val res = hashMapOf<String, Any>()
        connectedDevices?.get(address)
            ?.read(uuid)
            ?.subscribe(
                {
                    res["success"] = true
                    res["data"] = it["data"] as Any
                    result.success(res)
                },{
                    res["success"] = false
                    res["message"] = it.message as Any
                    result.success(res)
                }
            )
    }
    @SuppressLint("CheckResult")
    private fun notify(data: HashMap<*, *>){
        val uuid = data["uuid"].toString()
        val address = data["address"].toString()
        connectedDevices?.get(address)
            ?.onNotify(uuid)
            ?.subscribe(
                {
                    Handler(Looper.getMainLooper()).post{
                        Companion.channel?.invokeMethod("notify", it)
                    }
                },{
                    Log.d("NOTIFY", "ERROR")
                }
            )
    }

    @SuppressLint("NewApi")
    private fun startScan(){
        getBleClient(context!!).backgroundScanner.stopBackgroundBleScan(callbackIntent)
        devices.clear()
        Log.d("SCAN", "Started")

        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilter = ScanFilter.Builder()
            .build()
        getBleClient(context!!).backgroundScanner.scanBleDeviceInBackground(
            callbackIntent, scanSettings, scanFilter
        )
    }

    @SuppressLint("ServiceCast", "InlinedApi")
    private fun checkConnectivity(device: RxBleDevice, retryCount: Int = 1){
        val status: HashMap<String, Any> = hashMapOf()
        connectedDevices?.get(device.macAddress)?.connectionDisposable = device.observeConnectionStateChanges()
            .subscribe({
                state ->
                when(state){
                    RxBleConnection.RxBleConnectionState.CONNECTED -> {
                        status["status"] = true
                        status["address"] = device.macAddress
                        Log.d("STATUS", "Device ${device.macAddress} connected")
                        Handler(Looper.getMainLooper()).post{
                            channel.invokeMethod("device_status", status)
                        }
                    }
                    RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                        status["status"] = false
                        status["address"] = device.macAddress
                        android.util.Log.d("STATUS", "Device ${device.macAddress} disconnected")
                        Handler(Looper.getMainLooper()).post{
                            channel.invokeMethod(
                                "device_status",
                                status
                            )
                        }
                        val btManager: BluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val btAdapter: BluetoothAdapter = btManager.adapter

                        if(btAdapter.isEnabled){
                            connectDevice(device, retryCount=retryCount)
                        }

                    }
                    else -> {
                        status["status"] = false
                        status["address"] = ""
                    }
                }
            }, {
                status["status"] = false
                status["address"] = ""
            })
    }
    @SuppressLint("ServiceCast", "InlinedApi")
    private fun connectDevice(device: RxBleDevice, result: MethodChannel.Result? = null, retryCount: Int = 1){
        val count = retryCount - 1
        connectedDevices?.get(device.macAddress)?.clearDisposable()
        connectedDevices?.get(device.macAddress)?.disposable?.add(
            device.establishConnection(false)
//                .retryWhen { errors ->
//                    errors.flatMap {
//                        android.util.Log.e("STATUS", "Connection error for device ${device.macAddress}")
//                        android.util.Log.e("STATUS", "Retrying connection...")
//                        Observable.timer(5, TimeUnit.SECONDS)
//                    }
//                }
//                .timeout (10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    conn ->
                    connectedDevices?.get(device.macAddress)?.firstConnect = false;
                    result?.success(true)
                    connectedDevices?.get(device.macAddress)?.connection = conn

                },{
                    if(connectedDevices?.get(device.macAddress)?.firstConnect!!) {
                        if (count > 0) {
                            val btManager: BluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                            val btAdapter: BluetoothAdapter = btManager.adapter

                            if(btAdapter.isEnabled) {
                                connectDevice(device, retryCount = count - 1)
                            }
                        } else {
                            android.util.Log.d("DISCONNECTED------>", "Device ${device.macAddress} disconnected")
                            disconnectDevice(device.macAddress)
                            result?.success(false)
                        }
                    } else {
                        val btManager: BluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val btAdapter: BluetoothAdapter = btManager.adapter

                        if(btAdapter.isEnabled) {
                            connectDevice(device)
                        }
                    }
                })
        )
    }
    private fun disconnectDevice(address: String, result: MethodChannel.Result? = null){
        val device = connectedDevices?.get(address)
        device?.clearDisposable()
        device?.disposeConnection()
        connectedDevices?.remove(address)
        removeDevice(address)

        val status: HashMap<String, Any> = hashMapOf()
        status["status"] = false
        status["address"] = address
        android.util.Log.d("STATUS", "Device $address disconnected")
        Handler(Looper.getMainLooper()).post{
            channel.invokeMethod(
                "device_status",
                status
            )
        }
        result?.success(true)
    }
}
