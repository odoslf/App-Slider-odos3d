package com.odos3d.slider.work

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ExportWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uris = inputData.getStringArray(KEY_URIS)?.toList().orEmpty()
        val fps = inputData.getInt(KEY_FPS, 24).coerceIn(1, 60)
        val scaleWidth = inputData.getInt(KEY_SCALE_W, 0).coerceAtLeast(0)

        if (uris.isEmpty()) return@withContext Result.failure(Data.Builder().putString("error", "Sin imágenes").build())

        // 1) Copia segura a /cache/export/frame_0001.jpg ... para alimentar a FFmpeg
        val cacheDir = File(applicationContext.cacheDir, "export")
        if (cacheDir.exists()) cacheDir.deleteRecursively()
        cacheDir.mkdirs()

        var index = 1
        for (s in uris) {
            val uri = Uri.parse(s)
            val name = "frame_%04d.jpg".format(index)
            val outFile = File(cacheDir, name)
            applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { out -> input.copyTo(out) }
                index++
            }
        }
        if (index == 1) return@withContext Result.failure(Data.Builder().putString("error", "No se pudieron leer imágenes").build())

        // 2) Construcción del comando FFmpeg
        val pattern = File(cacheDir, "frame_%04d.jpg").absolutePath
        val tmpMp4 = File(cacheDir, "out.mp4").absolutePath

        val vf = if (scaleWidth > 0) {
            "scale=${scaleWidth}:-2:flags=lanczos"
        } else {
            "null"
        }
        val cmd = arrayOf(
            "-y",
            "-framerate", fps.toString(),
            "-i", pattern,
            "-vf", vf,
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-crf", "20",
            "-preset", "veryfast",
            tmpMp4
        ).joinToString(" ")

        val session = FFmpegKit.execute(cmd)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            return@withContext Result.failure(Data.Builder().putString("error", "FFmpeg fallo: ${session.failStackTrace}").build())
        }

        // 3) Publicar en MediaStore /Movies/Slider-odos3d
        val outName = "Slider_${STAMP()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, outName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Slider-odos3d")
        }
        val uri = applicationContext.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext Result.failure(Data.Builder().putString("error", "No se pudo crear salida").build())
        applicationContext.contentResolver.openOutputStream(uri)?.use { out ->
            File(tmpMp4).inputStream().use { it.copyTo(out) }
        }

        Result.success(Data.Builder().putString("videoUri", uri.toString()).build())
    }

    private fun STAMP(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

    companion object {
        const val KEY_URIS = "uris"
        const val KEY_FPS = "fps"
        const val KEY_SCALE_W = "scale_w"
    }
}
