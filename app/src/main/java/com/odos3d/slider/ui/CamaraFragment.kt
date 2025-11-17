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
import com.odos3d.slider.grbl.GrblProvider
import com.odos3d.slider.link.LinkPermissions
import com.odos3d.slider.scenes.SceneTemplates
import com.odos3d.slider.scenes.ScenesRunner
import com.odos3d.slider.scenes.ScenesRunner.Failure
import com.odos3d.slider.scenes.ScenesRunner.MoveResult
import com.odos3d.slider.settings.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

class CamaraFragment : Fragment() {

    private var _binding: FragmentCamaraBinding? = null
    private val binding get() = _binding!!

    private lateinit var controller: TimelapseController
    private lateinit var settings: SettingsStore
    private var grblClient = GrblProvider.client
    private var runner: ScenesRunner? = null

    private var autoIntervalSec: Int? = null
    private var autoStart: Boolean = false
    private var presetTitle: String? = null
    private var presetId: String? = null
    private var runMovementInCamera: Boolean = true
    private val preset by lazy { presetId?.let { id -> SceneTemplates.all.find { it.id == id } } }
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
            presetId = args.getString("presetId")
            runMovementInCamera = args.getBoolean("runMovementInCamera", true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCamaraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        settings = SettingsStore.get(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            grblClient = GrblProvider.ensureConnected(
                context = requireContext(),
                settings = settings,
                scope = viewLifecycleOwner.lifecycleScope
            )
        }
        controller = TimelapseController(
            fragment = this,
            previewView = binding.preview
        ) { name ->
            shots++
            binding.tStatus.text = getString(R.string.estado_fotos, shots, name)
        }

        runner = if (runMovementInCamera) {
            ScenesRunner(viewLifecycleOwner.lifecycleScope, settings) { grblClient ?: GrblProvider.client }
        } else null

        if (runMovementInCamera) {
            controller.onBeforeCapture = { _, _ ->
                val tpl = preset
                if (tpl != null && tpl.moveBeforeShot) handleMove(tpl)
            }
            controller.onAfterCapture = { _, _ ->
                val tpl = preset
                if (tpl != null && !tpl.moveBeforeShot) handleMove(tpl)
            }
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
            val totalShots = preset?.let { tpl -> max(1, (tpl.durationMin * 60) / tpl.intervalSec) }
            runner?.resetProgress()
            controller.start(intervalSec = interval, totalShots = totalShots, presetTitle = presetTitle)
            binding.tStatus.text = getString(R.string.estado_capturando, interval)
        }
    }

    private fun stopTimelapse() {
        controller.stop()
        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
        binding.tStatus.text = getString(R.string.listo)
    }

    private fun handleMove(tpl: com.odos3d.slider.scenes.SceneTemplate) {
        if (runner == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            if (settings.offlineMode.first()) {
                infoSnackText(getString(R.string.modo_offline))
                return@launch
            }
            if (grblClient?.isConnected() != true) {
                grblClient = GrblProvider.ensureConnected(requireContext(), settings, viewLifecycleOwner.lifecycleScope)
            }
            val result = runner?.moveOnce(tpl)
                ?: MoveResult(false, Failure.NO_CLIENT)
            if (!result.success) {
                when (result.reason) {
                    Failure.OFFLINE -> infoSnackText(getString(R.string.modo_offline))
                    Failure.NO_CLIENT, Failure.SEND_FAIL -> infoSnackText(getString(R.string.sin_conexion))
                    Failure.LIMIT -> infoSnackText(
                        getString(
                            R.string.fuera_limites,
                            0f,
                            result.limitMm ?: 0f
                        )
                    )
                    null -> infoSnackText(getString(R.string.sin_conexion))
                }
            }
        }
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

    private fun infoSnackText(text: String) {
        val anchor = requireActivity().findViewById<View>(R.id.bottom_nav)
        val target = anchor ?: requireView()
        Snackbar.make(target, text, Snackbar.LENGTH_SHORT)
            .apply { anchor?.let { setAnchorView(it) } }
            .show()
    }
}
