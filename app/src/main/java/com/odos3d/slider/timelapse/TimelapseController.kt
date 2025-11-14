package com.odos3d.slider.timelapse

import android.content.Context
import android.os.Environment
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.odos3d.slider.ble.BluetoothConnector
import com.odos3d.slider.core.Constants
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TimelapseController(
  private val context: Context,
  private val bluetoothConnector: BluetoothConnector
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val _running = MutableStateFlow(false)
  val running = _running.asStateFlow()
  val imageCapture: ImageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .build()

  fun framesDirectory(): File = getOutputDirectory()

  fun startTimelapse(frames: Int, intervalMs: Long, distanceMm: Float, speed: Int) {
    if (_running.value) return
    _running.value = true
    scope.launch {
      runTimelapse(frames, intervalMs, distanceMm, speed)
    }
  }

  fun stop() {
    _running.value = false
  }

  fun release() {
    stop()
    cameraExecutor.shutdown()
  }

  suspend fun exportToMp4(outputName: String): Result<File> = withContext(Dispatchers.IO) {
    val inputDir = getOutputDirectory()
    val outputFile = File(inputDir, "$outputName.mp4")
    val cmd = "-framerate 30 -pattern_type glob -i '${inputDir.absolutePath}/*.jpg' -c:v libx264 -pix_fmt yuv420p '${outputFile.absolutePath}'"
    val session = FFmpegKit.execute(cmd)
    if (ReturnCode.isSuccess(session.returnCode)) {
      Result.success(outputFile)
    } else {
      Result.failure(IllegalStateException(session.failStackTrace ?: "Error exportando video"))
    }
  }

  private suspend fun runTimelapse(frames: Int, intervalMs: Long, distanceMm: Float, speed: Int) {
    val outputDir = getOutputDirectory()
    for (index in 0 until frames) {
      if (!_running.value) break
      sendStep(distanceMm, speed)
      capturePhoto(index, outputDir)
      if (index < frames - 1) {
        delay(intervalMs)
      }
    }
    _running.value = false
  }

  private fun sendStep(distanceMm: Float, speed: Int) {
    val formattedDistance = String.format(Locale.US, "%.2f", distanceMm)
    bluetoothConnector.sendLine("G91")
    bluetoothConnector.sendLine("G0 X$formattedDistance F$speed")
    bluetoothConnector.sendLine("G90")
  }

  private suspend fun capturePhoto(index: Int, outputDir: File) {
    val fileName = "frame_${index.toString().padStart(5, '0')}.jpg"
    val photoFile = File(outputDir, fileName)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    suspendCancellableCoroutine<Unit> { continuation ->
      imageCapture.takePicture(
        outputOptions,
        cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
          override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            if (!continuation.isCompleted) {
              continuation.resume(Unit)
            }
          }

          override fun onError(exception: ImageCaptureException) {
            if (!continuation.isCompleted) {
              continuation.resumeWithException(exception)
            }
          }
        }
      )
    }
  }

  private fun getOutputDirectory(): File {
    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val dir = File(pictures, Constants.APP_FOLDER)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }
}
