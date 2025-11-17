package com.odos3d.slider.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.odos3d.slider.R
import com.odos3d.slider.databinding.FragmentManualBinding
import com.odos3d.slider.grbl.GrblClient
import com.odos3d.slider.grbl.GrblListener
import com.odos3d.slider.grbl.GrblProvider
import com.odos3d.slider.grbl.GrblStatus
import com.odos3d.slider.link.BtTransport
import com.odos3d.slider.settings.SettingsStore
import com.odos3d.slider.ui.components.StatusChipView
import com.odos3d.slider.ui.conexion.ConnectActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ManualFragment : Fragment(), GrblListener {

    private var _binding: FragmentManualBinding? = null
    private val binding get() = _binding!!

    private lateinit var transport: BtTransport
    private lateinit var client: GrblClient
    private lateinit var settings: SettingsStore

    private var pollJob: Job? = null
    private var jogActive = false
    private var lastJogMs = 0L

    private var deviceName: String = "—"
    private var deviceMac: String = ""
    private var pollHz: Int = 4
    private var defaultStep: Float = 1f
    private var defaultFeed: Int = 300
    private var maxFeed: Int = 1500
    private var maxTravel: Float = 400f
    private var axisDefault: String = "X"
    private var offlineMode: Boolean = false

    private val positionsMm = mutableMapOf<Char, Float>().apply {
        this['X'] = 0f; this['Y'] = 0f; this['Z'] = 0f
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManualBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsStore.get(requireContext())
        transport = BtTransport(requireContext())
        client = GrblClient(transport, this)

        observeSettings()

        binding.statusChip.setState(StatusChipView.State.DISCONNECTED)
        binding.tState.text = getString(R.string.sin_conexion)

        binding.btnPick.setOnClickListener {
            startActivity(Intent(requireContext(), ConnectActivity::class.java))
        }
        binding.btnConnect.setOnClickListener { startConnection() }
        binding.btnDisconnect.setOnClickListener { disconnect() }
        binding.btnHome.setOnClickListener { lifecycleScope.launch { client.sendLineBlocking("\$H") } }
        binding.btnUnlock.setOnClickListener { client.sendLine("\$X") }
        binding.btnPause.setOnClickListener { client.pause() }
        binding.btnResume.setOnClickListener { client.resume() }
        binding.btnReset.setOnClickListener { client.reset() }
        binding.btnCancelJog.setOnClickListener {
            client.cancelJog()
            jogActive = false
            updateJogControls()
        }

        binding.btnXminus.setOnClickListener { jog('X', -1f) }
        binding.btnXplus.setOnClickListener { jog('X', 1f) }
        binding.btnYminus.setOnClickListener { jog('Y', -1f) }
        binding.btnYplus.setOnClickListener { jog('Y', 1f) }

        updateTitle()
        updateJogControls()
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            settings.deviceName.collectLatest {
                deviceName = it
                updateTitle()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.deviceMac.collectLatest {
                deviceMac = it
                updateTitle()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.pollHz.collectLatest { pollHz = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.defaultStep.collectLatest { defaultStep = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.defaultFeed.collectLatest { defaultFeed = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.maxFeed.collectLatest { maxFeed = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.maxTravelMm.collectLatest { maxTravel = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.axisDefault.collectLatest { axisDefault = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.offlineMode.collectLatest { offlineMode = it }
        }
    }

    private fun startConnection() {
        if (offlineMode) {
            Toast.makeText(requireContext(), getString(R.string.modo_offline), Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), getString(R.string.permiso_necesario), Toast.LENGTH_SHORT).show()
            return
        }
        if (deviceMac.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.seleccionar_dispositivo), Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnConnect.isEnabled = false
        binding.statusChip.setState(StatusChipView.State.CONNECTING)
        binding.tState.text = getString(R.string.conectando)
        client.connectWithRetries(deviceMac)
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = waitUntilConnected(4000)
            binding.btnConnect.isEnabled = true
            if (ok) {
                binding.statusChip.setState(StatusChipView.State.CONNECTED)
                GrblProvider.client = client
                startPolling()
                updateJogControls()
            } else {
                onDisconnectUi()
                Toast.makeText(requireContext(), getString(R.string.sin_conexion), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disconnect() {
        stopPolling()
        client.disconnect()
        GrblProvider.client = null
        onDisconnectUi()
    }

    private fun onDisconnectUi() {
        binding.statusChip.setState(StatusChipView.State.DISCONNECTED)
        binding.tState.text = getString(R.string.sin_conexion)
        binding.btnConnect.isEnabled = true
        jogActive = false
        lastJogMs = 0L
        updateJogControls()
    }

    private suspend fun waitUntilConnected(timeoutMs: Long): Boolean {
        var remaining = timeoutMs
        while (remaining > 0) {
            if (client.isConnected()) return true
            delay(100)
            remaining -= 100
        }
        return client.isConnected()
    }

    private fun startPolling() {
        stopPolling()
        pollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && client.isConnected()) {
                client.queryStatus()
                delay((1000f / pollHz).toLong())
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun jog(axis: Char, sign: Float) {
        if (offlineMode) {
            Toast.makeText(requireContext(), getString(R.string.modo_offline), Toast.LENGTH_SHORT).show()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastJogMs < 250L) return

        val step = binding.edtStep.text.toString().toFloatOrNull() ?: defaultStep
        val feed = binding.edtFeed.text.toString().toIntOrNull() ?: defaultFeed
        if (step <= 0f) {
            Toast.makeText(requireContext(), getString(R.string.step_invalido), Toast.LENGTH_SHORT).show()
            return
        }
        if (feed !in 1..maxFeed) {
            Toast.makeText(requireContext(), getString(R.string.feed_invalido), Toast.LENGTH_SHORT).show()
            return
        }

        val currentPos = positionsMm[axis] ?: 0f
        val candidate = currentPos + (sign * step)
        if (candidate < 0f || candidate > maxTravel) {
            Toast.makeText(
                requireContext(),
                getString(R.string.fuera_limites, 0f, maxTravel),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val cmd = "${'$'}J=G21 G91 ${axis}${"%.3f".format(sign * step)} F${feed.coerceIn(1, maxFeed)}"
        lastJogMs = now
        jogActive = true
        client.sendLine(cmd)
        positionsMm[axis] = candidate
        updateJogControls()
    }

    private fun updateTitle() {
        val mac = if (deviceMac.isBlank()) "—" else deviceMac
        binding.tDevice.text = "$deviceName ($mac)"
        binding.tLimits.text = getString(
            R.string.info_limits,
            axisDefault,
            maxTravel,
            maxFeed
        )
    }

    private fun updateJogControls() {
        val connected = client.isConnected()
        binding.btnXminus.isEnabled = connected
        binding.btnXplus.isEnabled = connected
        binding.btnYminus.isEnabled = connected
        binding.btnYplus.isEnabled = connected
        binding.btnCancelJog.isEnabled = connected && jogActive
        binding.btnHome.isEnabled = connected
        binding.btnUnlock.isEnabled = connected
        binding.btnPause.isEnabled = connected
        binding.btnResume.isEnabled = connected
        binding.btnReset.isEnabled = connected
    }

    override fun onPause() {
        super.onPause()
        if (jogActive) client.cancelJog()
        stopPolling()
        client.disconnect()
        GrblProvider.client = null
        onDisconnectUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // GrblListener
    override fun onState(text: String) {
        requireActivity().runOnUiThread {
            binding.tState.text = text
            binding.statusChip.setState(if (client.isConnected()) StatusChipView.State.CONNECTED else StatusChipView.State.DISCONNECTED)
        }
    }

    override fun onStatus(status: GrblStatus) {
        requireActivity().runOnUiThread {
            binding.tState.text = "${status.state}  WPos:${status.wpos ?: "?"}"
            binding.statusChip.setState(if (client.isConnected()) StatusChipView.State.CONNECTED else StatusChipView.State.DISCONNECTED)
        }
    }

    override fun onError(msg: String) {
        requireActivity().runOnUiThread {
            binding.statusChip.setState(StatusChipView.State.DISCONNECTED)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            updateJogControls()
        }
    }

    override fun onAlarm(msg: String) {
        requireActivity().runOnUiThread {
            binding.statusChip.setState(StatusChipView.State.ALARM)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }
    }
}
