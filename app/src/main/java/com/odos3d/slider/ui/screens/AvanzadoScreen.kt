package com.odos3d.slider.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.odos3d.slider.ble.BluetoothConnector
import kotlinx.coroutines.launch

@Composable
fun AvanzadoScreen(bluetoothConnector: BluetoothConnector) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val logs = remember { mutableStateListOf<String>() }
  var command by rememberSaveable { mutableStateOf("") }
  val isConnected by bluetoothConnector.connected.collectAsState()
  val status = if (isConnected) "Conectado" else "Desconectado"

  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(text = "Modo Avanzado - $status")

    OutlinedTextField(
      value = command,
      onValueChange = { command = it },
      label = { Text("Comando G-code") },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Characters)
    )

    Button(
      onClick = {
        if (command.isNotBlank() && isConnected) {
          scope.launch {
            bluetoothConnector.sendLine(command)
            logs.add(0, "> $command")
            command = ""
          }
        } else {
          val message = if (isConnected) "Introduce un comando" else "Conecta el slider primero"
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
      },
      enabled = isConnected
    ) {
      Text("Enviar")
    }

    LazyColumn(reverseLayout = true) {
      items(logs) { log ->
        Text(text = log)
      }
    }
  }
}
