package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentRootInstallerBinding
import com.ultra.optimize.x.utils.RootManager
import com.ultra.optimize.x.utils.SettingsManager
import com.ultra.optimize.x.utils.ShizukuManager
import rikka.shizuku.Shizuku

class RootFragment : Fragment(), Shizuku.OnRequestPermissionResultListener {

    private var _binding: FragmentRootInstallerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRootInstallerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRequestRoot.setOnClickListener {
            val success = RootManager.requestRoot()
            if (success) {
                Toast.makeText(context, "Root Access Granted!", Toast.LENGTH_SHORT).show()
                updateStatus()
            } else {
                Toast.makeText(context, "Root Access Denied. Is your device rooted?", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnRequestShizuku.setOnClickListener {
            if (ShizukuManager.isShizukuAvailable()) {
                ShizukuManager.requestPermission(this)
            } else {
                Toast.makeText(context, "Shizuku Service is not running!", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnEnableSimulated.setOnClickListener {
            SettingsManager.saveSetting(requireContext(), SettingsManager.KEY_ROOT_MODE, true)
            Toast.makeText(context, "Simulated Root Mode Enabled", Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        updateStatus()
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == ShizukuManager.REQUEST_CODE) {
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show()
                SettingsManager.saveSetting(requireContext(), SettingsManager.KEY_SHIZUKU_MODE, true)
            } else {
                Toast.makeText(context, "Shizuku Permission Denied", Toast.LENGTH_SHORT).show()
            }
            updateStatus()
        }
    }

    private fun updateStatus() {
        val isRooted = RootManager.isRooted(requireContext())
        val isSimulated = SettingsManager.getSetting(requireContext(), SettingsManager.KEY_ROOT_MODE, false)
        
        if (isRooted) {
            binding.tvRootStatusDetail.text = if (isSimulated) "AUTHORIZED (SIMULATED)" else "AUTHORIZED"
            binding.tvRootStatusDetail.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
        } else {
            binding.tvRootStatusDetail.text = "DENIED (NOT ROOTED)"
            binding.tvRootStatusDetail.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
        }

        val isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        val isShizukuPermitted = ShizukuManager.isPermissionGranted()

        if (isShizukuAvailable) {
            if (isShizukuPermitted) {
                binding.tvShizukuStatus.text = "AUTHORIZED"
                binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                binding.btnRequestShizuku.isEnabled = false
                binding.btnRequestShizuku.text = "PERMISSION GRANTED"
            } else {
                binding.tvShizukuStatus.text = "PENDING PERMISSION"
                binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_orange))
                binding.btnRequestShizuku.isEnabled = true
            }
        } else {
            binding.tvShizukuStatus.text = "NOT RUNNING"
            binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            binding.btnRequestShizuku.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ShizukuManager.removePermissionListener(this)
        _binding = null
    }
}
