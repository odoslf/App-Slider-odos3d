package com.odos3d.slider.ui.conexion

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.odos3d.slider.R
import com.odos3d.slider.settings.SettingsStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ConnectActivity : ComponentActivity() {

    private val scope = MainScope()
    private lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_connect)
        settings = SettingsStore.get(this)

        val recycler = findViewById<RecyclerView>(R.id.rvDevices)
        recycler.layoutManager = LinearLayoutManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 100)
            }
        }

        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = manager.adapter
        val bonded = adapter?.bondedDevices?.map { it.name.orEmpty() to it.address.orEmpty() } ?: emptyList()

        recycler.adapter = object : RecyclerView.Adapter<DeviceViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
                return DeviceViewHolder(view)
            }

            override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
                val (name, mac) = bonded[position]
                holder.name.text = name.ifBlank { getString(R.string.dispositivo_no_seleccionado) }
                holder.mac.text = mac
                holder.itemView.setOnClickListener {
                    scope.launch { settings.saveDevice(name, mac) }
                    finish()
                }
            }

            override fun getItemCount(): Int = bonded.size
        }
    }

    private class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tName)
        val mac: TextView = view.findViewById(R.id.tMac)
    }
}
