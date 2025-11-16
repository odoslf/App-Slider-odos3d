package com.odos3d.slider.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.odos3d.slider.R
import com.odos3d.slider.databinding.FragmentPlaceholderBinding

class BasicoFragment : Fragment() {
    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tTitle.text = getString(R.string.nav_basico)
        binding.tDesc.text = getString(R.string.ph_basico)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
