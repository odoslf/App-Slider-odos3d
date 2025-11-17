package com.odos3d.slider.camera

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

class TimelapseController(
    private val fragment: Fragment,
    private val previewView: PreviewView,
    private val onCaptureSaved: (String) -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var imageCapture: ImageCapture? = null
    private var runningJob: Job? = null
    private var isBound = false
    private var isRunning = false
    private val mainExecutor: Executor by lazy { ContextCompat.getMainExecutor(fragment.requireContext()) }

    var onBeforeCapture: ((Int, Int?) -> Unit)? = null
    var onAfterCapture: ((Int, Int?) -> Unit)? = null

    suspend fun bindIfNeeded() {
        if (isBound) return
        val context = fragment.requireContext()
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(fragment.viewLifecycleOwner, selector, preview, imageCapture)
        isBound = true
    }

    fun start(intervalSec: Int, totalShots: Int? = null, presetTitle: String? = null) {
        if (intervalSec <= 0) return
        if (isRunning) return
        isRunning = true
        val safeTitle = presetTitle?.takeIf { it.isNotBlank() }?.replace("\\s+".toRegex(), "_") ?: "Timelapse"
        runningJob = scope.launch(Dispatchers.Main) {
            delay(200)
            var shotIndex = 0
            val limit = totalShots
            while (isActive && isRunning) {
                onBeforeCapture?.invoke(shotIndex, limit)
                takePhoto(fragment.requireContext(), safeTitle)
                onAfterCapture?.invoke(shotIndex, limit)
                shotIndex++
                if (limit != null && shotIndex >= limit) {
                    isRunning = false
                    break
                }
                val sleep = (intervalSec.coerceAtLeast(1) * 1000L)
                var remain = sleep
                while (remain > 0 && isActive && isRunning) {
                    delay(minOf(remain, 200L))
                    remain -= 200L
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        runningJob?.cancel()
        runningJob = null
    }

    fun shutdown() {
        stop()
    }

    private fun takePhoto(context: Context, titlePrefix: String) {
        val capture = imageCapture ?: return
        val name = "${titlePrefix}_${STAMP()}.jpg"
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Slider-odos3d")
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
            .build()

        capture.takePicture(outputOptions, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onCaptureSaved(name)
            }
            override fun onError(exception: ImageCaptureException) {
                // error puntual, se seguirá intentando en el siguiente tick
            }
        })
    }

    private fun STAMP(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
    }
}
