package com.tech.flutter_ble.models

import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.UUID

class BLEDevice (
    var disposable: CompositeDisposable? = null,
    var connection: RxBleConnection? = null,
    var connectionDisposable: Disposable? = null,
    var firstConnect: Boolean = true
){
    fun disposeConnection(){
        connectionDisposable?.dispose()
    }
    fun clearDisposable(){
        disposable?.clear()
    }
    fun onNotify(uuid: String): Observable<Any> {
        return connection?.setupNotification(UUID.fromString(uuid))
            ?.flatMap { observable ->
                observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }?:Observable.error(IllegalStateException("Connection is null"))
    }
    fun read(uuid: String): Single<MutableMap<String, Any>> {
        return connection?.readCharacteristic(UUID.fromString(uuid))
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.map{
                val resultMap = mutableMapOf<String, Any>("data" to it)
                resultMap
            }?:Single.error(IllegalStateException("Connection is null"))
    }
    fun write(uuid: String, value: Any): Single<Boolean>{
        var byteArray: ByteArray = byteArrayOf()
        when(value){
            is String -> {byteArray = value.toByteArray()}
            is Int -> { byteArray = ByteBuffer.allocate(4).putInt(value).array()}
            is Float -> { byteArray = ByteBuffer.allocate(4).putFloat(value).array()}
            is Boolean -> { byteArray = ByteArray(1){if(value) 1 else 0}
            }
        }
        return connection?.writeCharacteristic(UUID.fromString(uuid), byteArray)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.map { true }
            ?.onErrorReturn { false }
            ?: Single.error(IllegalStateException("Connection is null"))
    }
}
