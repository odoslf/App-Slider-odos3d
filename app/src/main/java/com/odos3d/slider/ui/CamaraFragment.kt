package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.odos3d.slider.R
import com.odos3d.slider.camera.TimelapseController
import com.odos3d.slider.databinding.FragmentCamaraBinding
import com.odos3d.slider.link.LinkPermissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CamaraFragment : Fragment() {

    private var _binding: FragmentCamaraBinding? = null
    private val binding get() = _binding!!

    private lateinit var controller: TimelapseController

    private var autoIntervalSec: Int? = null
    private var autoStart: Boolean = false
    private var presetTitle: String? = null
    private var shots = 0

    private val camPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Si lo conceden, intentamos (re)bind
        lifecycleScope.launch { controller.bindIfNeeded() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            autoIntervalSec = args.getInt("autoIntervalSec", -1).takeIf { it > 0 }
            autoStart = args.getBoolean("autoStart", false)
            presetTitle = args.getString("presetTitle")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCamaraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        controller = TimelapseController(
            fragment = this,
            previewView = binding.preview
        ) { name ->
            shots++
            binding.tStatus.text = getString(R.string.estado_fotos, shots, name)
        }

        // UI de contexto del preset
        binding.tSceneTitle.isVisible = !presetTitle.isNullOrBlank()
        binding.tSceneTitle.text = presetTitle ?: ""

        // Precarga de intervalo desde preset
        autoIntervalSec?.let { binding.edtInterval.setText(it.toString()) }

        // Pide permiso de cámara si hace falta y bindea preview
        LinkPermissions.requestCamera(camPerm)

        binding.btnStart.setOnClickListener { startTimelapse() }
        binding.btnStop.setOnClickListener { stopTimelapse() }

        // Auto-arranque si procede
        if (autoStart) {
            infoSnack(R.string.preset_auto_started)
            lifecycleScope.launch {
                // Pequeño delay para asegurar preview lista
                delay(350)
                startTimelapse()
            }
        }
    }

    private fun startTimelapse() {
        val interval = binding.edtInterval.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 2
        binding.btnStart.isEnabled = false
        binding.btnStop.isEnabled = true
        shots = 0
        lifecycleScope.launch {
            controller.bindIfNeeded()
            controller.start(intervalSec = interval, presetTitle = presetTitle)
            binding.tStatus.text = getString(R.string.estado_capturando, interval)
        }
    }

    private fun stopTimelapse() {
        controller.stop()
        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
        binding.tStatus.text = getString(R.string.listo)
    }

    override fun onPause() {
        super.onPause()
        // Seguridad: detener si salen de pantalla
        stopTimelapse()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.shutdown()
        _binding = null
    }

    private fun infoSnack(@StringRes msg: Int) {
        val anchor = requireActivity().findViewById<View>(R.id.bottom_nav)
        val target = anchor ?: requireView()
        Snackbar.make(target, getString(msg), Snackbar.LENGTH_SHORT)
            .apply { anchor?.let { setAnchorView(it) } }
            .show()
    }
}
