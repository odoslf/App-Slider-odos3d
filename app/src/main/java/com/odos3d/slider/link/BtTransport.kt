package com.odos3d.slider.link

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class BtTransport(private val ctx: Context) : LinkTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var readJob: Job? = null
    private val connected = AtomicBoolean(false)
    private var onLineCb: ((String) -> Unit)? = null

    override fun setReader(onLine: (String) -> Unit) {
        onLineCb = onLine
    }

    override fun connect(address: String, onConnected: (Boolean, String?) -> Unit) {
        close()
        readJob = scope.launch {
            try {
                val mgr = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = mgr.adapter ?: BluetoothAdapter.getDefaultAdapter()
                val dev = adapter?.getRemoteDevice(address)
                    ?: return@launch onConnected(false, "Dispositivo inválido")
                adapter.cancelDiscovery()
                val s = dev.createRfcommSocketToServiceRecord(sppUuid)
                s.connect()
                socket = s
                connected.set(true)
                onConnected(true, null)
                val reader = BufferedReader(InputStreamReader(s.inputStream))
                while (isActive) {
                    val line = try {
                        reader.readLine()
                    } catch (_: Exception) {
                        null
                    } ?: break
                    onLineCb?.invoke(line)
                }
            } catch (e: SecurityException) {
                onConnected(false, "Permiso de Conexión requerido")
            } catch (e: Exception) {
                onConnected(false, e.message ?: "Error de conexión")
            } finally {
                connected.set(false)
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
                socket = null
            }
        }
    }

    override fun isConnected(): Boolean = connected.get()

    override fun write(bytes: ByteArray): Boolean {
        return try {
            socket?.outputStream?.write(bytes)
            socket?.outputStream?.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun close() {
        readJob?.cancel()
        readJob = null
        connected.set(false)
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }
}
