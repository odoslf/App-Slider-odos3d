package com.odos3d.slider.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.odos3d.slider.core.datastore.DataStoreManager
import kotlinx.coroutines.launch

@Composable
fun AjustesScreen() {
  val context = LocalContext.current
  val dataStore = remember { DataStoreManager(context.applicationContext) }
  val scope = rememberCoroutineScope()

  val storedMac by dataStore.btAddress.collectAsState(initial = "")
  val storedInterval by dataStore.intervalMs.collectAsState(initial = 2000)
  val storedSpeed by dataStore.speed.collectAsState(initial = 600)

  var macInput by rememberSaveable { mutableStateOf("") }
  var intervalInput by rememberSaveable { mutableStateOf(storedInterval.toString()) }
  var speedInput by rememberSaveable { mutableStateOf(storedSpeed.toString()) }

  LaunchedEffect(storedMac) { macInput = storedMac }
  LaunchedEffect(storedInterval) { intervalInput = storedInterval.toString() }
  LaunchedEffect(storedSpeed) { speedInput = storedSpeed.toString() }

  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Ajustes de la aplicación")

    OutlinedTextField(
      value = macInput,
      onValueChange = { macInput = it.trim() },
      label = { Text("MAC por defecto") },
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = intervalInput,
      onValueChange = { intervalInput = it.filter { ch -> ch.isDigit() } },
      label = { Text("Intervalo timelapse (ms)") },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    OutlinedTextField(
      value = speedInput,
      onValueChange = { speedInput = it.filter { ch -> ch.isDigit() } },
      label = { Text("Velocidad slider (mm/min)") },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    Button(onClick = {
      val interval = intervalInput.toIntOrNull()
      val speed = speedInput.toIntOrNull()
      if (interval == null || speed == null) {
        Toast.makeText(context, "Valores inválidos", Toast.LENGTH_SHORT).show()
        return@Button
      }
      scope.launch {
        dataStore.setBt(macInput)
        dataStore.setInterval(interval)
        dataStore.setSpeed(speed)
        Toast.makeText(context, "Ajustes guardados", Toast.LENGTH_SHORT).show()
      }
    }) {
      Text("Guardar")
    }
  }
}
