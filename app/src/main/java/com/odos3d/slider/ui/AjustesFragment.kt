package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        lifecycleScope.launch { settings.deviceName.collect { binding.edtDeviceName.setText(it) } }
        lifecycleScope.launch { settings.deviceMac.collect { binding.edtDeviceMac.setText(it) } }
        lifecycleScope.launch { settings.pollHz.collect { binding.edtPollHz.setText(it.toString()) } }
        lifecycleScope.launch { settings.defaultStep.collect { binding.edtDefaultStep.setText(it.toString()) } }
        lifecycleScope.launch { settings.defaultFeed.collect { binding.edtDefaultFeed.setText(it.toString()) } }

        binding.btnSave.setOnClickListener {
            val name = binding.edtDeviceName.text.toString().trim()
            val mac = binding.edtDeviceMac.text.toString().trim()
            val hz = binding.edtPollHz.text.toString().toIntOrNull() ?: 4
            val step = binding.edtDefaultStep.text.toString().toFloatOrNull() ?: 1f
            val feed = binding.edtDefaultFeed.text.toString().toIntOrNull() ?: 300

            lifecycleScope.launch {
                settings.saveDevice(name, mac)
                settings.savePollHz(hz)
                settings.saveDefaults(step, feed)
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
