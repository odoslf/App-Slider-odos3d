package com.odos3d.slider.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.odos3d.slider.databinding.FragmentGalleryBinding
import com.odos3d.slider.databinding.ItemMediaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val adapter = MediaAdapter()
    private val needReadPerm = (Build.VERSION.SDK_INT >= 33)

    private val readPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadMedia()
        else Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.list.adapter = adapter

        binding.btnReload.setOnClickListener { loadMedia() }
        binding.btnExport.setOnClickListener { goExport() }

        requestPermissionAndLoad()
    }

    private fun requestPermissionAndLoad() {
        if (needReadPerm) {
            readPerm.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // <= Android 12: no pedimos nada si solo mostramos nuestras propias imágenes recientes
            loadMedia()
        }
    }

    private fun loadMedia() {
        lifecycleScope.launch {
            binding.progress.isVisible = true
            val items = withContext(Dispatchers.IO) {
                queryPictures("Slider-odos3d")
            }
            binding.progress.isVisible = false
            adapter.submit(items)
            binding.empty.isVisible = items.isEmpty()
        }
    }

    private fun goExport() {
        val selection = adapter.selectedUris()
        if (selection.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona al menos 1 foto", Toast.LENGTH_SHORT).show()
            return
        }
        val bundle = Bundle().apply {
            putStringArrayList("uris", ArrayList(selection.map { it.toString() }))
        }
        parentFragmentManager.beginTransaction()
            .replace(this.id, ExportFragment().apply { arguments = bundle })
            .addToBackStack("export")
            .commit()
    }

    private fun queryPictures(folderName: String): List<Uri> {
        val resolver = requireContext().contentResolver
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME}=?"
        val args = arrayOf(folderName)
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val list = mutableListOf<Uri>()
        resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, args, sort)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                list.add(uri)
            }
        }
        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class MediaAdapter : RecyclerView.Adapter<MediaAdapter.VH>() {
        private val data = mutableListOf<Uri>()
        private val selected = linkedSetOf<Uri>()

        fun submit(items: List<Uri>) {
            data.clear(); selected.clear()
            data.addAll(items)
            notifyDataSetChanged()
        }

        fun selectedUris(): List<Uri> = selected.toList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val uri = data[position]
            holder.bind(uri, selected.contains(uri))
            holder.itemView.setOnClickListener {
                if (selected.contains(uri)) selected.remove(uri) else selected.add(uri)
                notifyItemChanged(position)
            }
        }

        class VH(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(uri: Uri, checked: Boolean) {
                binding.thumb.setImageURI(uri)
                binding.check.isChecked = checked
            }
        }
    }
}
