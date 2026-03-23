package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.databinding.FragmentGameToolsBinding
import com.ultra.optimize.x.utils.SettingsManager

class GameToolsFragment : Fragment() {

    private var _binding: FragmentGameToolsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val fpsEnabled = SettingsManager.getSetting(requireContext(), "fps_meter_enabled")
        val crosshairEnabled = SettingsManager.getSetting(requireContext(), "crosshair_enabled")
        
        binding.switchFps.isChecked = fpsEnabled
        binding.switchCrosshair.isChecked = crosshairEnabled

        binding.switchFps.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "fps_meter_enabled", isChecked)
            if (isChecked) {
                Toast.makeText(requireContext(), "FPS Meter Overlay Enabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchCrosshair.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "crosshair_enabled", isChecked)
            if (isChecked) {
                Toast.makeText(requireContext(), "Crosshair Overlay Enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
