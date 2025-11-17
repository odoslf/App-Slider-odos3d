package com.odos3d.slider.ui.conexion

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
    private val items = mutableListOf<Pair<String, String>>()
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val dev: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val name = dev?.name.orEmpty()
                    val mac = dev?.address.orEmpty()
                    if (mac.isNotBlank() && items.none { it.second == mac }) {
                        items.add(name to mac)
                        refreshList()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(this@ConnectActivity, getString(R.string.scan_finished), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_connect)
        settings = SettingsStore.get(this)

        val recycler = findViewById<RecyclerView>(R.id.rvDevices)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = DeviceAdapter()

        ensurePermissions()
        registerScanReceiver()
        loadBonded()
        startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDiscovery()
        if (receiverRegistered) unregisterReceiver(receiver)
    }

    private fun ensurePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            val scanGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            val req = mutableListOf<String>()
            if (!connectGranted) req.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!scanGranted) req.add(Manifest.permission.BLUETOOTH_SCAN)
            if (req.isNotEmpty()) ActivityCompat.requestPermissions(this, req.toTypedArray(), 100)
        } else {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!fine) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    private fun loadBonded() {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = manager.adapter
        items.clear()
        val bonded = adapter?.bondedDevices?.map { it.name.orEmpty() to it.address.orEmpty() } ?: emptyList()
        items.addAll(bonded)
        refreshList()
    }

    private fun startDiscovery() {
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: return
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        adapter.startDiscovery()
    }

    private fun stopDiscovery() {
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter?.isDiscovering == true) adapter.cancelDiscovery()
    }

    private fun refreshList() {
        (findViewById<RecyclerView>(R.id.rvDevices).adapter as? DeviceAdapter)?.submit(items)
    }

    private fun registerScanReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)
        receiverRegistered = true
    }

    private inner class DeviceAdapter : RecyclerView.Adapter<DeviceViewHolder>() {
        private val data = mutableListOf<Pair<String, String>>()
        fun submit(list: List<Pair<String, String>>) {
            data.clear(); data.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            return DeviceViewHolder(view)
        }
        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val (name, mac) = data[position]
            holder.name.text = name.ifBlank { getString(R.string.dispositivo_no_seleccionado) }
            holder.mac.text = mac
            holder.itemView.setOnClickListener {
                scope.launch { settings.saveDevice(name, mac) }
                Toast.makeText(this@ConnectActivity, getString(R.string.guardado_ok), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        override fun getItemCount(): Int = data.size
    }

    private class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tName)
        val mac: TextView = view.findViewById(R.id.tMac)
    }
}
