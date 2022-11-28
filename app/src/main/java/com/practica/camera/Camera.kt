package com.practica.camera

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.practica.camera.databinding.ActivityCameraBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


const val INTENTARIO_NAME = "intentarioName"

class Camera : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraControl:CameraControl
    private var imageCapture: ImageCapture? = null
    private var path=""
    private var isSave = false
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var data=""
    private var flashFlap = false
    private lateinit var bitmapToSave: Bitmap
    private val viewModel:CameraViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        data = intent.getStringExtra(INTENTARIO_NAME)!!
        checkPremisos()
        setListeners()
        viewModel.changeStatusVisibility(true)
    }

    private fun checkPremisos(){
        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (CameraUtil().allPermissionsGranted(this)) {
            startCamera()
            outputDirectory = getOutputDirectory()
            cameraExecutor = Executors.newSingleThreadExecutor()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            //set image captured
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(75)     //image quality
                .build()

            // Select back camera as a default
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                cameraControl=camera.cameraControl
                // set touch autofocus
                binding.viewFinder.afterMeasured {
                    binding.viewFinder.setOnTouchListener { _, event ->
                        return@setOnTouchListener when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                    binding.viewFinder.width.toFloat(), binding.viewFinder.height.toFloat()
                                )
                                val autoFocusPoint = factory.createPoint(event.x, event.y)
                                try {
                                    cameraControl.startFocusAndMetering(
                                        FocusMeteringAction.Builder(
                                            autoFocusPoint,
                                            FocusMeteringAction.FLAG_AF
                                        ).apply {
                                            //focus only when the user tap the preview
                                            disableAutoCancel()
                                        }.build()
                                    )
                                } catch (e: CameraInfoUnavailableException) {
                                    Log.d("ERROR", "cannot access camera", e)
                                }
                                true
                            }
                            else -> false // Unhandled event.
                        }
                    }
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun setListeners(){
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }
        binding.flipCamera.setOnClickListener{ flipCamera() }
        binding.backBtn.setOnClickListener{ takeAgainPhoto()}
        binding.saveBtn.setOnClickListener{ saveImage()}
        binding.flashBtn.setOnClickListener{
            flashFlap=!flashFlap
            cameraControl.enableTorch(flashFlap)
        }
        binding.rotarBtn.setOnClickListener{ rotateImage()}
    }

    private fun takePhoto() {
        // Get a stable reference of the
        // modifiable image capture use case
        cameraControl.enableTorch(false)
        val imageCapture = imageCapture ?: return
        binding.pbLoading.visibility=View.VISIBLE
        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        path = photoFile.absolutePath

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener,
        // which is triggered after photo has
        // been taken

        imageCapture.takePicture(
            outputOptions,//outputOptions
            ContextCompat.getMainExecutor(this),// ContextCompat.getMainExecutor(this)
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                //code after take the photo
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)
                    bitmapToSave= BitmapFactory.decodeFile(savedUri.path)
                    setVisibility(savedUri)
//                    viewModel.changeStatusVisibility(false)
                    viewModel.changeStatusVisibility(true)
                    // set the saved uri to the image view
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, msg)
                }
            })
    }

    private fun flipCamera(){
        if (CameraUtil().allPermissionsGranted(this)) {
            cameraSelector = if(cameraSelector==CameraSelector.DEFAULT_BACK_CAMERA){
                CameraSelector.DEFAULT_FRONT_CAMERA
            }else{
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun takeAgainPhoto(){
        if (!isSave){
            val image = File(path)
            if (image.exists()) image.delete()
        }
        setVisibility()
        viewModel.changeStatusVisibility(true)
    }

    private fun saveImage(){
        isSave = true
        val bos = ByteArrayOutputStream()
        val image = File(path)
        if (image.exists()){
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            val bArray = bos.toByteArray()
            image.writeBytes(bArray)
        }
//        if(path!=""){
//            val returnIntent = Intent()
//            returnIntent.putExtra("path", path)
//            setResult(RESULT_OK, returnIntent)
//            finish()
//        }else
        Toast.makeText(this, path, Toast.LENGTH_SHORT).show()
        takeAgainPhoto()
    }

    private  fun setVisibility(savedUri: Uri? = null){
        if(binding.ivCapture.visibility == View.GONE){
            binding.ivCapture.visibility = View.VISIBLE
            binding.ivCapture.setImageURI(savedUri)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.pbLoading.visibility= View.GONE
                binding.viewFinder.visibility = View.GONE
                binding.cameraCaptureButton.visibility= View.GONE
                binding.flipCamera.visibility= View.GONE
                binding.backBtn.visibility = View.VISIBLE
                binding.saveBtn.visibility= View.VISIBLE
//                binding.flashBtn.visibility=View.GONE
                binding.logo.visibility=View.GONE
                binding.bgLogo.visibility=View.GONE
                binding.rotarBtn.visibility= View.VISIBLE
            }, 300)
        }else{
            binding.ivCapture.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed({
                binding.viewFinder.visibility = View.VISIBLE
                binding.cameraCaptureButton.visibility= View.VISIBLE
                binding.flipCamera.visibility= View.VISIBLE
                binding.backBtn.visibility = View.GONE
                binding.saveBtn.visibility= View.GONE
//                binding.flashBtn.visibility=View.VISIBLE
                binding.logo.visibility=View.VISIBLE
                binding.bgLogo.visibility=View.VISIBLE
                binding.rotarBtn.visibility= View.GONE
            }, 300)
        }
    }

    private fun rotateImage(){
        val image = File(path)
        if (image.exists()){
            val rotatedBitmap = CameraUtil().rotateImage(bitmapToSave)
            bitmapToSave = rotatedBitmap
            binding.ivCapture.setImageBitmap(bitmapToSave)
        }
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        if (measuredWidth > 0 && measuredHeight > 0) {
            block()
        } else {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0 && measuredHeight > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        block()
                    }
                }
            })
        }
    }

    override fun onBackPressed() {
        if(binding.ivCapture.visibility==View.VISIBLE){
            takeAgainPhoto()
        }else
            super.onBackPressed()
    }

    companion object {
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }


}