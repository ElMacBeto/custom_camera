package com.practica.camera

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.content.ContextCompat
import java.io.File

class CameraUtil {

     fun allPermissionsGranted(context: Context) = Camera.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun rotateImage(bitmapToSave:Bitmap):Bitmap{
        val matrix = Matrix()
        matrix.postRotate(90f)

        return Bitmap.createBitmap(
            bitmapToSave, 0, 0, bitmapToSave.width, bitmapToSave.height, matrix, true
        )
    }

}