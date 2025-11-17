package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.odos3d.slider.R
import com.odos3d.slider.databinding.FragmentAvanzadoBinding
import com.odos3d.slider.grbl.GrblProvider
import com.odos3d.slider.settings.SettingsStore
import com.odos3d.slider.ui.components.StatusChipView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AvanzadoFragment : Fragment() {
    private var _binding: FragmentAvanzadoBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: SettingsStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvanzadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsStore.get(requireContext())
        binding.statusChip.setState(StatusChipView.State.DISCONNECTED)
        observeSettings()
        binding.btnManual.setOnClickListener { findNavController().navigate(R.id.nav_manual) }
        binding.btnScenes.setOnClickListener { findNavController().navigate(R.id.nav_scenes) }
        binding.btnAjustes.setOnClickListener { findNavController().navigate(R.id.nav_ajustes) }
        refreshConnection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(settings.deviceName, settings.deviceMac) { name, mac -> name to mac }
                .collectLatest { (name, mac) ->
                    binding.tDevice.text = getString(
                        R.string.aj_bt_dispositivo_val,
                        name.ifBlank { getString(R.string.dispositivo_no_seleccionado) },
                        mac.ifBlank { "—" }
                    )
                }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(settings.axisDefault, settings.maxTravelMm, settings.maxFeed) { axis, travel, feed ->
                Triple(axis, travel, feed)
            }.collectLatest { (axis, travel, feed) ->
                binding.tLimits.text = getString(R.string.info_limits, axis, travel, feed)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(settings.defaultStep, settings.defaultFeed) { step, feed -> step to feed }
                .collectLatest { (step, feed) ->
                    binding.tDefault.text = getString(R.string.info_defaults, step, feed)
                }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            settings.offlineMode.collectLatest { offline ->
                if (offline) {
                    binding.statusChip.setState(StatusChipView.State.DISCONNECTED)
                } else {
                    refreshConnection()
                }
            }
        }
    }

    private fun refreshConnection() {
        val connected = GrblProvider.client?.isConnected() == true
        binding.statusChip.setState(if (connected) StatusChipView.State.CONNECTED else StatusChipView.State.DISCONNECTED)
    }
}
