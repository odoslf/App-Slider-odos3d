package com.odos3d.slider

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.odos3d.slider.ui.SliderNavHost

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    val permissions = buildList {
      add(Manifest.permission.CAMERA)
      add(Manifest.permission.RECORD_AUDIO)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
        add(Manifest.permission.READ_MEDIA_IMAGES)
      }
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_CONNECT)
        add(Manifest.permission.BLUETOOTH_SCAN)
      } else {
        add(Manifest.permission.BLUETOOTH)
        add(Manifest.permission.BLUETOOTH_ADMIN)
      }
    }

    setContent {
      val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
      ) { }

      LaunchedEffect(Unit) {
        launcher.launch(permissions.toTypedArray())
      }

      MaterialTheme {
        Surface(modifier = Modifier) {
          SliderNavHost()
        }
      }
    }
  }
}
