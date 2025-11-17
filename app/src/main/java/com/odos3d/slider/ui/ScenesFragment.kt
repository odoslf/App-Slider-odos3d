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
import com.odos3d.slider.scenes.SceneRunner
import com.odos3d.slider.scenes.SceneTemplate
import com.odos3d.slider.scenes.SceneTemplates
import com.odos3d.slider.settings.SettingsStore
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
        settings = SettingsStore.get(requireContext())
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = ScenesAdapter(SceneTemplates.all) { template ->
            showPresetSnack(template.title, template.intervalSec, autoStart = true)
            val args = bundleOf(
                "presetTitle" to template.title,
                "autoIntervalSec" to template.intervalSec,
                "autoStart" to true,
                "presetId" to template.id,
            )
            findNavController().navigate(R.id.camaraFragment, args)
            launchSceneIfPossible(template)
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

    private fun launchSceneIfPossible(template: SceneTemplate) {
        val grbl = GrblProvider.client ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val runner = SceneRunner(viewLifecycleOwner.lifecycleScope, settings) { GrblProvider.client }
            runner.runPreset(template)
        }
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
