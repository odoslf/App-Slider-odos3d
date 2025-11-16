package com.odos3d.slider.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore

object MediaStoreHelper {
    fun insertJpeg(context: Context, displayName: String, bytes: ByteArray): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Slider-odos3d")
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
                true
            } ?: false
        }.getOrElse { false }
    }
}
