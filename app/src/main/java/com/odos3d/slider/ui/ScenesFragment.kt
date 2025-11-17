package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.odos3d.slider.R
import com.odos3d.slider.databinding.FragmentScenesBinding
import com.odos3d.slider.databinding.ItemSceneBinding
import com.odos3d.slider.grbl.GrblProvider
import com.odos3d.slider.scenes.SceneTemplate
import com.odos3d.slider.scenes.SceneTemplates
import com.odos3d.slider.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScenesFragment : Fragment() {

    private var _binding: FragmentScenesBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: SettingsStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScenesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        settings = SettingsStore.get(requireContext())
        binding.recycler.adapter = ScenesAdapter(SceneTemplates.all) { template ->
            viewLifecycleOwner.lifecycleScope.launch {
                val offline = settings.offlineMode.first()
                val grbl = GrblProvider.client
                val anchor = requireActivity().findViewById<View>(R.id.bottom_nav)
                val targetView = anchor ?: requireView()

                if (offline) {
                    Snackbar.make(targetView, getString(R.string.modo_offline), Snackbar.LENGTH_SHORT)
                        .apply { anchor?.let { setAnchorView(it) } }
                        .show()
                    return@launch
                }

                val activeClient = if (grbl?.isConnected() == true) {
                    grbl
                } else {
                    GrblProvider.ensureConnected(
                        context = requireContext(),
                        settings = settings,
                        scope = viewLifecycleOwner.lifecycleScope
                    )
                }

                if (activeClient == null || !activeClient.isConnected()) {
                    Snackbar.make(targetView, getString(R.string.sin_conexion), Snackbar.LENGTH_SHORT)
                        .apply { anchor?.let { setAnchorView(it) } }
                        .show()
                    return@launch
                }

                showPresetSnack(template.title, template.intervalSec, autoStart = true)
                val args = bundleOf(
                    "presetTitle" to template.title,
                    "autoIntervalSec" to template.intervalSec,
                    "autoStart" to true,
                    "presetId" to template.id,
                    "runMovementInCamera" to true,
                )
                findNavController().navigate(R.id.camaraFragment, args)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showPresetSnack(title: String, intervalSec: Int, autoStart: Boolean) {
        val anchor = requireActivity().findViewById<View>(R.id.bottom_nav)
        val targetView = anchor ?: requireView()
        Snackbar.make(targetView, getString(R.string.preset_started, title), Snackbar.LENGTH_SHORT)
            .apply { anchor?.let { setAnchorView(it) } }
            .show()
        if (autoStart) {
            Snackbar.make(targetView, getString(R.string.preset_auto_started), Snackbar.LENGTH_SHORT)
                .apply { anchor?.let { setAnchorView(it) } }
                .show()
        }
        Snackbar.make(targetView, getString(R.string.preset_started_interval, intervalSec), Snackbar.LENGTH_SHORT)
            .apply { anchor?.let { setAnchorView(it) } }
            .show()
    }

    private class ScenesAdapter(
        private val items: List<SceneTemplate>,
        private val onClick: (SceneTemplate) -> Unit
    ) : RecyclerView.Adapter<ScenesAdapter.VH>() {

        class VH(val vb: ItemSceneBinding) : RecyclerView.ViewHolder(vb.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val vb = ItemSceneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(vb)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val template = items[position]
            holder.vb.tTitle.text = template.title
            holder.vb.tSubtitle.text = "${template.durationMin} min · ${template.intervalSec}s/Foto"
            holder.vb.tDesc.text = template.description
            holder.vb.root.setOnClickListener { onClick(template) }
        }
    }
}
