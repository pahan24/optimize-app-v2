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
        val subtitle = arguments?.getString("featureDesc") ?: "Configure advanced system optimization parameters"
        val settingsManager = com.ultra.optimize.x.utils.SettingsManager(requireContext())
        val featureKey = title.lowercase().replace(" ", "_") + "_enabled"

        // Set Spannable Title
        val upperTitle = title.uppercase()
        val spannable = android.text.SpannableString(upperTitle)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        
        // Find first word or first few chars to highlight
        val spaceIndex = upperTitle.indexOf(" ")
        val end = if (spaceIndex != -1) spaceIndex else (upperTitle.length / 2).coerceAtLeast(1)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        binding.tvFeatureTitle.text = spannable
        binding.tvSubtitle.text = subtitle
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.switchFeature.isChecked = settingsManager.isFeatureEnabled(featureKey)

        binding.btnApply.setOnClickListener {
            binding.btnApply.isEnabled = false
            binding.btnApply.text = "APPLYING..."
            
            Thread {
                // Execute root commands based on feature
                if (com.ultra.optimize.x.utils.RootManager.isRooted()) {
                    when (title) {
                        "Battery Saver" -> {
                            com.ultra.optimize.x.utils.RootManager.runCommand("settings put global low_power 1")
                        }
                        "Network Optimizer" -> {
                            com.ultra.optimize.x.utils.RootManager.runCommand("sysctl -w net.ipv4.tcp_fastopen=3")
                        }
                        "Game Boost" -> {
                            com.ultra.optimize.x.utils.RootManager.runCommand("am set-standby-bucket com.ultra.optimize.x active")
                        }
                    }
                }
                
                Thread.sleep(1500)
                activity?.runOnUiThread {
                    binding.btnApply.isEnabled = true
                    binding.btnApply.text = "APPLIED"
                    Toast.makeText(requireContext(), "$title Tweaks Applied!", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }

        binding.switchFeature.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setFeatureEnabled(featureKey, isChecked)
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(requireContext(), "$title $status", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
