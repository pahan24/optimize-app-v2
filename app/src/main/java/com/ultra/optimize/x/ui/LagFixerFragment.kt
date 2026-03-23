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
import com.ultra.optimize.x.databinding.FragmentLagFixerBinding
import com.ultra.optimize.x.utils.LagManager
import com.ultra.optimize.x.utils.RootManager
import com.ultra.optimize.x.utils.SettingsManager

class LagFixerFragment : Fragment() {

    private var _binding: FragmentLagFixerBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLagFixerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEnabled = SettingsManager.getSetting(requireContext(), "lag_fixer")
        binding.switchFeature.isChecked = isEnabled
        binding.progressLag.progress = if (isEnabled) 100 else 40

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.switchFeature.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "lag_fixer", isChecked)
            if (isChecked) {
                binding.tvStatus.text = "Status: Optimized"
                binding.tvStatus.setTextColor(resources.getColor(com.ultra.optimize.x.R.color.accent_green))
                binding.progressLag.setProgress(100, true)
            } else {
                binding.tvStatus.text = "Status: Normal"
                binding.tvStatus.setTextColor(resources.getColor(com.ultra.optimize.x.R.color.neon_blue))
                binding.progressLag.setProgress(40, true)
            }
        }

        binding.btnApply.setOnClickListener {
            performLagFix()
        }
    }

    private fun performLagFix() {
        binding.btnApply.isEnabled = false
        binding.btnApply.text = "FIXING LAG..."
        binding.progressLag.isIndeterminate = true
        
        Thread {
            val isRooted = RootManager.isRooted()
            LagManager.fixLag(requireContext(), isRooted)
            Thread.sleep(2500)
            handler.post {
                if (_binding != null) {
                    binding.btnApply.isEnabled = true
                    binding.btnApply.text = "FIX SYSTEM LAG"
                    binding.progressLag.isIndeterminate = false
                    binding.progressLag.setProgress(100, true)
                    binding.tvStatus.text = "Status: Optimized"
                    binding.tvStatus.setTextColor(resources.getColor(com.ultra.optimize.x.R.color.accent_green))
                    Toast.makeText(requireContext(), "System lag reduced successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
