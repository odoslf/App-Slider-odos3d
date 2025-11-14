package com.odos3d.slider.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.common.util.concurrent.ListenableFuture
import com.odos3d.slider.ble.BluetoothConnector
import com.odos3d.slider.timelapse.TimelapseController
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun TimelapseScreen(bluetoothConnector: BluetoothConnector) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val controller = remember { TimelapseController(context, bluetoothConnector) }
  val isRunning by controller.running.collectAsState()
  val isConnected by bluetoothConnector.connected.collectAsState()
  val scope = rememberCoroutineScope()

  var intervalMsInput by rememberSaveable { mutableStateOf("2000") }
  var framesInput by rememberSaveable { mutableStateOf("10") }
  var distanceInput by rememberSaveable { mutableStateOf("5.0") }
  var speedInput by rememberSaveable { mutableStateOf("600") }
  var exportName by rememberSaveable { mutableStateOf("timelapse") }
  var isExporting by remember { mutableStateOf(false) }
  var lastVideo by remember { mutableStateOf<File?>(null) }

  val framesDir = remember { controller.framesDirectory() }
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
  val previewView = remember { PreviewView(context) }

  DisposableEffect(Unit) {
    onDispose {
      controller.stop()
      controller.release()
    }
  }

  LaunchedEffect(cameraProviderFuture, lifecycleOwner) {
    val cameraProvider = cameraProviderFuture.await(context)
    val preview = Preview.Builder().build().apply {
      surfaceProvider = previewView.surfaceProvider
    }
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
      lifecycleOwner,
      CameraSelector.DEFAULT_BACK_CAMERA,
      preview,
      controller.imageCapture
    )
  }

  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    AndroidView(
      factory = { previewView },
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
    )

    OutlinedTextField(
      value = intervalMsInput,
      onValueChange = { intervalMsInput = it.filter { ch -> ch.isDigit() } },
      label = { Text("Intervalo (ms)") },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    OutlinedTextField(
      value = framesInput,
      onValueChange = { framesInput = it.filter { ch -> ch.isDigit() } },
      label = { Text("Frames totales") },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    OutlinedTextField(
      value = distanceInput,
      onValueChange = { distanceInput = it.replace(',', '.') },
      label = { Text("Distancia por paso (mm)") },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )

    OutlinedTextField(
      value = speedInput,
      onValueChange = { speedInput = it.filter { ch -> ch.isDigit() } },
      label = { Text("Velocidad (mm/min)") },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    Button(
      onClick = {
        if (!isConnected) {
          Toast.makeText(context, "Conecta el slider primero", Toast.LENGTH_SHORT).show()
          return@Button
        }
        val interval = intervalMsInput.toLongOrNull()
        val frames = framesInput.toIntOrNull()
        val distance = distanceInput.toFloatOrNull()
        val speed = speedInput.toIntOrNull()
        if (interval == null || frames == null || distance == null || speed == null || frames <= 0) {
          Toast.makeText(context, "Revisa los parámetros", Toast.LENGTH_SHORT).show()
          return@Button
        }
        if (!isRunning) {
          controller.startTimelapse(frames, interval, distance, speed)
        } else {
          controller.stop()
        }
      },
      enabled = isConnected
    ) {
      Text(if (isRunning) "Parar" else "Iniciar")
    }

    Text(text = "Carpeta de fotos: ${framesDir.absolutePath}")

    OutlinedTextField(
      value = exportName,
      onValueChange = { exportName = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '-' } },
      label = { Text("Nombre del MP4") },
      modifier = Modifier.fillMaxWidth()
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = {
          if (isExporting) return@Button
          isExporting = true
          scope.launch {
            val sanitizedName = exportName.ifBlank { "timelapse" }
            val result = controller.exportToMp4(sanitizedName)
            result.onSuccess {
              lastVideo = it
              Toast.makeText(context, "MP4 creado: ${it.name}", Toast.LENGTH_SHORT).show()
            }.onFailure {
              Toast.makeText(context, "Error al crear MP4", Toast.LENGTH_SHORT).show()
            }
            isExporting = false
          }
        }
      ) {
        Text(if (isExporting) "Creando..." else "Crear MP4")
      }

      Button(
        onClick = {
          val video = lastVideo ?: run {
            Toast.makeText(context, "Genera primero el MP4", Toast.LENGTH_SHORT).show()
            return@Button
          }
          shareVideo(context, video)
        },
        enabled = lastVideo != null
      ) {
        Text("Compartir")
      }
    }
  }
}

private fun shareVideo(context: Context, file: File) {
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "video/mp4"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  context.startActivity(Intent.createChooser(intent, "Compartir timelapse"))
}

private suspend fun <T> ListenableFuture<T>.await(context: Context): T = suspendCancellableCoroutine { continuation ->
  addListener({
    try {
      continuation.resume(get())
    } catch (throwable: Throwable) {
      continuation.resumeWithException(throwable)
    }
  }, ContextCompat.getMainExecutor(context))
  continuation.invokeOnCancellation {
    cancel(true)
  }
}
