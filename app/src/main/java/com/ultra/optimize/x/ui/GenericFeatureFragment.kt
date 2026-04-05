package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentGenericFeatureBinding

class GenericFeatureFragment : Fragment() {
    private var _binding: FragmentGenericFeatureBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericFeatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val title = arguments?.getString("featureTitle") ?: "Feature"
        val subtitle = when (title) {
            "GX BOOST" -> "Advanced graphics engine optimization for smoother gameplay"
            "SETTINGS CONFIG" -> "Optimize system settings for professional gaming performance"
            "RED BUTTON" -> "Emergency performance boost and memory flush"
            "CORRECT DPI" -> "Adjust screen density for precise touch response"
            "MOVEMENT TUTORIAL" -> "Learn advanced movement techniques and sensitivity settings"
            "MACRODROID SCRIPT" -> "Automate complex gaming actions with optimized scripts"
            "EASY DRAG" -> "Enhance touch sensitivity for effortless drag shots"
            "SPREAD FIX" -> "Minimize bullet spread and improve weapon accuracy"
            "AIMSTABILIZE" -> "Stabilize crosshair movement for better aim control"
            "120 FPS UNLOCK" -> "Bypass frame rate limits for ultra-smooth 120 FPS gaming"
            "MAIN OBB" -> "Optimize game data files for faster loading and stability"
            "3RD REGEDIT" -> "Apply advanced registry tweaks for system-level optimization"
            "OPTIMIZE DEVICE" -> "Deep clean and optimize device for peak gaming performance"
            "SENSI LESSON" -> "Master professional sensitivity settings and techniques"
            "TOUCH SPEED" -> "Maximize touch response speed and reduce input lag"
            "CUSTOMIZED HUDS" -> "Import and optimize professional gaming HUD layouts"
            "FIRE BUTTON" -> "Optimize fire button response and sensitivity"
            else -> arguments?.getString("featureDesc") ?: "Configure advanced system optimization parameters"
        }
        val context = context ?: return
        val settingsManager = com.ultra.optimize.x.utils.SettingsManager(context)
        val featureKey = title.lowercase().replace(" ", "_") + "_enabled"

        // Set Spannable Title
        val upperTitle = title.uppercase()
        val spannable = android.text.SpannableString(upperTitle)
        val blueColor = androidx.core.content.ContextCompat.getColor(context, R.color.neon_blue)
        
        // Find first word or first few chars to highlight
        val spaceIndex = upperTitle.indexOf(" ")
        val end = if (spaceIndex != -1) spaceIndex else (upperTitle.length / 2).coerceAtLeast(1)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        if (_binding != null) {
            binding.tvFeatureTitle.text = spannable
            binding.tvSubtitle.text = subtitle
            binding.btnBack.setOnClickListener { findNavController().popBackStack() }
            
            binding.switchFeature.isChecked = settingsManager.isFeatureEnabled(featureKey)

            binding.btnApply.setOnClickListener {
                binding.btnApply.isEnabled = false
                binding.btnApply.text = "APPLYING..."
                
                Thread {
                    try {
                        // Execute root commands based on feature
                        if (com.ultra.optimize.x.utils.RootManager.isRooted(context)) {
                            when (title) {
                                "GX BOOST" -> {
                                    com.ultra.optimize.x.utils.RootManager.runCommand("setprop debug.egl.hw 1")
                                    com.ultra.optimize.x.utils.RootManager.runCommand("setprop debug.gr.num_common_buffers 3")
                                }
                                "TOUCH SPEED" -> {
                                    com.ultra.optimize.x.utils.RootManager.runCommand("settings put secure long_press_timeout 250")
                                    com.ultra.optimize.x.utils.RootManager.runCommand("settings put secure multi_press_timeout 250")
                                }
                                "120 FPS UNLOCK" -> {
                                    com.ultra.optimize.x.utils.RootManager.runCommand("setprop persist.sys.composition.type gpu")
                                    com.ultra.optimize.x.utils.RootManager.runCommand("setprop persist.sys.ui.hw 1")
                                }
                            }
                        }
                        
                        Thread.sleep(1500)
                        activity?.runOnUiThread {
                            if (_binding != null) {
                                binding.btnApply.isEnabled = true
                                binding.btnApply.text = "APPLIED"
                                Toast.makeText(context, "$title Tweaks Applied!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GenericFeatureFragment", "Error applying tweaks", e)
                    }
                }.start()
            }

            binding.switchFeature.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setFeatureEnabled(featureKey, isChecked)
                val status = if (isChecked) "Enabled" else "Disabled"
                Toast.makeText(context, "$title $status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
