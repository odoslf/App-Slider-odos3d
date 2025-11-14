package com.odos3d.slider.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.odos3d.slider.core.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class BluetoothConnector(private val ctx: Context) {
  private var socket: BluetoothSocket? = null
  private val _connected = MutableStateFlow(false)
  val connected = _connected.asStateFlow()

  suspend fun connect(mac: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val adapter = BluetoothAdapter.getDefaultAdapter()
      val device = adapter.getRemoteDevice(mac)
      val uuid = java.util.UUID.fromString(Constants.SPP_UUID)
      socket?.close()
      socket = device.createRfcommSocketToServiceRecord(uuid)
      adapter.cancelDiscovery()
      socket!!.connect()
      _connected.value = true
    }
  }

  fun sendLine(line: String) {
    socket?.outputStream?.apply {
      write((line.trim() + "\n").toByteArray())
      flush()
    }
  }

  fun close() {
    try {
      socket?.close()
    } catch (_: Exception) {
    }
    _connected.value = false
  }
}
