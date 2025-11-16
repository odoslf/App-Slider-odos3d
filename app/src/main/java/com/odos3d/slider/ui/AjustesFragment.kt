package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.odos3d.slider.R
import com.odos3d.slider.databinding.FragmentAjustesBinding
import com.odos3d.slider.settings.SettingsStore
import com.odos3d.slider.util.Logger
import kotlinx.coroutines.launch

class AjustesFragment : Fragment() {
    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: SettingsStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsStore.get(requireContext())

        val axes = resources.getStringArray(R.array.axis_values).toList()
        val axisAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, axes).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spAxis.adapter = axisAdapter

        // Cargar valores actuales
        lifecycleScope.launch { settings.deviceName.collect { binding.edtDeviceName.setText(it) } }
        lifecycleScope.launch { settings.deviceMac.collect { binding.edtDeviceMac.setText(it) } }
        lifecycleScope.launch { settings.pollHz.collect { binding.edtPollHz.setText(it.toString()) } }
        lifecycleScope.launch { settings.defaultStep.collect { binding.edtDefaultStep.setText(it.toString()) } }
        lifecycleScope.launch { settings.defaultFeed.collect { binding.edtDefaultFeed.setText(it.toString()) } }
        lifecycleScope.launch { settings.maxTravelMm.collect { binding.edtMaxTravel.setText(it.toString()) } }
        lifecycleScope.launch { settings.maxFeed.collect { binding.edtMaxFeed.setText(it.toString()) } }
        lifecycleScope.launch {
            settings.axisDefault.collect {
                val idx = axes.indexOf(it.uppercase())
                if (idx >= 0) binding.spAxis.setSelection(idx)
            }
        }
        lifecycleScope.launch { settings.offlineMode.collect { binding.swOffline.isChecked = it } }

        binding.spAxis.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val value = axes.getOrNull(position) ?: "X"
                lifecycleScope.launch { settings.saveAxis(value) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.swOffline.setOnCheckedChangeListener { _, checked ->
            lifecycleScope.launch { settings.setOfflineMode(checked) }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.edtDeviceName.text.toString().trim()
            val mac = binding.edtDeviceMac.text.toString().trim()
            val hz = binding.edtPollHz.text.toString().toIntOrNull() ?: 4
            val step = binding.edtDefaultStep.text.toString().toFloatOrNull() ?: 1f
            val feed = binding.edtDefaultFeed.text.toString().toIntOrNull() ?: 300
            val maxTravel = binding.edtMaxTravel.text.toString().toFloatOrNull() ?: 400f
            val maxFeed = binding.edtMaxFeed.text.toString().toIntOrNull() ?: 1500
            val axis = axes.getOrNull(binding.spAxis.selectedItemPosition)?.uppercase() ?: "X"

            if (maxTravel <= 0f || maxFeed <= 0) {
                Toast.makeText(requireContext(), getString(R.string.step_invalido), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                settings.saveDevice(name, mac)
                settings.savePollHz(hz)
                settings.saveDefaults(step, feed)
                settings.saveMaxTravelMm(maxTravel)
                settings.saveMaxFeed(maxFeed)
                settings.saveAxis(axis)
                Toast.makeText(requireContext(), getString(R.string.guardado_ok), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShareLogs.setOnClickListener {
            Logger.shareLogs(requireContext())
            Toast.makeText(requireContext(), getString(R.string.logs_shared), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
