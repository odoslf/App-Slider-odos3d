package com.odos3d.slider.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.odos3d.slider.ble.BluetoothConnector
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(bluetoothConnector: BluetoothConnector) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var macAddress by rememberSaveable { mutableStateOf("") }
  var distance by rememberSaveable { mutableStateOf(10f) }
  var speed by rememberSaveable { mutableStateOf(600f) }
  val isConnected by bluetoothConnector.connected.collectAsState()

  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(text = if (isConnected) "Estado: Conectado" else "Estado: Desconectado")

    OutlinedTextField(
      value = macAddress,
      onValueChange = { macAddress = it.trim() },
      label = { Text("MAC Bluetooth") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = {
          if (macAddress.isNotBlank()) {
            coroutineScope.launch {
              val result = bluetoothConnector.connect(macAddress)
              result.onFailure {
                Toast.makeText(context, "Error al conectar: ${'$'}{it.message}", Toast.LENGTH_SHORT).show()
              }.onSuccess {
                Toast.makeText(context, "Conectado", Toast.LENGTH_SHORT).show()
              }
            }
          } else {
            Toast.makeText(context, "Introduce la MAC", Toast.LENGTH_SHORT).show()
          }
        },
        enabled = !isConnected
      ) {
        Text("Conectar")
      }

      Button(
        onClick = {
          bluetoothConnector.close()
          Toast.makeText(context, "Desconectado", Toast.LENGTH_SHORT).show()
        },
        enabled = isConnected
      ) {
        Text("Desconectar")
      }
    }

    Text("Distancia paso (mm): ${'$'}{distance.toInt()}")
    Slider(
      value = distance,
      onValueChange = { distance = it.coerceIn(1f, 200f) },
      valueRange = 1f..200f
    )

    Text("Velocidad (mm/min): ${'$'}{speed.toInt()}")
    Slider(
      value = speed,
      onValueChange = { speed = it.coerceIn(100f, 2000f) },
      valueRange = 100f..2000f
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = {
          if (isConnected) {
            sendMove(bluetoothConnector, -distance, speed)
          } else {
            Toast.makeText(context, "Conecta primero", Toast.LENGTH_SHORT).show()
          }
        },
        modifier = Modifier.weight(1f)
      ) {
        Text("Izquierda")
      }

      Button(
        onClick = {
          if (isConnected) {
            sendMove(bluetoothConnector, distance, speed)
          } else {
            Toast.makeText(context, "Conecta primero", Toast.LENGTH_SHORT).show()
          }
        },
        modifier = Modifier.weight(1f)
      ) {
        Text("Derecha")
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Los movimientos envían comandos G0 al slider.")
  }
}

private fun sendMove(connector: BluetoothConnector, delta: Float, speed: Float) {
  connector.sendLine("G91")
  connector.sendLine(
    "G0 X${'$'}{"%.2f".format(Locale.US, delta)} F${'$'}{speed.toInt()}"
  )
  connector.sendLine("G90")
}
