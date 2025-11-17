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
import kotlinx.coroutines.flow.flowOf
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
    private var selectedAxis: Char = 'X'
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
        binding.btnHome.setOnClickListener {
            if (blockIfOffline()) return@setOnClickListener
            lifecycleScope.launch { client.sendLineBlocking("\$H") }
        }
        binding.btnUnlock.setOnClickListener {
            if (blockIfOffline()) return@setOnClickListener
            client.sendLine("\$X")
        }
        binding.btnPause.setOnClickListener {
            if (blockIfOffline()) return@setOnClickListener
            client.pause()
        }
        binding.btnResume.setOnClickListener {
            if (blockIfOffline()) return@setOnClickListener
            client.resume()
        }
        binding.btnReset.setOnClickListener {
            if (blockIfOffline()) return@setOnClickListener
            client.reset()
        }
        binding.btnCancelJog.setOnClickListener {
            if (blockIfOffline()) return@setOnClickListener
            client.cancelJog()
            jogActive = false
            updateJogControls()
        }

        binding.rbAxisX.setOnCheckedChangeListener { _, checked -> if (checked) selectedAxis = 'X' }
        binding.rbAxisY.setOnCheckedChangeListener { _, checked -> if (checked) selectedAxis = 'Y' }
        binding.rbAxisZ.setOnCheckedChangeListener { _, checked -> if (checked) selectedAxis = 'Z' }

        binding.btnMinus.setOnClickListener { jog(selectedAxis, -1f) }
        binding.btnPlus.setOnClickListener { jog(selectedAxis, 1f) }

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
            selectedAxis = axisDefault.firstOrNull() ?: 'X'
            when (selectedAxis) {
                'Y' -> binding.rbAxisY.isChecked = true
                'Z' -> binding.rbAxisZ.isChecked = true
                else -> binding.rbAxisX.isChecked = true
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.offlineMode.collectLatest {
                offlineMode = it
                updateOfflineUi()
            }
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
                client.startWatchdog(
                    scope = viewLifecycleOwner.lifecycleScope,
                    autoReconnectFlow = settings.autoReconnect,
                    intervalSecsFlow = settings.reconnectSecs
                )
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
        client.stopWatchdog()
        client.disconnect()
        GrblProvider.client = null
        onDisconnectUi()
    }

    private fun onDisconnectUi() {
        if (offlineMode) {
            binding.statusChip.text = getString(R.string.modo_offline)
        } else {
            binding.statusChip.setState(StatusChipView.State.DISCONNECTED)
            binding.tState.text = getString(R.string.sin_conexion)
        }
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
        if (blockIfOffline()) return
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

    private fun blockIfOffline(): Boolean {
        if (!offlineMode) return false
        Toast.makeText(requireContext(), getString(R.string.modo_offline), Toast.LENGTH_SHORT).show()
        return true
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

    private fun updateOfflineUi() {
        if (offlineMode) {
            binding.statusChip.text = getString(R.string.modo_offline)
            binding.tState.text = getString(R.string.modo_offline)
        } else {
            binding.statusChip.setState(
                if (client.isConnected()) StatusChipView.State.CONNECTED else StatusChipView.State.DISCONNECTED
            )
            binding.tState.text = if (client.isConnected()) getString(R.string.estado_conectado) else getString(R.string.sin_conexion)
        }
        updateJogControls()
    }

    private fun updateJogControls() {
        val allow = offlineMode || client.isConnected()
        binding.btnMinus.isEnabled = allow
        binding.btnPlus.isEnabled = allow
        binding.btnCancelJog.isEnabled = allow && jogActive
        binding.btnHome.isEnabled = allow && !offlineMode
        binding.btnUnlock.isEnabled = allow && !offlineMode
        binding.btnPause.isEnabled = allow && !offlineMode
        binding.btnResume.isEnabled = allow && !offlineMode
        binding.btnReset.isEnabled = allow && !offlineMode
        binding.btnConnect.isEnabled = !offlineMode
        binding.btnDisconnect.isEnabled = allow && !offlineMode
    }

    override fun onPause() {
        super.onPause()
        if (jogActive) client.cancelJog()
        stopPolling()
        client.stopWatchdog()
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
            if (!offlineMode) {
                binding.tState.text = text
                binding.statusChip.setState(if (client.isConnected()) StatusChipView.State.CONNECTED else StatusChipView.State.DISCONNECTED)
            }
        }
    }

    override fun onStatus(status: GrblStatus) {
        requireActivity().runOnUiThread {
            if (!offlineMode) {
                binding.tState.text = "${status.state}  WPos:${status.wpos ?: "?"}"
                updatePositionsFromStatus(status.wpos)
                binding.statusChip.setState(if (client.isConnected()) StatusChipView.State.CONNECTED else StatusChipView.State.DISCONNECTED)
            }
        }
    }

    private fun updatePositionsFromStatus(wpos: String?) {
        if (wpos.isNullOrBlank()) return
        val parts = wpos.split(',')
        if (parts.isNotEmpty()) parts.getOrNull(0)?.toFloatOrNull()?.let { positionsMm['X'] = it }
        if (parts.size > 1) parts.getOrNull(1)?.toFloatOrNull()?.let { positionsMm['Y'] = it }
        if (parts.size > 2) parts.getOrNull(2)?.toFloatOrNull()?.let { positionsMm['Z'] = it }
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
