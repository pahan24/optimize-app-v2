package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentCpuControlBinding
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.ultra.optimize.x.adapter.GovernorAdapter
import com.ultra.optimize.x.utils.CpuManager
import com.ultra.optimize.x.utils.RootManager
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

        // Set Spannable Title
        val title = "CPU\nCONTROL"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 3, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTitle.text = spannable
        
        binding.tvCpuModel.text = "CPU: ${Utils.getCpuInfo()}"
        binding.tvCores.text = "Cores: ${Runtime.getRuntime().availableProcessors()}"

        setupGovernors()
        handler.post(updateRunnable)
    }

    private fun setupGovernors() {
        if (!RootManager.isRooted()) {
            binding.rvGovernors.visibility = View.GONE
            binding.tvLabelGovernors.text = "Root Access Required for Governor Control"
            return
        }

        val governors = CpuManager.getAvailableGovernors()
        val currentGovernor = RootManager.runCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").trim()
        binding.tvCurrentGovernor.text = "Current Governor: $currentGovernor"
        
        binding.rvGovernors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGovernors.adapter = GovernorAdapter(governors, currentGovernor) { governor ->
            CpuManager.setGovernor(governor)
            binding.tvCurrentGovernor.text = "Current Governor: $governor"
            Toast.makeText(requireContext(), "Governor set to $governor", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStats() {
        if (_binding == null) return
        Thread {
            val cpuUsage = CpuManager.getCpuUsage()
            handler.post {
                if (_binding != null) {
                    animateProgress(binding.progressCpuLinear, cpuUsage)
                    binding.tvCpuUsageLabel.text = "CPU Load: $cpuUsage%"
                }
            }
        }.start()
    }

    private fun animateProgress(progress: com.google.android.material.progressindicator.LinearProgressIndicator, value: Int) {
        val animator = android.animation.ValueAnimator.ofInt(progress.progress, value)
        animator.duration = 800
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener {
            if (_binding != null) {
                progress.progress = it.animatedValue as Int
            }
        }
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
