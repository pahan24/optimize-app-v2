package com.ultra.optimize.x.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentFreeFireBinding
import com.ultra.optimize.x.utils.FreeFireManager
import com.ultra.optimize.x.utils.RootManager
import com.ultra.optimize.x.utils.SettingsManager

class FreeFireFragment : Fragment() {

    private var _binding: FragmentFreeFireBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFreeFireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Set Spannable Title
        val title = "FREE\nFIRE"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 4, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTitle.text = spannable

        val autoBoost = SettingsManager.getSetting(requireContext(), "ff_auto_boost")
        val sensitivity = SettingsManager.getSetting(requireContext(), "ff_sensitivity")
        
        binding.switchAutoBoost.isChecked = autoBoost
        binding.switchSensitivity.isChecked = sensitivity
        
        binding.progressSensitivity.progress = if (sensitivity) 90 else 30

        binding.switchAutoBoost.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "ff_auto_boost", isChecked)
        }

        binding.switchSensitivity.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "ff_sensitivity", isChecked)
            if (isChecked) {
                binding.progressSensitivity.setProgress(90, true)
            } else {
                binding.progressSensitivity.setProgress(30, true)
            }
        }

        binding.btnApply.setOnClickListener {
            performFreeFireBoost()
        }
    }

    private fun performFreeFireBoost() {
        binding.btnApply.isEnabled = false
        binding.btnApply.text = "OPTIMIZING FOR FREE FIRE..."
        binding.progressSensitivity.isIndeterminate = true
        
        Thread {
            val isRooted = RootManager.isRooted()
            FreeFireManager.optimizeForFreeFire(requireContext(), isRooted)
            Thread.sleep(3000)
            handler.post {
                if (_binding != null) {
                    binding.btnApply.isEnabled = true
                    binding.btnApply.text = "BOOST FREE FIRE"
                    binding.progressSensitivity.isIndeterminate = false
                    binding.progressSensitivity.setProgress(100, true)
                    binding.tvStatus.text = "Status: Optimized"
                    binding.tvStatus.setTextColor(resources.getColor(R.color.accent_green))
                    Toast.makeText(requireContext(), "Free Fire optimized for maximum performance!", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
