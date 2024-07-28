package com.tech.flutter_ble.handlers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class PermissionHandler {
    fun verifyPermissions(context: Context, permissions: Array<String>): Boolean{
        var permissionCondition = true
        for(permission: String in permissions){
            permissionCondition = permissionCondition && ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        return permissionCondition
    }
    fun showDialog(activity: Activity, requestCode: Int, requestIntent: String){
        val enableIntent = Intent(requestIntent)
        activity.startActivityForResult(enableIntent, requestCode)
    }
}
