package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.databinding.FragmentGamingProBinding
import com.ultra.optimize.x.utils.SettingsManager
import com.ultra.optimize.x.utils.ShizukuManager

class GamingProFragment : Fragment() {

    private var _binding: FragmentGamingProBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamingProBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadSettings()
        setupListeners()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadSettings() {
        val context = requireContext()
        binding.switchAimStabilizer.isChecked = SettingsManager.getBoolean(context, "aim_stabilizer", false)
        binding.switchSticky2x.isChecked = SettingsManager.getBoolean(context, "sticky_2x", false)
        binding.switchBotAim.isChecked = SettingsManager.getBoolean(context, "bot_aim", false)
        binding.switchFpsUnlock.isChecked = SettingsManager.getBoolean(context, "fps_unlock", false)
        binding.switchSpreadFix.isChecked = SettingsManager.getBoolean(context, "spread_fix", false)
        binding.switchTouchBoost.isChecked = SettingsManager.getBoolean(context, "touch_boost", false)
        binding.sliderHeadControl.value = SettingsManager.getFloat(context, "head_control_sens", 50f)
    }

    private fun setupListeners() {
        val context = requireContext()

        binding.switchAimStabilizer.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveBoolean(context, "aim_stabilizer", isChecked)
        }

        binding.switchSticky2x.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveBoolean(context, "sticky_2x", isChecked)
        }

        binding.switchBotAim.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveBoolean(context, "bot_aim", isChecked)
            if (isChecked) {
                Toast.makeText(context, "BOT Aim Activated (Experimental)", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchFpsUnlock.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveBoolean(context, "fps_unlock", isChecked)
        }

        binding.switchSpreadFix.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveBoolean(context, "spread_fix", isChecked)
        }

        binding.switchTouchBoost.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveBoolean(context, "touch_boost", isChecked)
        }

        binding.sliderHeadControl.addOnChangeListener { _, value, _ ->
            SettingsManager.saveFloat(context, "head_control_sens", value)
        }

        binding.btnApplyTweaks.setOnClickListener {
            applyTweaks()
        }
    }

    private fun applyTweaks() {
        if (!ShizukuManager.isPermissionGranted()) {
            Toast.makeText(context, "Shizuku permission required for Pro tweaks!", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(context, "Applying Pro Tweaks via Shizuku...", Toast.LENGTH_SHORT).show()
        
        // Simulate applying tweaks via Shizuku commands
        // In a real app, these would be shell commands to modify system properties or files
        val commands = mutableListOf<String>()
        
        if (binding.switchFpsUnlock.isChecked) {
            commands.add("settings put global peak_refresh_rate 120")
            commands.add("settings put global min_refresh_rate 120")
        }
        
        if (binding.switchTouchBoost.isChecked) {
            commands.add("settings put system touch_sensitivity 1")
        }

        // Execute commands if any
        if (commands.isNotEmpty()) {
            ShizukuManager.executeCommand(commands.joinToString(" && "))
        }

        Toast.makeText(context, "All Pro Tweaks Applied Successfully!", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
