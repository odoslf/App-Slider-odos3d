package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.odos3d.slider.R
import com.odos3d.slider.databinding.FragmentTimelapseBinding

class TimelapseFragment : Fragment() {
    private var _binding: FragmentTimelapseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTimelapseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOpenGallery.setOnClickListener {
            findNavController().navigate(R.id.galleryFragment)
        }
        binding.btnOpenScenes.setOnClickListener {
            findNavController().navigate(R.id.nav_scenes)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
