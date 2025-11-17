package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.odos3d.slider.databinding.FragmentExportBinding
import com.odos3d.slider.work.ExportWorker
import java.util.ArrayList

class ExportFragment : Fragment() {
    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private var uris: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uris = arguments?.getStringArrayList("uris") ?: arrayListOf()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tCount.text = "Fotos seleccionadas: ${uris.size}"

        binding.btnExport.setOnClickListener {
            val fps = binding.edtFps.text.toString().toIntOrNull()?.coerceIn(1, 60) ?: 24
            val scaleW = binding.edtWidth.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0

            val input = Data.Builder()
                .putStringArray(ExportWorker.KEY_URIS, uris.toTypedArray())
                .putInt(ExportWorker.KEY_FPS, fps)
                .putInt(ExportWorker.KEY_SCALE_W, scaleW)
                .build()

            val req = OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(input)
                .build()

            WorkManager.getInstance(requireContext()).enqueue(req)
            Toast.makeText(requireContext(), "Exportación en curso…", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
