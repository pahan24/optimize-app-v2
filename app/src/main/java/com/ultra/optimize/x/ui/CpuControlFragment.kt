package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.databinding.FragmentCpuControlBinding
import com.ultra.optimize.x.utils.CpuManager
import com.ultra.optimize.x.utils.Utils

class CpuControlFragment : Fragment() {
    private var _binding: FragmentCpuControlBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCpuControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.tvCpuModel.text = "CPU: ${Utils.getCpuInfo()}"
        binding.tvCores.text = "Cores: ${Runtime.getRuntime().availableProcessors()}"

        handler.post(updateRunnable)
    }

    private fun updateStats() {
        if (_binding == null) return
        Thread {
            val cpuUsage = CpuManager.getCpuUsage()
            handler.post {
                if (_binding != null) {
                    binding.progressCpuLinear.setProgress(cpuUsage, true)
                    binding.tvCpuUsageLabel.text = "CPU Load: $cpuUsage%"
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
