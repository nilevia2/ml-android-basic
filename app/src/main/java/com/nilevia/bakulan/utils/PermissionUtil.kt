package com.nilevia.bakulan.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

const val PERMISSION_CAMERA = Manifest.permission.CAMERA
const val PERMISSION_AUDIO = Manifest.permission.RECORD_AUDIO

/**
 * get permission one by one and trigger callback when it granted
 */
fun AppCompatActivity.getPermission(permission: String, permissionResult: (isGranted: Boolean) -> Unit) {
    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
        permissionResult.invoke(true)
    } else {
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                permissionResult.invoke(true)
            } else {
                permissionResult.invoke(false)
            }
        }.launch(permission)
    }
}

/**
 * get multiple permissions and trigger one callback when it granted
 */

fun AppCompatActivity.getPermissions(permissions: Array<String>, permissionResult: (isGranted: Boolean) -> Unit) {
    val deniedPermissions = permissions.filter {
        ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    if (deniedPermissions.isEmpty()) {
        permissionResult.invoke(true)
    } else {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val allGranted = results.all { it.value }
            if (allGranted) {
                permissionResult.invoke(true)
            } else {
                permissionResult.invoke(false)
            }
        }.launch(deniedPermissions.toTypedArray())
    }
}