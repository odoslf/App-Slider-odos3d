package com.odos3d.slider.link

import android.Manifest
import android.os.Build
import androidx.activity.result.ActivityResultLauncher

object LinkPermissions {
    fun requestBtScanIfNeeded(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= 31) {
            launcher.launch(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun requestBtConnectIfNeeded(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= 31) {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun requestCamera(launcher: ActivityResultLauncher<String>) {
        launcher.launch(Manifest.permission.CAMERA)
    }
}
