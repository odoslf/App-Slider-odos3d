package com.odos3d.slider.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.odos3d.slider.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val buffer = ArrayDeque<String>()
    private const val MAX_LINES = 500

    @Synchronized
    fun i(tag: String, message: String) {
        append("I/$tag: $message")
    }

    @Synchronized
    fun e(tag: String, message: String) {
        append("E/$tag: $message")
    }

    @Synchronized
    fun w(tag: String, message: String) {
        append("W/$tag: $message")
    }

    @Synchronized
    fun log(message: String) {
        append(message)
    }

    @Synchronized
    fun dump(): String = getBufferAsString()

    @Synchronized
    fun clear() {
        buffer.clear()
    }

    @Synchronized
    fun getBufferAsString(): String = buffer.joinToString(separator = "\n")

    @Synchronized
    fun flushTo(out: File): File {
        out.parentFile?.mkdirs()
        out.writeText(getBufferAsString())
        return out
    }

    fun shareLogs(ctx: Context) {
        val dir = File(ctx.cacheDir, "logs").apply { mkdirs() }
        val out = File(dir, "logs-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.txt")
        flushTo(out)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", out)
        val intent = Intent(Intent.ACTION_SEND).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share)))
    }

    @Synchronized
    private fun append(line: String) {
        val stamped = "[${dateFormat.format(Date())}] ${line.take(2000)}"
        buffer.addLast(stamped)
        if (buffer.size > MAX_LINES) {
            buffer.removeFirst()
        }
    }
}
