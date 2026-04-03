package com.ultra.optimize.x.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentBypassBinding
import com.ultra.optimize.x.utils.SettingsManager
import com.ultra.optimize.x.utils.ShizukuManager

class BypassFragment : Fragment() {

    private var _binding: FragmentBypassBinding? = null
    private val binding get() = _binding!!

    private var isBypassActive = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBypassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isBypassActive = SettingsManager.getBoolean(requireContext(), "bypass_active", false)
        updateUi()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnActivateBypass.setOnClickListener {
            if (isBypassActive) {
                deactivateBypass()
            } else {
                activateBypass()
            }
        }
    }

    private fun activateBypass() {
        if (!ShizukuManager.isPermissionGranted()) {
            Toast.makeText(context, "Shizuku permission required for Bypass!", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnActivateBypass.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "ACTIVATING..."
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_orange))

        handler.postDelayed({
            // Simulate bypass activation via Shizuku
            ShizukuManager.executeCommand("settings put global bypass_active 1")
            
            isBypassActive = true
            SettingsManager.saveBoolean(requireContext(), "bypass_active", true)
            updateUi()
            binding.btnActivateBypass.isEnabled = true
            binding.progressBar.visibility = View.GONE
            Toast.makeText(context, "Anti-Ban Bypass Activated!", Toast.LENGTH_SHORT).show()
        }, 3000)
    }

    private fun deactivateBypass() {
        binding.btnActivateBypass.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "DEACTIVATING..."

        handler.postDelayed({
            ShizukuManager.executeCommand("settings put global bypass_active 0")
            
            isBypassActive = false
            SettingsManager.saveBoolean(requireContext(), "bypass_active", false)
            updateUi()
            binding.btnActivateBypass.isEnabled = true
            binding.progressBar.visibility = View.GONE
            Toast.makeText(context, "Bypass Deactivated", Toast.LENGTH_SHORT).show()
        }, 1500)
    }

    private fun updateUi() {
        if (isBypassActive) {
            binding.tvStatus.text = "BYPASS ACTIVE"
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
            binding.ivShield.setImageResource(R.drawable.ic_shield)
            binding.ivShield.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_green))
            binding.btnActivateBypass.text = "DEACTIVATE BYPASS"
            binding.btnActivateBypass.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent_red)
        } else {
            binding.tvStatus.text = "BYPASS INACTIVE"
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            binding.ivShield.setImageResource(R.drawable.ic_shield)
            binding.ivShield.setColorFilter(ContextCompat.getColor(requireContext(), R.color.neon_blue))
            binding.btnActivateBypass.text = "ACTIVATE BYPASS"
            binding.btnActivateBypass.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_blue)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
