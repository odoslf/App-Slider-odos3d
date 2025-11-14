package com.odos3d.slider.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odos3d.slider.ble.BluetoothConnector
import kotlinx.coroutines.launch

@Composable
fun BluetoothScreen(bluetoothConnector: BluetoothConnector) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val adapter = remember { BluetoothAdapter.getDefaultAdapter() }
  val bondedDevices = remember { adapter?.bondedDevices?.toList().orEmpty() }
  val isConnected by bluetoothConnector.connected.collectAsState()
  var macInput by rememberSaveable { mutableStateOf("") }

  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(text = "Estado Bluetooth: ${if (adapter?.isEnabled == true) "Activo" else "Desactivado"}")
    Text(text = "Conexión slider: ${if (isConnected) "Conectado" else "Desconectado"}")

    OutlinedTextField(
      value = macInput,
      onValueChange = { macInput = it.trim() },
      label = { Text("MAC manual") },
      modifier = Modifier.fillMaxWidth()
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(onClick = {
        if (macInput.isBlank()) {
          Toast.makeText(context, "Introduce una MAC", Toast.LENGTH_SHORT).show()
          return@Button
        }
        coroutineScope.launch {
          val result = bluetoothConnector.connect(macInput)
          result.onFailure {
            Toast.makeText(context, "Fallo conexión", Toast.LENGTH_SHORT).show()
          }.onSuccess {
            Toast.makeText(context, "Conectado", Toast.LENGTH_SHORT).show()
          }
        }
      }) {
        Text("Conectar")
      }

      Button(onClick = {
        bluetoothConnector.close()
        Toast.makeText(context, "Conexión cerrada", Toast.LENGTH_SHORT).show()
      }) {
        Text("Desconectar")
      }
    }

    Button(onClick = {
      context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }) {
      Text("Abrir ajustes del sistema")
    }

    Text(text = "Dispositivos emparejados", fontWeight = FontWeight.Bold)
    LazyColumn {
      items(bondedDevices) { device ->
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
              macInput = device.address
            }
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(text = device.name ?: "Sin nombre", fontWeight = FontWeight.SemiBold)
            Text(text = device.address)
          }
        }
      }
    }
  }
}
