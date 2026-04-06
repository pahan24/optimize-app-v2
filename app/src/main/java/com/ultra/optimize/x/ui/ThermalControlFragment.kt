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
import com.ultra.optimize.x.databinding.FragmentThermalControlBinding
import com.ultra.optimize.x.utils.ThermalManager

class ThermalControlFragment : Fragment() {
    private var _binding: FragmentThermalControlBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThermalControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Set Spannable Title
        val title = "THERMAL\nCONTROL"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 7, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTitle.text = spannable
        
        binding.switchCoolDown.isChecked = ThermalManager.isAutoCoolDownEnabled(requireContext())
        binding.switchCoolDown.setOnCheckedChangeListener { _, isChecked ->
            ThermalManager.setAutoCoolDownEnabled(requireContext(), isChecked)
        }

        handler.post(updateRunnable)
    }

    private fun updateStats() {
        if (_binding == null) return
        val temp = ThermalManager.getCpuTemp()
        binding.tvTempLarge.text = "${temp.toInt()}°C"
        animateProgress(binding.progressTempThermal, temp.toInt())

        val status: String
        val color: Int
        when {
            temp > 45 -> {
                status = "Status: Critical"
                color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent_red)
            }
            temp > 38 -> {
                status = "Status: Moderate"
                color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent_orange)
            }
            else -> {
                status = "Status: Normal"
                color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent_green)
            }
        }
        binding.tvThermalStatus.text = status
        binding.tvThermalStatus.setTextColor(color)
    }

    private fun animateProgress(progress: com.google.android.material.progressindicator.CircularProgressIndicator, value: Int) {
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
