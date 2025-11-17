package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.odos3d.slider.R
import com.odos3d.slider.databinding.FragmentBasicoBinding
import com.odos3d.slider.grbl.GrblProvider
import com.odos3d.slider.settings.SettingsStore
import com.odos3d.slider.ui.components.StatusChipView
import com.odos3d.slider.ui.conexion.ConnectActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BasicoFragment : Fragment() {
    private var _binding: FragmentBasicoBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: SettingsStore
    private var deviceName: String = ""
    private var deviceMac: String = ""
    private var axis: String = "X"
    private var maxTravel: Float = 0f
    private var maxFeed: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBasicoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsStore.get(requireContext())
        binding.statusChip.setState(StatusChipView.State.DISCONNECTED)
        observeSettings()
        refreshConnectionState()

        binding.btnConnect.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), ConnectActivity::class.java))
        }
        binding.btnManual.setOnClickListener { findNavController().navigate(R.id.nav_manual) }
        binding.btnCamera.setOnClickListener { findNavController().navigate(R.id.camaraFragment) }
        binding.btnEscenas.setOnClickListener { findNavController().navigate(R.id.nav_scenes) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            settings.deviceName.collectLatest { name ->
                deviceName = name
                updateStatusTexts()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.deviceMac.collectLatest { mac ->
                deviceMac = mac
                updateStatusTexts()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.offlineMode.collectLatest { offline ->
                binding.tMode.text = if (offline) {
                    getString(R.string.modo_offline)
                } else {
                    getString(R.string.estado_conectado)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.maxTravelMm.collectLatest {
                maxTravel = it
                updateStatusTexts()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.maxFeed.collectLatest {
                maxFeed = it
                updateStatusTexts()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.axisDefault.collectLatest {
                axis = it
                updateStatusTexts()
            }
        }
    }

    private fun updateStatusTexts() {
        binding.tDevice.text = getString(
            R.string.aj_bt_dispositivo_val,
            deviceName.ifBlank { getString(R.string.dispositivo_no_seleccionado) },
            deviceMac.ifBlank { "—" }
        )
        if (maxTravel > 0f && maxFeed > 0) {
            binding.tLimits.text = getString(R.string.info_limits, axis, maxTravel, maxFeed)
        }
    }

    private fun refreshConnectionState() {
        val connected = GrblProvider.client?.isConnected() == true
        binding.statusChip.setState(
            if (connected) StatusChipView.State.CONNECTED else StatusChipView.State.DISCONNECTED
        )
        if (!connected) {
            Toast.makeText(requireContext(), getString(R.string.sin_conexion), Toast.LENGTH_SHORT).show()
        }
    }
}
