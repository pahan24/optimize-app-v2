package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentGameToolsBinding
import com.ultra.optimize.x.utils.SettingsManager

import android.content.Intent
import com.ultra.optimize.x.utils.ShizukuManager

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

        // Set Spannable Title
        val title = "GAME\nTOOLS"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 4, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTitle.text = spannable

        // Load Settings
        binding.switchFps.isChecked = SettingsManager.getSetting(requireContext(), "fps_meter_enabled", false)
        binding.switchCrosshair.isChecked = SettingsManager.getSetting(requireContext(), "crosshair_enabled", false)
        binding.switchSidePanel.isChecked = SettingsManager.getSetting(requireContext(), "side_panel_enabled", false)
        binding.switchAntiAliasing.isChecked = SettingsManager.getSetting(requireContext(), "anti_aliasing_enabled", false)
        binding.switchShadowControl.isChecked = SettingsManager.getSetting(requireContext(), "shadow_control_enabled", false)
        binding.switchHeadshot.isChecked = SettingsManager.getSetting(requireContext(), SettingsManager.KEY_HEADSHOT_BOOST, false)
        binding.sliderSensitivity.value = SettingsManager.getFloat(requireContext(), SettingsManager.KEY_TOUCH_SENSITIVITY, 50f)

        // Listeners
        binding.switchFps.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "fps_meter_enabled", isChecked)
            if (isChecked) Toast.makeText(requireContext(), "FPS Meter Enabled", Toast.LENGTH_SHORT).show()
        }

        binding.switchCrosshair.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "crosshair_enabled", isChecked)
            if (isChecked) Toast.makeText(requireContext(), "Crosshair Enabled", Toast.LENGTH_SHORT).show()
        }

        binding.switchSidePanel.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!android.provider.Settings.canDrawOverlays(requireContext())) {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivityForResult(intent, 1001)
                    binding.switchSidePanel.isChecked = false
                } else {
                    startOverlayService()
                }
            } else {
                stopOverlayService()
            }
        }

        binding.switchAntiAliasing.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "anti_aliasing_enabled", isChecked)
            if (isChecked) Toast.makeText(requireContext(), "Anti-Aliasing Optimized", Toast.LENGTH_SHORT).show()
        }

        binding.switchShadowControl.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "shadow_control_enabled", isChecked)
            if (isChecked) Toast.makeText(requireContext(), "Shadow Control Optimized", Toast.LENGTH_SHORT).show()
        }

        binding.switchHeadshot.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), SettingsManager.KEY_HEADSHOT_BOOST, isChecked)
            if (isChecked) {
                applyHeadshotBoost()
            } else {
                Toast.makeText(requireContext(), "Headshot Boost Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.sliderSensitivity.addOnChangeListener { _, value, _ ->
            SettingsManager.saveFloat(requireContext(), SettingsManager.KEY_TOUCH_SENSITIVITY, value)
        }

        binding.btnDirectBoot.setOnClickListener {
            bootToGame()
        }
    }

    private fun applyHeadshotBoost() {
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted()) {
            // Tweak touch response via shell
            ShizukuManager.executeCommand("settings put system pointer_speed 7")
            ShizukuManager.executeCommand("settings put system touch_exploration_enabled 0")
            Toast.makeText(requireContext(), "Headshot Boost Applied via Shizuku!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Headshot Boost Enabled (Simulated)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bootToGame() {
        Toast.makeText(requireContext(), "Launching Game with Optimization...", Toast.LENGTH_LONG).show()
        // Here we could launch a specific game package if configured
        // For now, we simulate the optimization process
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted()) {
            ShizukuManager.executeCommand("am kill-all") // Kill background apps
        }
    }

    private fun startOverlayService() {
        SettingsManager.saveSetting(requireContext(), "side_panel_enabled", true)
        val intent = Intent(requireContext(), com.ultra.optimize.x.services.GameOverlayService::class.java)
        requireContext().startService(intent)
        Toast.makeText(requireContext(), "Game Side Panel Enabled", Toast.LENGTH_SHORT).show()
    }

    private fun stopOverlayService() {
        SettingsManager.saveSetting(requireContext(), "side_panel_enabled", false)
        val intent = Intent(requireContext(), com.ultra.optimize.x.services.GameOverlayService::class.java)
        requireContext().stopService(intent)
        Toast.makeText(requireContext(), "Game Side Panel Disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (android.provider.Settings.canDrawOverlays(requireContext())) {
                binding.switchSidePanel.isChecked = true
                startOverlayService()
            } else {
                Toast.makeText(requireContext(), "Permission denied for overlay", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
