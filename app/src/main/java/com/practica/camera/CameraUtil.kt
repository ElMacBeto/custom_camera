package com.practica.camera

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class CameraUtil {

     fun allPermissionsGranted(context: Context) = Camera.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }






}