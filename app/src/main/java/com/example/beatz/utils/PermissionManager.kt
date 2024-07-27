package com.example.beatz.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object PermissionManager {
    lateinit var permissionLauncher: ActivityResultLauncher<String>

    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        permissionLauncher.launch(permission)
    }

    interface PermissionCallback {
        val requestCode: Int
        fun onPermissionGranted()
        fun onPermissionDenied()
    }
}